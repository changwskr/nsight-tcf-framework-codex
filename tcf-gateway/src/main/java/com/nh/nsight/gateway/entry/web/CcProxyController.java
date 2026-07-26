package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cc")
public class CcProxyController extends AbstractBusinessProxyController {
    public CcProxyController(BusinessRouteService routeService) {
        super(routeService, "CC");
    }
}
