package com.nh.nsight.gateway.entry.web;

import com.nh.nsight.gateway.entry.facade.BusinessRouteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eb")
public class EbProxyController extends AbstractBusinessProxyController {
    public EbProxyController(BusinessRouteService routeService) {
        super(routeService, "EB");
    }
}
