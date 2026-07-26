package com.nh.nsight.tcf.ui.entry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OcEnvPageController {

    @GetMapping("/traceenvironment")
    public String legacyRoot() {
        return "redirect:/oc/env-001.html";
    }

    @GetMapping("/traceenvironment/env-001")
    public String legacyEnv001() {
        return "redirect:/oc/env-001.html";
    }

    @GetMapping("/traceenvironment/env-002")
    public String legacyEnv002() {
        return "redirect:/oc/env-002.html";
    }

    @GetMapping("/traceenvironment/env-003")
    public String legacyEnv003() {
        return "redirect:/oc/env-003.html";
    }

    @GetMapping("/traceenvironment/env-004")
    public String legacyEnv004() {
        return "redirect:/oc/env-004.html";
    }

    @GetMapping("/traceenvironment/check")
    public String legacyCheck() {
        return "redirect:/oc/check.html";
    }

    @GetMapping("/traceenvironment/rule-check")
    public String legacyRuleCheck() {
        return "redirect:/oc/rule-check.html";
    }

    @GetMapping({"/help", "/help/"})
    public String help() {
        return "redirect:/help.html";
    }

    @GetMapping({"/help/view", "/help/view/"})
    public String helpView() {
        return "redirect:/help/view.html";
    }

    @GetMapping({"/help/library", "/help/library/"})
    public String helpLibrary() {
        return "redirect:/help/library.html";
    }

    @GetMapping({"/help/health", "/help/health/"})
    public String helpHealth() {
        return "redirect:/help/health.html";
    }

    @GetMapping({"/error-popup", "/error-popup/"})
    public String legacyErrorPopup() {
        return "redirect:/help.html#errors";
    }

    @GetMapping({"/oc", "/oc/"})
    public String ocRoot() {
        return "redirect:/oc/index.html";
    }

    @GetMapping({"/oc/env", "/oc/env/"})
    public String ocEnvRoot() {
        return "redirect:/oc/env-001.html";
    }

    @GetMapping("/oc/env-001")
    public String env001() {
        return "redirect:/oc/env-001.html";
    }

    @GetMapping("/oc/env-002")
    public String env002() {
        return "redirect:/oc/env-002.html";
    }

    @GetMapping("/oc/env-003")
    public String env003() {
        return "redirect:/oc/env-003.html";
    }

    @GetMapping("/oc/env-004")
    public String env004() {
        return "redirect:/oc/env-004.html";
    }

    @GetMapping("/oc/check")
    public String check() {
        return "redirect:/oc/check.html";
    }

    @GetMapping("/oc/rule-check")
    public String ruleCheck() {
        return "redirect:/oc/rule-check.html";
    }

    @GetMapping({"/oc/plan", "/oc/plan/"})
    public String plan() {
        return "redirect:/oc/capacity.html";
    }

    @GetMapping({"/oc/capacity", "/oc/capacity/"})
    public String capacity() {
        return "redirect:/oc/capacity.html";
    }

    @GetMapping({"/oc/cap-new", "/oc/cap-new/"})
    public String capNewRoot() {
        return "redirect:/oc/cap-new/index.html";
    }

    @GetMapping("/oc/cap-new/wizard")
    public String capNewWizard() {
        return "redirect:/oc/cap-new/wizard.html";
    }

    @GetMapping("/oc/cap-new/compare")
    public String capNewCompare() {
        return "redirect:/oc/cap-new/compare.html";
    }

    @GetMapping("/oc/cap-new/approved")
    public String capNewApproved() {
        return "redirect:/oc/cap-new/approved.html";
    }
}
