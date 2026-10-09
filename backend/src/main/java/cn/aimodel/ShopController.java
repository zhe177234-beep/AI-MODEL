package cn.aimodel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class ShopController {
 final Auth auth;final Shop shop;
 public ShopController(Auth auth,Shop shop){this.auth=auth;this.shop=shop;}
 public record CreateOrder(@NotBlank String productId,@Min(1)@Max(20) int quantity,@NotBlank@Size(max=500)String address,@NotBlank@Size(max=64)String requestKey){}
 public record Reason(@NotBlank@Size(max=1000)String reason){}
 public record Tracking(@NotBlank@Size(max=100)String tracking){}
 public record Knowledge(@NotBlank@Size(max=150)String title,@NotBlank@Size(max=3000)String content){}
 public record CartItem(@NotBlank String productId,@Min(0)@Max(20)int quantity){}
 public record Checkout(@NotBlank@Size(max=500)String address,@NotBlank@Size(max=64)String requestKey){}
 @GetMapping("/cart")public Object cart(@RequestHeader(value="Authorization",required=false)String h){return shop.cart(auth.user(h));}
 @PostMapping("/cart/items")public Object cartItem(@RequestHeader(value="Authorization",required=false)String h,@Valid@RequestBody CartItem r){return shop.setCart(auth.user(h),r.productId(),r.quantity());}
 @PostMapping("/cart/checkout")public Object checkout(@RequestHeader(value="Authorization",required=false)String h,@Valid@RequestBody Checkout r){return shop.checkout(auth.user(h),r.address(),r.requestKey());}
 @GetMapping("/products") public Object products(){return shop.products();}
 @GetMapping("/orders") public Object orders(@RequestHeader(value="Authorization",required=false)String h){return shop.orders(auth.user(h));}
 @GetMapping("/orders/{id}") public Object order(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id){return shop.order(id,auth.user(h));}
 @PostMapping("/orders") public Object create(@RequestHeader(value="Authorization",required=false)String h,@Valid@RequestBody CreateOrder r){return shop.create(auth.user(h),r.productId(),r.quantity(),r.address(),r.requestKey());}
 @PostMapping("/orders/{id}/pay") public Object pay(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id){return shop.pay(auth.user(h),id);}
 @PostMapping("/orders/{id}/ship") public Object ship(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Tracking r){return shop.ship(auth.merchant(h),id,r.tracking());}
 @PostMapping("/orders/{id}/refund") public Object refund(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Reason r){return shop.requestRefund(auth.user(h),id,r.reason());}
 @GetMapping("/refunds") public Object refunds(@RequestHeader(value="Authorization",required=false)String h){return shop.refunds(auth.user(h));}
 @PostMapping("/refunds/{id}/approve") public Object approve(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id){return shop.approve(auth.merchant(h),id);}
 @GetMapping("/knowledge") public Object knowledge(){return shop.knowledge();}
 @PostMapping("/knowledge") public Object knowledge(@RequestHeader(value="Authorization",required=false)String h,@Valid@RequestBody Knowledge r){var user=auth.merchant(h);String id=UUID.randomUUID().toString();shop.jdbc().update("insert into knowledge values (?,?,?)",id,r.title(),r.content());shop.audit(user.id(),"ADD_KNOWLEDGE",id,r.title());return Map.of("id",id);}
 @PostMapping("/knowledge/{id}") public Object editKnowledge(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Knowledge r){var user=auth.merchant(h);if(shop.jdbc().update("update knowledge set title=?,content=? where id=?",r.title(),r.content(),id)==0)throw Auth.fail(404,"知识不存在");shop.audit(user.id(),"EDIT_KNOWLEDGE",id,r.title());return Map.of("id",id);}
 @GetMapping("/audit") public Object audit(@RequestHeader(value="Authorization",required=false)String h){auth.merchant(h);return shop.jdbc().queryForList("select * from audit order by created_at desc limit 100");}
}
