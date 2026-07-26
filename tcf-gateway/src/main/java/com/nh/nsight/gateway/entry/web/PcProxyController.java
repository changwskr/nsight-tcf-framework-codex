package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pc")
public class PcProxyController extends AbstractBusinessProxyController {
    public PcProxyController(BusinessRouteService routeService) {
        super(routeService, "PC");
    }
}
