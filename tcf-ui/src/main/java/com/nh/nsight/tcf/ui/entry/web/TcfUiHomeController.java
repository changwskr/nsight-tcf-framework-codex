package com.nh.nsight.tcf.ui.entry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TcfUiHomeController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "redirect:/index.html#moduleSection";
    }
}
