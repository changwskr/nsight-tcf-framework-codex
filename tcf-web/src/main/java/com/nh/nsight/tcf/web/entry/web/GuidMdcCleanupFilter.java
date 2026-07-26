package com.nh.nsight.tcf.web.entry.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.nh.nsight.tcf.core.support.security.AuthenticationContextHolder;
import com.nh.nsight.tcf.web.support.AuthenticatedUserContextHolder;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GuidMdcCleanupFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("\n ======================================================================[GuidMdcCleanupFilter.doFilterInternal] start method="
                + request.getMethod() + " uri=" + request.getRequestURI());
        try {
            System.out.println(" ======================================================================[GuidMdcCleanupFilter.doFilterInternal] filterChain.doFilter");
            filterChain.doFilter(request, response);
            System.out.println(" ======================================================================[GuidMdcCleanupFilter.doFilterInternal] filterChain completed");
        } finally {
            System.out.println(" ======================================================================[GuidMdcCleanupFilter.doFilterInternal] MDC.clear");
            MDC.clear();
            AuthenticatedUserContextHolder.clear();
            AuthenticationContextHolder.clear();
            System.out.println(" ======================================================================[GuidMdcCleanupFilter.doFilterInternal] end");
        }
    }
}
