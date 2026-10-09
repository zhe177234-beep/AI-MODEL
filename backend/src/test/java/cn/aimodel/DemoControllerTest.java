package cn.aimodel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DemoControllerTest {
    @Test void refundDoesNotExecuteAutomatically() {
        var reply = new DemoController().reply(new DemoController.Request("我要退款"));
        assertTrue(reply.requiresHuman());
        assertTrue(reply.reply().contains("尚未执行退款"));
        assertEquals("demo", reply.mode());
    }
}
