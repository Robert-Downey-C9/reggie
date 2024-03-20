package com.itheima.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否登录的过滤器
 */
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    // 路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 1、获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("本次拦截的是：{}", requestURI);
        // 定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/login", // 移动端登录
                "/user/sendMsg" // 移动端短信
        };
        // 2、判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        // 3、判断不需要处理，则直接放行
        if (check){
            log.info("本次请求 {} 不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        // 4-1、判断登录状态，已登录，则直接放行
        if (request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户id为{}", request.getSession().getAttribute("employee"));

            // 将当前登录用户id放入ThreadLocal
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);
            return;
        }

        // 4-2、判断登录状态，已登录，则直接放行
        if (request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为{}", request.getSession().getAttribute("user"));

            // 将当前登录用户id放入ThreadLocal
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        // 5、如果未登录，则返回未登录结果,通过输出流的方式向客户端响应数据
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配，检查本次是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match){
                return true;
            }
        }
        return false;
    }

}
