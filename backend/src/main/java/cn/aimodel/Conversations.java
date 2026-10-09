package cn.aimodel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class Conversations {
 final Shop shop;
 public Conversations(Shop shop){this.shop=shop;}
 @Transactional public Map<String,Object> mine(Auth.User user) {
  if(!user.role().equals("BUYER"))throw Auth.fail(403,"需要买家账号");
  shop.jdbc().queryForList("select id from users where id=? for update",user.id());
  var rows=shop.jdbc().queryForList("select * from conversations where buyer_id=?",user.id());
  if(!rows.isEmpty())return rows.get(0);
  String id=UUID.randomUUID().toString();shop.jdbc().update("insert into conversations (id,buyer_id) values (?,?)",id,user.id());return conversation(id,user);
 }
 public Map<String,Object> conversation(String id,Auth.User user) {
  var rows=shop.jdbc().queryForList("select * from conversations where id=?",id);
  if(rows.isEmpty() || (!user.role().equals("MERCHANT") && !rows.get(0).get("buyer_id").equals(user.id())))throw Auth.fail(404,"会话不存在");
  return rows.get(0);
 }
 public List<Map<String,Object>> messages(String id,Auth.User user) {
  conversation(id,user);
  return user.role().equals("MERCHANT")?shop.jdbc().queryForList("select * from messages where conversation_id=? order by created_at,id",id):shop.jdbc().queryForList("select * from messages where conversation_id=? and status='SENT' order by created_at,id",id);
 }
 @Transactional public Map<String,Object> send(String id,Auth.User user,String content) {
  shop.jdbc().queryForList("select * from conversations where id=? for update",id);conversation(id,user);
  shop.jdbc().update("update conversations set revision=revision+1 where id=?",id);
  return insert(id,user.role(),content,"SENT",0);
 }
 Map<String,Object> insert(String conversation,String author,String content,String status,long revision){String id=UUID.randomUUID().toString();long last=shop.jdbc().queryForObject("select coalesce(max(created_at),0) from messages where conversation_id=?",Long.class,conversation);long timestamp=Math.max(System.currentTimeMillis(),last+1);shop.jdbc().update("insert into messages values (?,?,?,?,?,?,?)",id,conversation,author,content,status,timestamp,revision);return shop.jdbc().queryForMap("select * from messages where id=?",id);}
 @Transactional public Map<String,Object> saveDraft(String id,Auth.User merchant,long revision,AgentService.Result result) {
  shop.jdbc().queryForList("select * from conversations where id=? for update",id);var c=conversation(id,merchant);
  if(!c.get("mode").equals("ASSISTED") || ((Number)c.get("revision")).longValue()!=revision)throw Auth.fail(409,"会话已变化或被接管，请重新生成");
  var message=insert(id,"AGENT",result.content(),"DRAFT",revision);
  shop.audit(merchant.id(),"AGENT_DRAFT",message.get("id").toString(),result.mode()+" "+result.trace().toString());return message;
 }
 @Transactional public Map<String,Object> approve(String conversation,String message,Auth.User merchant) {
  shop.jdbc().queryForList("select * from conversations where id=? for update",conversation);var c=conversation(conversation,merchant);
  var rows=shop.jdbc().queryForList("select * from messages where id=? and conversation_id=?",message,conversation);
  if(rows.isEmpty())throw Auth.fail(404,"草稿不存在");var draft=rows.get(0);
  if(draft.get("status").equals("SENT"))return draft;
  if(!draft.get("author").equals("AGENT") || !c.get("mode").equals("ASSISTED") || ((Number)c.get("revision")).longValue()!=((Number)draft.get("source_revision")).longValue())throw Auth.fail(409,"草稿已失效，请重新生成或人工回复");
  shop.jdbc().update("update messages set status='SENT' where id=?",message);shop.jdbc().update("update conversations set revision=revision+1 where id=?",conversation);
  shop.audit(merchant.id(),"APPROVE_REPLY",message,"人工审核发送");return shop.jdbc().queryForMap("select * from messages where id=?",message);
 }
 @Transactional public Object mode(String id,Auth.User user,String mode){conversation(id,user);shop.jdbc().update("update conversations set mode=?,revision=revision+1 where id=?",mode,id);shop.audit(user.id(),"CHANGE_MODE",id,mode);return conversation(id,user);}
}
