package cn.aimodel;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@Service
public class Auth {
 private final JdbcTemplate db;
 private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
 public record User(String id,String username,String role) {}
 public Auth(JdbcTemplate db) {this.db=db;}
 @Bean ApplicationRunner bootstrap(@Value("${merchant.username}") String username,@Value("${merchant.password}") String password) {
  return args -> {
   if (!password.isBlank() && db.queryForObject("select count(*) from users where role='MERCHANT'",Integer.class)==0) {
    if(password.length()<12) throw new IllegalStateException("MERCHANT_PASSWORD must be at least 12 characters");
    db.update("insert into users values (?,?,?,?)",UUID.randomUUID().toString(),username,encoder.encode(password),"MERCHANT");
   }
  };
 }
 public User register(String username,String password) {
  if(password.getBytes(StandardCharsets.UTF_8).length>72)throw fail(400,"密码UTF-8编码长度不能超过72字节");
  String id=UUID.randomUUID().toString();
  db.update("insert into users values (?,?,?,?)",id,username,encoder.encode(password),"BUYER");
  return new User(id,username,"BUYER");
 }
 public Map<String,Object> login(String username,String password) {
  var rows=db.queryForList("select * from users where username=?",username);
  if(rows.isEmpty() || !encoder.matches(password,rows.get(0).get("password_hash").toString())) throw fail(401,"账号或密码错误");
  var row=rows.get(0);byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
  String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  db.update("delete from sessions where expires_at<?",System.currentTimeMillis());
  db.update("insert into sessions values (?,?,?)",hash(token),row.get("id"),System.currentTimeMillis()+86400000);
  return Map.of("token",token,"user",new User(row.get("id").toString(),username,row.get("role").toString()));
 }
 public User user(String authorization) {
  if(authorization==null || !authorization.startsWith("Bearer ")) throw fail(401,"请先登录");
  var rows=db.queryForList("select u.id,u.username,u.role from sessions s join users u on u.id=s.user_id where s.token_hash=? and s.expires_at>?",hash(authorization.substring(7)),System.currentTimeMillis());
  if(rows.isEmpty()) throw fail(401,"登录已失效");
  var row=rows.get(0);return new User(row.get("id").toString(),row.get("username").toString(),row.get("role").toString());
 }
 public User merchant(String authorization) {var u=user(authorization);if(!u.role().equals("MERCHANT"))throw fail(403,"需要客服权限");return u;}
 public void logout(String authorization) {user(authorization);db.update("delete from sessions where token_hash=?",hash(authorization.substring(7)));}
 static ResponseStatusException fail(int status,String message) {return new ResponseStatusException(HttpStatus.valueOf(status),message);}
 private static String hash(String input) {
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
 }
}
