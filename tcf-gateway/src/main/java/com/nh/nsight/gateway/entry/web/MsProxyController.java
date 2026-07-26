package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ms")
public class MsProxyController extends AbstractBusinessProxyController {
    public MsProxyController(BusinessRouteService routeService) {
        super(routeService, "MS");
    }
}
