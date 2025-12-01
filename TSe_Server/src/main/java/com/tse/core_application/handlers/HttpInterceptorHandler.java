package com.tse.core_application.handlers;

import com.tse.core_application.config.DebugConfig;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@Configuration     // when need to use this interceptor, add the annotation
public class HttpInterceptorHandler implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("===========pre handle ============");
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("========= post handle ===============");
        }
        ContentCachingResponseWrapper responseWrapper =new ContentCachingResponseWrapper(response);
        byte[] responseArray=responseWrapper.getContentAsByteArray();
        String responseStr=new String(responseArray,responseWrapper.getCharacterEncoding());
//        System.out.println("string==============" + responseStr);
//            /*It is important to copy cached reponse body back to response stream
//            to see response */
//        responseWrapper.copyBodyToResponse();
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("content ====");
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (DebugConfig.getInstance().isDebug()) {
            System.out.println("============== after completion ===========");
        }
    }


}
