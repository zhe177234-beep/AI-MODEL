package cn.aimodel;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
@Service
public class ModelGateway {
 private final ObjectMapper json;private final String base,key,model;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public ModelGateway(ObjectMapper json,@Value("${agent.base-url}")String base,@Value("${agent.api-key}")String key,@Value("${agent.model}")String model){this.json=json;this.base=base;this.key=key;this.model=model;}
 public boolean configured(){return !key.isBlank() && !model.isBlank();}
 public JsonNode complete(ArrayNode messages,ArrayNode tools) throws Exception {
  if(!configured())throw new IllegalStateException("模型未配置");
  URI uri=URI.create(base.replaceAll("/$","")+"/chat/completions");
  if(!uri.getScheme().equals("https") && !SetLocal.isLocal(uri))throw new IllegalStateException("模型接口需要HTTPS");
  ObjectNode body=json.createObjectNode();body.put("model",model);body.set("messages",messages);body.set("tools",tools);body.put("tool_choice","auto");
  var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
  var response=http.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()/100!=2)throw new IllegalStateException("模型服务请求失败");
  if(response.body().length()>1000000)throw new IllegalStateException("模型响应过大");
  JsonNode result=json.readTree(response.body()).path("choices").path(0).path("message");
  if(!result.isObject())throw new IllegalStateException("模型响应格式错误");return result;
 }
 private static class SetLocal {static boolean isLocal(URI u){return java.util.Set.of("localhost","127.0.0.1","[::1]").contains(u.getHost());}}
}
