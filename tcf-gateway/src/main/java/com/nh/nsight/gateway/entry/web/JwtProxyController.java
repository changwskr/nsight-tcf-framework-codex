package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jwt")
public class JwtProxyController extends AbstractBusinessProxyController {

    public JwtProxyController(BusinessRouteService routeService) {
        super(routeService, "JWT");
    }
}
