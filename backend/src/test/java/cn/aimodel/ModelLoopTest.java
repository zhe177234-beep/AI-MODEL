package cn.aimodel;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:model;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","merchant.password=TestMerchant12345!"})
class ModelLoopTest {
 @Autowired Shop shop;@Autowired Auth auth;@Autowired ObjectMapper json;
 @Test void toolResultReturnsToModelBeforeDraft()throws Exception{
  var buyer=auth.register("modelbuyer","PasswordBuyer123!");var order=shop.create(buyer,"p-lamp",1,"private-address",UUID.randomUUID().toString());
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  AtomicInteger calls=new AtomicInteger();AtomicReference<String> secondRequest=new AtomicReference<>("");
  server.createContext("/v1/chat/completions",exchange->{
   String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);int n=calls.incrementAndGet();
   if(n>1)secondRequest.set(request);
   String response=n==1?"{\"choices\":[{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"list_my_orders\",\"arguments\":\"{}\"}}]}}]}":"{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"您的模拟订单待支付。\"}}]}";
   byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
  try {
   var gateway=new ModelGateway(json,"http://127.0.0.1:"+server.getAddress().getPort()+"/v1","test-key","test-model");
   var agent=new AgentService(shop,gateway,json);var result=agent.run(buyer,List.of(Map.of("author","BUYER","content","查询我的订单")));
   assertEquals("MODEL",result.mode());assertEquals(2,calls.get());assertEquals(1,result.trace().size());
   assertTrue(secondRequest.get().contains(order.get("id").toString()));assertFalse(secondRequest.get().contains("private-address"));
   assertEquals("PENDING",shop.order(order.get("id").toString(),buyer).get("status"));
  }finally{server.stop(0);}
 }
}
