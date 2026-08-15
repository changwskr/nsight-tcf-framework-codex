package nhnis.infra.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 내장 UI 진입점. {@code /} 는 정적 index 로 보낸다.
 */
@Controller
public class InfraUiController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}
