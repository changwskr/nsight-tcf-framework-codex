package nhnis.mg.ui.entry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PdmgUiHomeController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "redirect:/index.html";
    }
}
