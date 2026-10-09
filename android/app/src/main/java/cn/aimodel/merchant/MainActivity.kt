package cn.aimodel.merchant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalLayoutApi::class)
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("merchant",MODE_PRIVATE)
  setContent {
   MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF126653),secondary=Color(0xFF658C75),background=Color(0xFFF3F6F5),surface=Color(0xFFFAFCF9))) {
    var base by remember {mutableStateOf((prefs.getString("base",BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL).let {if(it.contains("10.0.2.2"))BuildConfig.API_BASE_URL else it})}
    var token by remember {mutableStateOf(prefs.getString("token","") ?: "")}
    var username by remember {mutableStateOf("merchant")}
    var password by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var tab by remember {mutableStateOf("概览")}
    var conversations by remember {mutableStateOf(emptyList<JSONObject>())}
    var messages by remember {mutableStateOf(emptyList<JSONObject>())}
    var orders by remember {mutableStateOf(emptyList<JSONObject>())}
    var refunds by remember {mutableStateOf(emptyList<JSONObject>())}
    var knowledge by remember {mutableStateOf(emptyList<JSONObject>())}
    var selected by remember {mutableStateOf("")}
    var mode by remember {mutableStateOf("ASSISTED")}
    var text by remember {mutableStateOf("")}
    var configured by remember {mutableStateOf(false)}
    var query by remember {mutableStateOf("")}
    var statusFilter by remember {mutableStateOf("全部")}
    var trace by remember {mutableStateOf("")}
    var confirmation by remember {mutableStateOf<Pair<String,JSONObject>?>(null)}
    var pendingLabel by remember {mutableStateOf("")}
    val scope=rememberCoroutineScope()
    suspend fun api(path:String,body:JSONObject?=null):String = request(base,token,path,body)
    suspend fun refresh() {
     conversations=JSONArray(api("/conversations")).objects()
     configured=JSONObject(api("/agent/status")).getBoolean("configured")
     if(selected.isNotBlank()) {
      mode=conversations.firstOrNull {it.getString("id")==selected}?.getString("mode") ?: "ASSISTED"
      messages=JSONArray(api("/conversations/$selected/messages")).objects()
     }
     orders=JSONArray(api("/orders")).objects()
     refunds=JSONArray(api("/refunds")).objects()
     knowledge=JSONArray(api("/knowledge")).objects()
    }
    fun action(block:suspend ()->Unit) {
     if(busy)return
     busy=true;error=""
     scope.launch {
      try {block()} catch(e:Exception) {error=e.message ?: "操作失败"} finally {busy=false}
     }
    }
    LaunchedEffect(token) {
     if(token.isNotBlank()) {
      try {
       val me=JSONObject(api("/auth/me"))
       check(me.getString("role")=="MERCHANT") {"请使用客服账号登录"}
       refresh()
      }catch(e:Exception){error=e.message ?: "登录失效";token="";prefs.edit().remove("token").apply()}
     }
    }
    Surface(Modifier.fillMaxSize()) {
     Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
      Text("商家客服 Agent",style=MaterialTheme.typography.headlineMedium)
      Text("连接客户 · 核实订单 · 人工审核",style=MaterialTheme.typography.bodySmall)
      if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
      if(token.isBlank()) {
       OutlinedTextField(value=base,onValueChange={base=it.trim()},label={Text("后端地址（线上使用 HTTPS）")},modifier=Modifier.fillMaxWidth())
       OutlinedTextField(value=username,onValueChange={username=it},label={Text("客服账号")},modifier=Modifier.fillMaxWidth())
       OutlinedTextField(value=password,onValueChange={password=it},label={Text("密码")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
       Button(enabled=!busy&&username.isNotBlank()&&password.isNotBlank(),onClick={action {
        val result=JSONObject(request(base,"","/auth/login",JSONObject().put("username",username).put("password",password)))
        check(result.getJSONObject("user").getString("role")=="MERCHANT") {"请使用客服账号"}
        token=result.getString("token");password=""
        prefs.edit().putString("token",token).putString("base",base).apply()
       }}) {Text(if(busy)"登录中…" else "登录客服工作台")}
       Text("免费服务首次启动可能较慢，请耐心等待。客服账号和密码由管理员提供。",style=MaterialTheme.typography.bodySmall)
      } else {
       Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(enabled=!busy,onClick={action {refresh()}}){Text("刷新")}
        TextButton(enabled=!busy,onClick={action {try{api("/auth/logout",JSONObject())}finally{token="";selected="";messages=emptyList();prefs.edit().remove("token").apply()}}}){Text("退出")}
       }
       FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("概览","会话","订单","知识库","设置").forEach {name->
         FilterChip(selected=tab==name,onClick={tab=name;query=""},label={Text(name)})
        }
       }
       if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
       when(tab) {
        "概览" -> {
         Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
          Text("今日工作空间",style=MaterialTheme.typography.titleLarge)
          Text("优先处理待回复客户和订单售后。")
          Text("待回复会话  "+conversations.count {it.optString("last_author")=="BUYER"})
          Text("待发货订单  "+orders.count {it.getString("status")=="PAID"})
          Text("待审批售后  "+refunds.count {it.getString("status")=="PENDING"})
          Button(onClick={tab="会话"}){Text("开始处理会话 →")}
         }}
         Text(if(configured)"Agent 已配置 · 回复需要审核" else "Agent 未配置 · 可处理人工客服与订单")
         OutlinedButton(onClick={tab="设置"}){Text("查看连接与服务设置")}
        }
        "设置" -> {
         Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
          Text("服务连接",style=MaterialTheme.typography.titleLarge)
          Text(base,style=MaterialTheme.typography.bodySmall)
          Text(if(configured)"模型配置已填写，实际可用性需生成草稿验证。" else "真实模型尚未配置，请在 Render 设置模型 API 密钥和模型名称。")
          Text("免费版重启或重新部署可能清空演示数据。支付、发货与退款均为模拟业务。")
          Text("切换后端地址请退出登录后修改。网页工作台还支持快捷回复配置、审计和 GrapesJS 页面模板编辑。")
         }}
        }
        "会话" -> {
         Text(if(configured)"模型已配置 · 回复需审核" else "模型未配置 · 请人工回复")
         if(conversations.isEmpty())Text("暂无会话，请买家先在商城发送咨询。")
         OutlinedTextField(value=query,onValueChange={query=it},label={Text("搜索客户 / 最近消息")},modifier=Modifier.fillMaxWidth())
         conversations.filter {it.getString("username").contains(query,true)||it.optString("last_message").contains(query,true)}.forEach {c->
          OutlinedButton(enabled=!busy,modifier=Modifier.fillMaxWidth(),onClick={selected=c.getString("id");trace="";action{refresh()}}) {
           Text(c.getString("username")+" · "+if(c.getString("mode")=="HUMAN")"人工接管" else "Agent辅助")
          }
         }
         if(selected.isNotBlank()) {
          HorizontalDivider()
          Text("当前会话："+selected.take(8))
          Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
           Button(enabled=!busy&&mode!="HUMAN"&&configured,onClick={action {
            val r=JSONObject(api("/conversations/$selected/draft",JSONObject()));trace=r.getJSONArray("trace").toString(2);refresh()
           }}){Text("生成草稿")}
           OutlinedButton(enabled=!busy,onClick={action {
            api("/conversations/$selected/mode",JSONObject().put("mode",if(mode=="HUMAN")"ASSISTED" else "HUMAN"));refresh()
           }}){Text(if(mode=="HUMAN")"恢复辅助" else "人工接管")}
          }
          messages.forEach {m->
           Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
             Text(m.getString("author")+" · "+if(m.getString("status")=="DRAFT")"待审核" else "已发送",style=MaterialTheme.typography.labelMedium)
             Text(m.getString("content"))
             if(m.getString("status")=="DRAFT")Button(enabled=!busy&&mode!="HUMAN",onClick={
              pendingLabel="确认审核并发送此回复？";confirmation=Pair("/conversations/$selected/drafts/"+m.getString("id")+"/approve",JSONObject())
             }) {Text("审核发送")}
            }
           }
          }
          OutlinedTextField(value=text,onValueChange={text=it.take(3500)},label={Text("人工回复")},modifier=Modifier.fillMaxWidth())
          Button(enabled=!busy&&text.isNotBlank(),onClick={action{api("/conversations/$selected/messages",JSONObject().put("content",text));text="";refresh()}}){Text("发送人工回复")}
          if(trace.isNotBlank()){Text("工具调用记录",style=MaterialTheme.typography.titleSmall);Text(trace,style=MaterialTheme.typography.bodySmall)}
         }
        }
        "订单" -> {
         Text("模拟业务，不产生真实资金交易。")
         OutlinedTextField(value=query,onValueChange={query=it},label={Text("搜索订单编号或商品")},modifier=Modifier.fillMaxWidth())
         FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("全部","PAID","SHIPPED","REFUNDED").forEach {s->FilterChip(selected=statusFilter==s,onClick={statusFilter=s},label={Text(if(s=="全部")s else orderStatus(s))})}}
         orders.filter {(statusFilter=="全部"||it.getString("status")==statusFilter)&&(it.getString("id").contains(query,true)||it.getString("product_id").contains(query,true))}.forEach {o->
          Card(Modifier.fillMaxWidth()) {
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(o.getString("id"),style=MaterialTheme.typography.labelSmall)
            Text(o.getString("product_id")+" × "+o.getInt("quantity"))
            Text("¥ "+"%.2f".format(o.getInt("total_cents")/100.0)+" · "+orderStatus(o.getString("status")))
            Text("模拟地址："+o.getString("address"))
            if(o.getString("status")=="PAID")Button(enabled=!busy,onClick={
             pendingLabel="确认模拟发货？";confirmation=Pair("/orders/"+o.getString("id")+"/ship",JSONObject().put("tracking","DEMO-"+o.getString("id").take(8)))
            }){Text("模拟发货")}
           }
          }
         }
         Text("售后申请",style=MaterialTheme.typography.titleMedium)
         refunds.forEach {r->
          Card(Modifier.fillMaxWidth()) {
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("订单："+r.getString("order_id"));Text(r.getString("reason"));Text(r.getString("status"))
            if(r.getString("status")=="PENDING")Button(enabled=!busy,onClick={
             pendingLabel="批准模拟退款？此操作改变订单状态，不产生真实资金转移。";confirmation=Pair("/refunds/"+r.getString("id")+"/approve",JSONObject())
            }){Text("人工批准退款")}
           }
          }
         }
        }
        "知识库" -> knowledge.forEach {k->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(k.getString("title"),style=MaterialTheme.typography.titleMedium);Text(k.getString("content"));Text("依据："+k.getString("id"),style=MaterialTheme.typography.labelSmall)}}}
       }
      }
     }
    }
    confirmation?.let {pending->
     AlertDialog(onDismissRequest={confirmation=null},title={Text("人工确认")},text={Text(pendingLabel)},confirmButton={TextButton(onClick={confirmation=null;action{api(pending.first,pending.second);refresh()}}){Text("确认")}},dismissButton={TextButton(onClick={confirmation=null}){Text("取消")}})
    }
   }
  }
 }
}
private fun JSONArray.objects():List<JSONObject> = (0 until length()).map {getJSONObject(it)}
private suspend fun request(base:String,token:String,path:String,body:JSONObject?):String=withContext(Dispatchers.IO) {
 val url=URL(base.trim().trimEnd('/')+"/api"+path)
 require(url.protocol=="https" || (BuildConfig.DEBUG&&url.protocol=="http")){"正式版本需要 HTTPS 地址"}
 val connection=url.openConnection() as HttpURLConnection
 try {
  connection.requestMethod=if(body==null)"GET" else "POST"
  connection.connectTimeout=60000;connection.readTimeout=150000
  if(token.isNotBlank())connection.setRequestProperty("Authorization","Bearer $token")
  if(body!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");connection.outputStream.use {it.write(body.toString().toByteArray(Charsets.UTF_8))}}
  val ok=connection.responseCode in 200..299
  val result=(if(ok)connection.inputStream else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()} ?: "{}"
  if(!ok)throw IllegalStateException(runCatching {JSONObject(result).optString("error","请求失败")}.getOrDefault("服务正在唤醒，请稍后重试（HTTP ${connection.responseCode}）"))
  result
 } finally {connection.disconnect()}
}

private fun orderStatus(s:String):String=when(s){"PENDING"->"待模拟支付";"PAID"->"待模拟发货";"SHIPPED"->"已模拟发货";"REFUNDED"->"已模拟退款";else->s}
