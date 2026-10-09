package cn.aimodel;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class RequestGuard extends OncePerRequestFilter {
 private record Window(long start,int count){}
 private final ConcurrentHashMap<String,Window> attempts=new ConcurrentHashMap<>();
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("X-Frame-Options","DENY");res.setHeader("Referrer-Policy","same-origin");
  if(req.getRequestURI().startsWith("/api/"))res.setHeader("Cache-Control","no-store");
  if(req.getContentLengthLong()>20000){reject(res,413,"请求内容过大");return;}
  if(req.getMethod().equals("POST") && (req.getRequestURI().startsWith("/api/auth/"))) {
   long now=System.currentTimeMillis();String key=req.getRemoteAddr();
   if(attempts.size()>10000)attempts.entrySet().removeIf(e->now-e.getValue().start()>60000);
   Window w=attempts.compute(key,(k,old)->old==null||now-old.start()>60000?new Window(now,1):new Window(old.start(),old.count()+1));
   if(w.count()>60){reject(res,429,"登录操作过于频繁，请稍后重试");return;}
  }
  chain.doFilter(req,res);
 }
 private void reject(HttpServletResponse res,int code,String error)throws IOException{res.setStatus(code);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"error\":\""+error+"\"}");}
}
