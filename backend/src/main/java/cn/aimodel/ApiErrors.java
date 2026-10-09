package cn.aimodel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class)
 public org.springframework.http.ResponseEntity<?> status(ResponseStatusException e) {
  return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()==null?"请求失败":e.getReason()));
 }
 @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
 @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(Exception e) { return Map.of("error","输入不符合要求"); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 @ResponseStatus(HttpStatus.CONFLICT) public Map<String,String> conflict(Exception e) { return Map.of("error","数据已存在或操作冲突，请刷新后重试"); }
}
