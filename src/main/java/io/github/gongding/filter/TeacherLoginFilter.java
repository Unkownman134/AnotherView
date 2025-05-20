package io.github.gongding.filter;

import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherService;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter("/*")
public class TeacherLoginFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TeacherLoginFilter.class);
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    /**
     * 尝试使用自动登录Cookie进行登录
     * @param req HTTP请求对象
     * @param resp HTTP响应对象
     * @return 如果自动登录成功返回true，否则返回false
     * @throws IOException 输入输出异常
     */
    private boolean tryAutoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("尝试进行教师自动登录。");
        Cookie[] cookies = req.getCookies();
        String name = null, password = null;
        if (cookies != null) {
            logger.trace("找到 {} 个 Cookie。", cookies.length);
            for (Cookie c : cookies) {
                if ("teacherAutoLogin".equals(c.getName())) {
                    logger.debug("找到 teacherAutoLogin Cookie。");
                    String cookieValue = c.getValue();
                    if (cookieValue != null && !cookieValue.isEmpty()) {
                        String[] p = cookieValue.split(":");
                        if (p.length == 2) {
                            name = p[0];
                            password = p[1];
                            logger.debug("从 Cookie 解析出姓名和密码。");
                        } else {
                            logger.warn("teacherAutoLogin Cookie 格式不正确: '{}'。", cookieValue);
                        }
                    } else {
                        logger.warn("teacherAutoLogin Cookie 的值为 null 或空。");
                    }
                    break;
                }
            }
        } else {
            logger.trace("请求中没有 Cookie。");
        }

        if (name != null && password != null) {
            logger.debug("尝试使用姓名 '{}' 进行自动登录。", name);

            TeacherEntity t = new TeacherService().login(name, password);
            if (t != null) {
                logger.info("教师姓名 '{}' 自动登录成功。", name);
                HttpSession session = req.getSession(true);
                session.setAttribute("teacher", t);
                session.setMaxInactiveInterval(COOKIE_EXPIRATION);
                logger.debug("为教师姓名 '{}' 设置 Session。", name);


                Cookie auto = new Cookie("teacherAutoLogin", name + ":" + password);
                auto.setMaxAge(COOKIE_EXPIRATION);
                auto.setPath("/Anotherview");
                resp.addCookie(auto);
                logger.debug("更新了 teacherAutoLogin Cookie 的过期时间。");
                return true;
            } else {
                logger.warn("教师姓名 '{}' 自动登录失败，用户名或密码不匹配。", name);
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

        if (uri.equals(contextPath + "/api/teacherLogin") || uri.equals(contextPath + "/api/teacherRegister")) {
            logger.debug("请求 URI {} 是教师登录或注册 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }
        if (uri.startsWith(contextPath + "/api/student")) {
            logger.debug("请求 URI {} 是学生相关的 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }

        boolean isTeacherPage = uri.startsWith(contextPath + "/html/teacher/");
        boolean isTeacherApi = uri.startsWith(contextPath + "/api/teacher")||uri.startsWith(contextPath + "/api/teacher/");
        boolean isLoginPage = uri.equals(contextPath + "/html/teacher/teacher-login.html");
        boolean isRegisterPage = uri.equals(contextPath + "/html/teacher/teacher-register.html");
        logger.trace("URI {} 判断 - isTeacherPage: {}, isTeacherApi: {}, isLoginPage: {}, isRegisterPage: {}",
                uri, isTeacherPage, isTeacherApi, isLoginPage, isRegisterPage);


        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("teacher") != null);
        logger.debug("当前 Session 中教师登录状态: {}", isLoggedIn);


        if (isTeacherPage || isTeacherApi) {
            logger.debug("请求 URI {} 是教师相关的页面或 API。", uri);
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            logger.trace("已设置缓存控制头。");


            if (isLoginPage || isRegisterPage) {
                logger.debug("请求 URI {} 是教师登录或注册页面。", uri);
                if (isLoggedIn || tryAutoLogin(req, resp)) {
                    logger.debug("教师已登录或自动登录成功，重定向到教师主页。");
                    resp.sendRedirect(contextPath + "/html/teacher/teacher.html");
                    return;
                }
            } else {
                logger.debug("请求 URI {} 是非登录/注册的教师页面或 API。", uri);
                if (!isLoggedIn && !tryAutoLogin(req, resp)) {
                    logger.warn("教师未登录且自动登录失败，重定向到教师登录页面。");
                    resp.sendRedirect(contextPath + "/html/teacher/teacher-login.html");
                    return;
                }
            }
        }
        logger.debug("请求 URI {} 不是教师相关的，或者教师登录检查通过，继续 Filter 链。", uri);
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("TeacherLoginFilter 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherLoginFilter 销毁。");
    }
}
