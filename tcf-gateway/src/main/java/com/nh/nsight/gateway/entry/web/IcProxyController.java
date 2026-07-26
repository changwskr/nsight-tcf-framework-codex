package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ic")
public class IcProxyController extends AbstractBusinessProxyController {
    public IcProxyController(BusinessRouteService routeService) {
        super(routeService, "IC");
    }
}
