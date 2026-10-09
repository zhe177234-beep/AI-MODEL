package cn.aimodel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final Auth auth;
 public AuthController(Auth auth){this.auth=auth;}
 public record Credentials(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,32}") String username,@NotBlank @Size(min=12,max=72) String password){}
 @PostMapping("/register") public Auth.User register(@Valid @RequestBody Credentials c){return auth.register(c.username(),c.password());}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Credentials c){return auth.login(c.username(),c.password());}
 @GetMapping("/me") public Auth.User me(@RequestHeader(value="Authorization",required=false)String h){return auth.user(h);}
 @PostMapping("/logout") public Map<String,Boolean> logout(@RequestHeader(value="Authorization",required=false)String h){auth.logout(h);return Map.of("ok",true);}
}
