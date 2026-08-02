package nhnis.mp.ui.entry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PdmpUiHomeController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "redirect:/index.html";
    }
}
