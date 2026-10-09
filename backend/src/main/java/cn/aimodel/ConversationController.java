package cn.aimodel;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class ConversationController {
 final Auth auth;final Conversations conversations;final AgentService agent;final Shop shop;
 public ConversationController(Auth a,Conversations c,AgentService agent,Shop shop){auth=a;conversations=c;this.agent=agent;this.shop=shop;}
 public record Content(@NotBlank@Size(max=3500)String content){}
 public record Mode(@NotBlank@Pattern(regexp="ASSISTED|HUMAN")String mode){}
 @GetMapping("/agent/status") public Object status(@RequestHeader(value="Authorization",required=false)String h){auth.merchant(h);return Map.of("configured",agent.configured(),"autoSend",false);}
 @PostMapping("/conversations/mine") public Object mine(@RequestHeader(value="Authorization",required=false)String h){return conversations.mine(auth.user(h));}
 @GetMapping("/conversations") public Object list(@RequestHeader(value="Authorization",required=false)String h){auth.merchant(h);return shop.jdbc().queryForList("select c.*,u.username,(select count(*) from messages m where m.conversation_id=c.id and m.status='DRAFT' and m.source_revision=c.revision) as draft_count,(select m.content from messages m where m.conversation_id=c.id and m.status='SENT' order by m.created_at desc,m.id desc limit 1) as last_message,(select m.author from messages m where m.conversation_id=c.id and m.status='SENT' order by m.created_at desc,m.id desc limit 1) as last_author,(select max(m.created_at) from messages m where m.conversation_id=c.id and m.status='SENT') as last_at from conversations c join users u on c.buyer_id=u.id order by last_at desc,u.username");}
 @GetMapping("/conversations/{id}/messages") public Object messages(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id){return conversations.messages(id,auth.user(h));}
 @PostMapping("/conversations/{id}/messages") public Object send(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Content r){return conversations.send(id,auth.user(h),r.content());}
 @PostMapping("/conversations/{id}/mode") public Object mode(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Mode r){return conversations.mode(id,auth.merchant(h),r.mode());}
 @PostMapping("/conversations/{id}/draft") public Object draft(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id) {
  var merchant=auth.merchant(h);var c=conversations.conversation(id,merchant);
  if(!c.get("mode").equals("ASSISTED"))throw Auth.fail(409,"人工接管中，Agent已暂停");
  var buyerRow=shop.jdbc().queryForMap("select * from users where id=?",c.get("buyer_id"));
  var buyer=new Auth.User(buyerRow.get("id").toString(),buyerRow.get("username").toString(),"BUYER");
  var all=conversations.messages(id,buyer);var history=all.subList(Math.max(0,all.size()-20),all.size());
  if(history.isEmpty())throw Auth.fail(409,"请等待客户消息");
  var result=agent.run(buyer,history);
  var message=conversations.saveDraft(id,merchant,((Number)c.get("revision")).longValue(),result);
  return Map.of("message",message,"mode",result.mode(),"trace",result.trace());
 }
 @PostMapping("/conversations/{id}/drafts/{message}/approve")public Object approve(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@PathVariable String message){return conversations.approve(id,message,auth.merchant(h));}
}
