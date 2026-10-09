package cn.aimodel.merchant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("merchant",MODE_PRIVATE)
  setContent {
   MaterialTheme {
    var base by remember {mutableStateOf(prefs.getString("base",BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL)}
    var token by remember {mutableStateOf(prefs.getString("token","") ?: "")}
    var username by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    var tab by remember {mutableStateOf("会话")}
    var conversations by remember {mutableStateOf(emptyList<JSONObject>())}
    var messages by remember {mutableStateOf(emptyList<JSONObject>())}
    var orders by remember {mutableStateOf(emptyList<JSONObject>())}
    var refunds by remember {mutableStateOf(emptyList<JSONObject>())}
    var knowledge by remember {mutableStateOf(emptyList<JSONObject>())}
    var selected by remember {mutableStateOf("")}
    var mode by remember {mutableStateOf("ASSISTED")}
    var text by remember {mutableStateOf("")}
    var configured by remember {mutableStateOf(false)}
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
     Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
      Text("商家客服 Agent",style=MaterialTheme.typography.headlineMedium)
      Text("毕业设计 · 模拟商城 / 人工审核",style=MaterialTheme.typography.bodySmall)
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
       Text("客服账号由后端 MERCHANT_USERNAME / MERCHANT_PASSWORD 配置创建。")
      } else {
       Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        TextButton(enabled=!busy,onClick={action {refresh()}}){Text("刷新")}
        TextButton(enabled=!busy,onClick={action {try{api("/auth/logout",JSONObject())}finally{token="";selected="";messages=emptyList();prefs.edit().remove("token").apply()}}}){Text("退出")}
       }
       Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("会话","订单","知识库").forEach {name->
         FilterChip(selected=tab==name,onClick={tab=name},label={Text(name)})
        }
       }
       if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
       when(tab) {
        "会话" -> {
         Text(if(configured)"模型已配置 · 回复需审核" else "模型未配置 · 请人工回复")
         if(conversations.isEmpty())Text("暂无会话，请买家先在商城发送咨询。")
         conversations.forEach {c->
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
         orders.forEach {o->
          Card(Modifier.fillMaxWidth()) {
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(o.getString("id"),style=MaterialTheme.typography.labelSmall)
            Text(o.getString("product_id")+" × "+o.getInt("quantity"))
            Text("¥ "+"%.2f".format(o.getInt("total_cents")/100.0)+" · "+o.getString("status"))
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
 val url=URL(base.trimEnd('/')+"/api"+path)
 require(url.protocol=="https" || (BuildConfig.DEBUG&&url.protocol=="http")){"正式版本需要 HTTPS 地址"}
 val connection=url.openConnection() as HttpURLConnection
 try {
  connection.requestMethod=if(body==null)"GET" else "POST"
  connection.connectTimeout=10000;connection.readTimeout=150000
  if(token.isNotBlank())connection.setRequestProperty("Authorization","Bearer $token")
  if(body!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");connection.outputStream.use {it.write(body.toString().toByteArray(Charsets.UTF_8))}}
  val ok=connection.responseCode in 200..299
  val result=(if(ok)connection.inputStream else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use{it.readText()} ?: "{}"
  if(!ok)throw IllegalStateException(JSONObject(result).optString("error","请求失败"))
  result
 } finally {connection.disconnect()}
}
