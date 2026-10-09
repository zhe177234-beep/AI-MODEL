package cn.aimodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:business;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","merchant.password=TestMerchant12345!","agent.api-key=","agent.model="})
@AutoConfigureMockMvc
class BusinessFlowTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired Shop shop;@Autowired Auth auth;@Autowired Conversations conversations;
 String buyer1,buyer2,merchant;
 @BeforeEach void setup() throws Exception {
  buyer1=register("b"+UUID.randomUUID().toString().replace("-","").substring(0,10));
  buyer2=register("b"+UUID.randomUUID().toString().replace("-","").substring(0,10));
  merchant=login("merchant","TestMerchant12345!");
 }
 String register(String name)throws Exception{
  mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",name,"password","BuyerPassword123!")))).andExpect(status().isOk());return login(name,"BuyerPassword123!");
 }
 String login(String name,String password)throws Exception{return "Bearer "+json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("username",name,"password",password)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();}
 String order()throws Exception{return json.readTree(mvc.perform(post("/api/orders").header("Authorization",buyer1).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("productId","p-lamp","quantity",1,"address","演示地址","requestKey",UUID.randomUUID().toString())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asText();}
 @Test void ownershipAndRoleChecks()throws Exception{
  String id=order();
  mvc.perform(get("/api/orders/"+id).header("Authorization",buyer2)).andExpect(status().isNotFound());
  mvc.perform(post("/api/orders/"+id+"/ship").header("Authorization",buyer1).contentType(MediaType.APPLICATION_JSON).content("{\"tracking\":\"DEMO\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/conversations").header("Authorization",buyer1)).andExpect(status().isForbidden());
 }
 @Test void fullRefundFlowIsIdempotent()throws Exception{
  String id=order();mvc.perform(post("/api/orders/"+id+"/pay").header("Authorization",buyer1)).andExpect(status().isOk());
  String body=mvc.perform(post("/api/orders/"+id+"/refund").header("Authorization",buyer1).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"演示售后\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String refund=json.readTree(body).get("id").asText();int stock=((Number)shop.jdbc().queryForMap("select stock from products where id='p-lamp'").get("stock")).intValue();
  mvc.perform(post("/api/refunds/"+refund+"/approve").header("Authorization",buyer1)).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(post("/api/refunds/"+refund+"/approve").header("Authorization",merchant)).andExpect(status().isOk());
  assertEquals(stock+1,shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class));
  assertEquals("REFUNDED",shop.jdbc().queryForObject("select status from orders where id=?",String.class,id));
 }
 @Test void duplicateOrderReservesInventoryOnce()throws Exception{
  var u=auth.user(buyer1);String key=UUID.randomUUID().toString();int before=shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class);
  var first=shop.create(u,"p-lamp",1,"演示地址",key);var second=shop.create(u,"p-lamp",1,"演示地址",key);
  assertEquals(first.get("id"),second.get("id"));assertEquals(before-1,shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->shop.create(u,"p-lamp",2,"演示地址",key));
 }
 @Test void staleDraftCannotBeSentAndTakeoverStopsAgent()throws Exception{
  var buyer=auth.user(buyer1);var seller=auth.merchant(merchant);String c=conversations.mine(buyer).get("id").toString();
  conversations.send(c,buyer,"订单在哪里");long rev=((Number)conversations.conversation(c,seller).get("revision")).longValue();
  var draft=conversations.saveDraft(c,seller,rev,new AgentService.Result("待核实","MODEL",List.of()));
  assertEquals(1,conversations.messages(c,buyer).size());
  conversations.send(c,buyer,"补充订单信息");
  mvc.perform(post("/api/conversations/"+c+"/drafts/"+draft.get("id")+"/approve").header("Authorization",merchant)).andExpect(status().isConflict());
  conversations.mode(c,seller,"HUMAN");mvc.perform(post("/api/conversations/"+c+"/draft").header("Authorization",merchant)).andExpect(status().isConflict());
  mvc.perform(get("/api/conversations/"+c+"/messages").header("Authorization",buyer2)).andExpect(status().isNotFound());
 }
 @Test void insufficientStockRollsBack() {
  int before=shop.jdbc().queryForObject("select stock from products where id='p-keyboard'",Integer.class);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->shop.create(auth.user(buyer1),"p-keyboard",before+1,"地址",UUID.randomUUID().toString()));
  assertEquals(before,shop.jdbc().queryForObject("select stock from products where id='p-keyboard'",Integer.class));
 }
 @Test void cartCheckoutIsAtomicAndIdempotent() {
  var buyer=auth.user(buyer1);String key=UUID.randomUUID().toString();
  int before=shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class);
  shop.setCart(buyer,"p-lamp",2);shop.setCart(buyer,"p-keyboard",1);
  @SuppressWarnings("unchecked") var result=(List<Map<String,Object>>)shop.checkout(buyer,"cart-address",key);
  assertEquals(2,result.size());assertTrue(shop.cart(buyer).isEmpty());
  shop.checkout(buyer,"cart-address",key);
  assertEquals(before-2,shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class));
  shop.setCart(buyer,"p-keyboard",1000);shop.setCart(buyer,"p-lamp",1);
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->shop.checkout(buyer,"address",UUID.randomUUID().toString()));
  assertEquals(2,shop.cart(buyer).size());
  assertEquals(before-2,shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class));
 }

}
