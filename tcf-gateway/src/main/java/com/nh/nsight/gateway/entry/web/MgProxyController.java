package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mg")
public class MgProxyController extends AbstractBusinessProxyController {
    public MgProxyController(BusinessRouteService routeService) {
        super(routeService, "MG");
    }
}
