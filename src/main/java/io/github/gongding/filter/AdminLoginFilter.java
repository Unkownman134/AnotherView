package io.github.gongding.filter;

import io.github.gongding.entity.AdminEntity;
import io.github.gongding.service.AdminService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@WebFilter("/*")
public class AdminLoginFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AdminLoginFilter.class);
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60; // 7 days, consistent with other filters

    /**
     * 尝试使用自动登录Cookie进行登录
     * @param req HTTP请求对象
     * @param resp HTTP响应对象
     * @return 如果自动登录成功返回true，否则返回false
     * @throws IOException 输入输出异常
     */
    private boolean tryAutoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("尝试进行管理员自动登录。");
        Cookie[] cookies = req.getCookies();
        String name = null, password = null;

        if (cookies != null) {
            logger.trace("找到 {} 个 Cookie。", cookies.length);
            for (Cookie c : cookies) {
                if ("adminAutoLogin".equals(c.getName())) {
                    logger.debug("找到 adminAutoLogin Cookie。");
                    String cookieValue = c.getValue();
                    if (cookieValue != null && !cookieValue.isEmpty()) {
                        String[] parts = cookieValue.split(":");
                        if (parts.length == 2) {
                            name = parts[0];
                            password = parts[1];
                            logger.debug("从 Cookie 解析出名字和密码。");
                        } else {
                            logger.warn("adminAutoLogin Cookie 格式不正确: '{}'。", cookieValue);
                        }
                    } else {
                        logger.warn("adminAutoLogin Cookie 的值为 null 或空。");
                    }
                    break;
                }
            }
        } else {
            logger.trace("请求中没有 Cookie。");
        }

        if (name != null && password != null) {
            logger.debug("尝试使用名字 '{}' 进行自动登录。", name);
            AdminEntity admin = new AdminService().login(name, password);
            if (admin != null) {
                logger.info("名字 '{}' 自动登录成功。", name);
                HttpSession session = req.getSession(true);
                session.setAttribute("admin", admin);
                session.setMaxInactiveInterval(COOKIE_EXPIRATION);
                logger.debug("为名字 '{}' 设置 Session。", name);

                // Update auto-login cookie's expiration time or path
                Cookie auto = new Cookie("adminAutoLogin", name + ":" + password);
                auto.setMaxAge(COOKIE_EXPIRATION);
                auto.setPath("/Anotherview");
                resp.addCookie(auto);
                logger.debug("更新了 adminAutoLogin Cookie 的过期时间。");
                return true;
            } else {
                logger.warn("名字 '{}' 自动登录失败，用户名或密码不匹配。", name);
            }
        } else {
            logger.debug("未找到有效的自动登录凭据。");
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest req0, ServletResponse resp0, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) req0;
        HttpServletResponse resp = (HttpServletResponse) resp0;
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String remoteAddr = req.getRemoteAddr();
        logger.debug("收到来自 IP 地址 {} 的请求: {}", remoteAddr, uri);

        if (uri.equals(contextPath + "/api/adminLogin")) {
            logger.debug("请求 URI {} 是管理员登录 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }

        if (uri.startsWith(contextPath + "/api/student") || uri.startsWith(contextPath + "/api/student/") ||
                uri.startsWith(contextPath + "/api/teacher") || uri.startsWith(contextPath + "/api/teacher/")) {
            logger.debug("请求 URI {} 是学生与教师相关的 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }

        boolean isAdminPage = uri.startsWith(contextPath + "/html/admin/");
        boolean isAdminApi = uri.startsWith(contextPath + "/api/admin")||uri.startsWith(contextPath + "/api/admin/");
        boolean isLoginPage = uri.equals(contextPath + "/html/admin/admin-login.html");

        logger.trace("URI {} 判断 - isAdminPage: {}, isAdminApi: {}, isLoginPage: {}",
                uri, isAdminPage, isAdminApi, isLoginPage);

        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("admin") != null);
        logger.debug("当前 Session 中管理员登录状态: {}", isLoggedIn);

        if (isAdminPage || isAdminApi) {
            logger.debug("请求 URI {} 是管理员相关的页面或 API。", uri);

            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            logger.trace("已设置缓存控制头。");

            if (isLoginPage) {
                logger.debug("请求 URI {} 是管理员登录页面。", uri);
                if (isLoggedIn || tryAutoLogin(req, resp)) {
                    logger.debug("管理员已登录或自动登录成功，重定向到管理员主页。");
                    resp.sendRedirect(contextPath + "/html/admin/admin.html");
                    return;
                }
            } else {
                logger.debug("请求 URI {} 是非登录的管理页面或 API。", uri);
                if (!isLoggedIn && !tryAutoLogin(req, resp)) {
                    logger.warn("管理员未登录且自动登录失败，重定向到管理员登录页面。");
                    resp.sendRedirect(contextPath + "/html/admin/admin-login.html");
                    return;
                }
            }
        }

        logger.debug("请求 URI {} 不是管理员相关的，或者管理员登录检查通过，继续 Filter 链。", uri);
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AdminLoginFilter 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("AdminLoginFilter 销毁。");
    }
}
