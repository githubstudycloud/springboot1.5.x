//package com.study.gateway.filter;
//
//import com.netflix.zuul.ZuulFilter;
//import com.netflix.zuul.context.RequestContext;
//import javax.servlet.http.HttpServletRequest;
//
//public class AuthenticationFilter extends ZuulFilter {
//
//    @Override
//    public String filterType() {
//        return "pre";
//    }
//
//    @Override
//    public int filterOrder() {
//        return 1;
//    }
//
//    @Override
//    public boolean shouldFilter() {
//        return true;
//    }
//
//    @Override
//    public Object run() {
//        RequestContext ctx = RequestContext.getCurrentContext();
//        HttpServletRequest request = ctx.getRequest();
//        // Implement authentication logic here
//        return null;
//    }
//}
