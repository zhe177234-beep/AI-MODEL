package cn.aimodel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class DemoController {
    public record Request(@NotBlank @Size(max=2000) String message) {}
    public record Reply(String mode, String reply, boolean requiresHuman) {}
    @GetMapping("/health")
    public Map<String,String> health() { return Map.of("status","ok","mode","demo"); }
    @PostMapping("/demo/reply")
    public Reply reply(@Valid @RequestBody Request request) {
        String message = request.message();
        if (message.contains("退款") || message.contains("退货"))
            return new Reply("demo","这是演示建议：请提供订单编号和售后原因，由客服核实店铺政策后处理。尚未执行退款。",true);
        if (message.contains("物流") || message.contains("订单"))
            return new Reply("demo","这是演示建议：请提供订单编号。当前未连接订单系统，无法查询真实订单或物流。",true);
        return new Reply("demo","这是演示建议：请补充商品名称及具体问题，客服核实商品资料后回复。",true);
    }
}
