package cn.aimodel.merchant
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var input by remember { mutableStateOf("") }
                var reply by remember { mutableStateOf("输入客户问题，生成待审核建议。") }
                var busy by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("商家客服 Agent", style = MaterialTheme.typography.headlineMedium)
                        Text("演示模式 · 未连接真实商城和大模型")
                        OutlinedTextField(value = input, onValueChange = { input = it.take(2000) },
                            label = { Text("客户问题") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = input.isNotBlank() && !busy, onClick = {
                            busy = true
                            val message = input
                            scope.launch {
                                try { reply = requestReply(message) }
                                catch (e: Exception) { reply = "请求失败，请检查后端地址和网络连接。" }
                                finally { busy = false }
                            }
                        }) { Text(if (busy) "处理中…" else "生成回复建议") }
                        Text("待人工审核", style = MaterialTheme.typography.titleMedium)
                        Text(reply)
                        Text("本页面不向买家发送消息，也不执行退款。")
                    }
                }
            }
        }
    }
}
private suspend fun requestReply(message: String): String = withContext(Dispatchers.IO) {
    val connection = URL(BuildConfig.API_BASE_URL + "/api/demo/reply").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        connection.outputStream.use { it.write(JSONObject().put("message", message).toString().toByteArray(Charsets.UTF_8)) }
        check(connection.responseCode in 200..299) { "HTTP request failed" }
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()).getString("reply") }
    } finally { connection.disconnect() }
}
