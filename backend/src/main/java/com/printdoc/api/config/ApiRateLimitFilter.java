package com.printdoc.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private record Window(long startedAt,AtomicInteger count){}
    private final ConcurrentHashMap<String,Window> buckets=new ConcurrentHashMap<>();
    private static final int MAX_PER_MINUTE=120;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String path=request.getRequestURI();if(!path.startsWith("/api/v1/auth/")&&!path.startsWith("/api/v1/public/")&&!path.equals("/api/v1/devices/check-status")&&!path.equals("/api/devices/check-status")){chain.doFilter(request,response);return;}
        String key=request.getRemoteAddr()+":"+path;long now=System.currentTimeMillis();Window window=buckets.compute(key,(k,old)->old==null||now-old.startedAt()>60_000?new Window(now,new AtomicInteger()):old);
        if(window.count().incrementAndGet()>MAX_PER_MINUTE){response.setStatus(429);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Try again shortly.\",\"data\":null,\"timestamp\":\""+Instant.now()+"\"}");return;}
        chain.doFilter(request,response);
        if(buckets.size()>10_000)buckets.entrySet().removeIf(e->now-e.getValue().startedAt()>120_000);
    }
}
