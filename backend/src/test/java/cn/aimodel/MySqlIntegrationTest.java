package cn.aimodel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"merchant.password=TestMerchant12345!"})
@EnabledIfEnvironmentVariable(named="MYSQL_TEST_URL",matches=".+")
class MySqlIntegrationTest {
 @DynamicPropertySource static void mysql(DynamicPropertyRegistry r){r.add("spring.datasource.url",()->System.getenv("MYSQL_TEST_URL"));r.add("spring.datasource.username",()->"merchant");r.add("spring.datasource.password",()->"ci-database-password");}
 @Autowired Auth auth;@Autowired Shop shop;
 @Test void migrationsAndTransactionalOrderRefundWorkOnMySql(){
  var buyer=auth.register("mysql"+UUID.randomUUID().toString().substring(0,8),"BuyerPassword123!");
  var seller=auth.user("Bearer "+auth.login("merchant","TestMerchant12345!").get("token"));
  int before=shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class);
  var order=shop.create(buyer,"p-lamp",1,"test-address",UUID.randomUUID().toString());String id=order.get("id").toString();
  shop.pay(buyer,id);var refund=shop.requestRefund(buyer,id,"test-refund");shop.approve(seller,refund.get("id").toString());shop.approve(seller,refund.get("id").toString());
  assertEquals("REFUNDED",shop.order(id,buyer).get("status"));assertEquals(before,shop.jdbc().queryForObject("select stock from products where id='p-lamp'",Integer.class));
 }
}
