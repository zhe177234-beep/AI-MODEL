package cn.aimodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tools;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","merchant.password=TestMerchant12345!"})
class AgentToolsTest {
 @Autowired AgentService agent;@Autowired Shop shop;@Autowired Auth auth;@Autowired ObjectMapper json;
 @Test void modelCannotReadOtherBuyerOrCallWriteTool()throws Exception{
  var first=auth.register("a"+UUID.randomUUID().toString().substring(0,8),"PasswordBuyer123!");
  var second=auth.register("b"+UUID.randomUUID().toString().substring(0,8),"PasswordBuyer123!");
  var order=shop.create(first,"p-lamp",1,"address",UUID.randomUUID().toString());
  assertThrows(Exception.class,()->agent.tool("get_my_order",json.readTree("{\"orderId\":\""+order.get("id")+"\"}"),second));
  assertThrows(Exception.class,()->agent.tool("approve_refund",json.createObjectNode(),first));
  assertThrows(Exception.class,()->agent.tool("list_my_orders",json.readTree("{\"buyerId\":\"other\"}"),first));
  String result=json.writeValueAsString(agent.tool("list_my_orders",json.createObjectNode(),first));assertFalse(result.contains("address"));assertFalse(result.contains("buyer_id"));
 }
}
