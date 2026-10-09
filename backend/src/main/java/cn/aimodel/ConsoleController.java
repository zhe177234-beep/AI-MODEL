package cn.aimodel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController @RequestMapping("/api/console")
public class ConsoleController {
 final Auth auth;final Shop shop;
 public ConsoleController(Auth auth,Shop shop){this.auth=auth;this.shop=shop;}
 public record Document(@NotBlank@Size(max=500000)String payload,@Min(0)long revision){}
 private void key(String id){if(!Set.of("settings","template").contains(id))throw Auth.fail(404,"配置不存在");}
 @GetMapping("/documents/{id}") public Object read(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id){auth.merchant(h);key(id);var rows=shop.jdbc().queryForList("select * from console_documents where id=?",id);return rows.isEmpty()?Map.of("id",id,"payload","{}","revision",0):rows.get(0);}
 @PostMapping("/documents/{id}") @Transactional public Object save(@RequestHeader(value="Authorization",required=false)String h,@PathVariable String id,@Valid@RequestBody Document d){var user=auth.merchant(h);key(id);
  try {if(!new com.fasterxml.jackson.databind.ObjectMapper().readTree(d.payload()).isObject())throw new IllegalArgumentException();}catch(Exception e){throw Auth.fail(400,"配置必须是合法JSON对象");}
  // Serialize writes even before a document exists; stale editors cannot overwrite another save.
  shop.jdbc().queryForList("select id from users where role='MERCHANT' order by id for update");
  var rows=shop.jdbc().queryForList("select * from console_documents where id=?",id);
  long revision=rows.isEmpty()?0:((Number)rows.get(0).get("revision")).longValue();
  if(revision!=d.revision())throw Auth.fail(409,"配置已被其他窗口更新，请刷新后重试");
  if(rows.isEmpty())shop.jdbc().update("insert into console_documents values (?,?,?)",id,d.payload(),1);
  else shop.jdbc().update("update console_documents set payload=?,revision=revision+1 where id=?",d.payload(),id);
  shop.audit(user.id(),"SAVE_CONSOLE",id,"保存工作台配置，版本 "+(revision+1));return Map.of("id",id,"revision",revision+1);
 }
}
