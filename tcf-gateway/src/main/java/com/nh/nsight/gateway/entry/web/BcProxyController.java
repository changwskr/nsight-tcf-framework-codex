package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bc")
public class BcProxyController extends AbstractBusinessProxyController {
    public BcProxyController(BusinessRouteService routeService) {
        super(routeService, "BC");
    }
}
