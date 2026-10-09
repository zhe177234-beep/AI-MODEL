package cn.aimodel;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class Shop {
 final JdbcTemplate db;
 public Shop(JdbcTemplate db){this.db=db;}
 public JdbcTemplate jdbc(){return db;}
 public List<Map<String,Object>> products(){return db.queryForList("select * from products order by name");}
 public List<Map<String,Object>> orders(Auth.User user){return user.role().equals("MERCHANT")?db.queryForList("select * from orders order by created_at desc"):db.queryForList("select * from orders where buyer_id=? order by created_at desc",user.id());}
 public Map<String,Object> order(String id,Auth.User user) {
  var rows=db.queryForList("select * from orders where id=?",id);
  if(rows.isEmpty())throw Auth.fail(404,"订单不存在");
  var row=rows.get(0);
  if(!user.role().equals("MERCHANT") && !row.get("buyer_id").equals(user.id()))throw Auth.fail(404,"订单不存在");
  return row;
 }
 @Transactional public Map<String,Object> create(Auth.User user,String product,int quantity,String address,String key) {
  if(!user.role().equals("BUYER"))throw Auth.fail(403,"请用买家账号下单");
  db.queryForList("select id from users where id=? for update",user.id());
  var existing=db.queryForList("select * from orders where buyer_id=? and request_key=?",user.id(),key);
  if(!existing.isEmpty()) {
   var row=existing.get(0);
   if(!row.get("product_id").equals(product) || ((Number)row.get("quantity")).intValue()!=quantity || !row.get("address").equals(address))throw Auth.fail(409,"同一请求编号不能用于不同订单");
   return row;
  }
  var products=db.queryForList("select * from products where id=? for update",product);
  if(products.isEmpty())throw Auth.fail(404,"商品不存在");
  var p=products.get(0);int stock=((Number)p.get("stock")).intValue();
  if(stock<quantity)throw Auth.fail(409,"库存不足");
  String id=UUID.randomUUID().toString();
  db.update("update products set stock=stock-? where id=?",quantity,product);
  db.update("insert into orders (id,buyer_id,product_id,quantity,total_cents,status,address,created_at,request_key) values (?,?,?,?,?,'PENDING',?,?,?)",id,user.id(),product,quantity,Math.multiplyExact(((Number)p.get("price_cents")).intValue(),quantity),address,System.currentTimeMillis(),key);
  audit(user.id(),"CREATE_ORDER",id,"模拟订单");return order(id,user);
 }
 @Transactional public Map<String,Object> pay(Auth.User user,String id) {
  var row=order(id,user);
  if(!row.get("buyer_id").equals(user.id()))throw Auth.fail(403,"只能支付自己的订单");
  if(row.get("status").equals("PAID"))return row;
  if(db.update("update orders set status='PAID' where id=? and status='PENDING'",id)!=1)throw Auth.fail(409,"订单状态不支持支付");
  audit(user.id(),"SIMULATE_PAYMENT",id,"未产生真实资金交易");return order(id,user);
 }
 @Transactional public Map<String,Object> ship(Auth.User user,String id,String tracking) {
  if(!user.role().equals("MERCHANT"))throw Auth.fail(403,"需要客服权限");
  order(id,user);
  if(db.update("update orders set status='SHIPPED',tracking=? where id=? and status='PAID'",tracking,id)!=1)throw Auth.fail(409,"仅已支付订单可以发货");
  audit(user.id(),"SIMULATE_SHIPMENT",id,tracking);return order(id,user);
 }
 @Transactional public Map<String,Object> requestRefund(Auth.User user,String id,String reason) {
  var locked=db.queryForList("select * from orders where id=? for update",id);
  order(id,user);
  if(locked.isEmpty() || !locked.get(0).get("buyer_id").equals(user.id()))throw Auth.fail(403,"只能申请自己的订单售后");
  String status=locked.get(0).get("status").toString();
  if(!Set.of("PAID","SHIPPED").contains(status))throw Auth.fail(409,"该订单不支持申请售后");
  var existing=db.queryForList("select * from refunds where order_id=?",id);
  if(!existing.isEmpty())return existing.get(0);
  String refund=UUID.randomUUID().toString();
  db.update("insert into refunds values (?,?,?,'PENDING',?)",refund,id,reason,System.currentTimeMillis());
  audit(user.id(),"REQUEST_REFUND",id,reason);
  return db.queryForMap("select * from refunds where id=?",refund);
 }
 @Transactional public Map<String,Object> approve(Auth.User user,String id) {
  if(!user.role().equals("MERCHANT"))throw Auth.fail(403,"需要客服权限");
  var refunds=db.queryForList("select * from refunds where id=? for update",id);
  if(refunds.isEmpty())throw Auth.fail(404,"售后申请不存在");
  var refund=refunds.get(0);String orderId=refund.get("order_id").toString();
  if(refund.get("status").equals("APPROVED"))return refund;
  var order=order(orderId,user);
  if(db.update("update orders set status='REFUNDED' where id=? and status in ('PAID','SHIPPED')",orderId)!=1)throw Auth.fail(409,"订单状态不支持退款");
  if(order.get("status").equals("PAID")) db.update("update products set stock=stock+? where id=?",order.get("quantity"),order.get("product_id"));
  db.update("update refunds set status='APPROVED' where id=?",id);
  audit(user.id(),"APPROVE_SIMULATED_REFUND",orderId,"人工审批；未产生真实资金转移");
  return db.queryForMap("select * from refunds where id=?",id);
 }
 public List<Map<String,Object>> refunds(Auth.User user) {
  return user.role().equals("MERCHANT")?db.queryForList("select * from refunds order by created_at desc"):db.queryForList("select r.* from refunds r join orders o on r.order_id=o.id where o.buyer_id=? order by r.created_at desc",user.id());
 }
 public List<Map<String,Object>> cart(Auth.User user) {
  if(!user.role().equals("BUYER"))throw Auth.fail(403,"需要买家账号");
  return db.queryForList("select c.product_id,c.quantity,p.name,p.price_cents,p.stock from cart_items c join products p on c.product_id=p.id where c.buyer_id=? order by c.product_id",user.id());
 }
 @Transactional public Object setCart(Auth.User user,String product,int quantity) {
  if(!user.role().equals("BUYER"))throw Auth.fail(403,"需要买家账号");
  db.queryForList("select id from users where id=? for update",user.id());
  if(db.queryForObject("select count(*) from products where id=?",Integer.class,product)==0)throw Auth.fail(404,"商品不存在");
  db.update("delete from cart_items where buyer_id=? and product_id=?",user.id(),product);
  if(quantity>0)db.update("insert into cart_items values (?,?,?)",user.id(),product,quantity);
  return cart(user);
 }
 @Transactional public Object checkout(Auth.User user,String address,String key) {
  if(!user.role().equals("BUYER"))throw Auth.fail(403,"需要买家账号");
  db.queryForList("select id from users where id=? for update",user.id());
  var existing=db.queryForList("select * from orders where buyer_id=? and checkout_key=? order by product_id",user.id(),key);
  if(!existing.isEmpty()){
   if(existing.stream().anyMatch(o->!o.get("address").equals(address)))throw Auth.fail(409,"同一结算请求不能修改地址");
   return existing;
  }
  var items=cart(user);if(items.isEmpty())throw Auth.fail(409,"购物车为空");
  List<Map<String,Object>> result=new ArrayList<>();
  for(var item:items) {
   String product=item.get("product_id").toString();
   String requestKey;
   try {requestKey=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((key+"/"+product).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
   catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
   var order=create(user,product,((Number)item.get("quantity")).intValue(),address,requestKey);
   db.update("update orders set checkout_key=? where id=?",key,order.get("id"));result.add(order);
  }
  db.update("delete from cart_items where buyer_id=?",user.id());return result;
 }
 public List<Map<String,Object>> knowledge(){return db.queryForList("select * from knowledge order by title");}
 public void audit(String actor,String action,String entity,String detail){db.update("insert into audit values (?,?,?,?,?,?)",UUID.randomUUID().toString(),actor,action,entity,detail.substring(0,Math.min(detail.length(),4000)),System.currentTimeMillis());}
}
