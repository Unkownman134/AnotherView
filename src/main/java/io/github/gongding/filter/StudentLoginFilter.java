package io.github.gongding.filter;

import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
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
public class StudentLoginFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(StudentLoginFilter.class);
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    /**
     * 尝试使用自动登录Cookie进行登录
     * @param req HTTP请求对象
     * @param resp HTTP响应对象
     * @return 如果自动登录成功返回true，否则返回false
     * @throws IOException 输入输出异常
     */
    private boolean tryAutoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("尝试进行学生自动登录。");
        //获取请求中所有的 Cookie
        Cookie[] cookies = req.getCookies();
        String studentNumber = null, password = null;
        if (cookies != null) {
            logger.trace("找到 {} 个 Cookie。", cookies.length);
            for (Cookie c : cookies) {
                //查找名为"studentAutoLogin"的Cookie
                if ("studentAutoLogin".equals(c.getName())) {
                    logger.debug("找到 studentAutoLogin Cookie。");
                    String cookieValue = c.getValue();
                    if (cookieValue != null && !cookieValue.isEmpty()) {
                        String[] p = cookieValue.split(":");
                        if (p.length == 2) {
                            studentNumber = p[0];
                            password = p[1];
                            logger.debug("从 Cookie 解析出学号和密码。");
                        } else {
                            logger.warn("studentAutoLogin Cookie 格式不正确: '{}'。", cookieValue);
                        }
                    } else {
                        logger.warn("studentAutoLogin Cookie 的值为 null 或空。");
                    }
                    break;
                }
            }
        } else {
            logger.trace("请求中没有 Cookie。");
        }

        if (studentNumber != null && password != null) {
            logger.debug("尝试使用学号 '{}' 进行自动登录。", studentNumber);
            StudentEntity s = new StudentService().login(studentNumber, password);
            //验证通过
            if (s != null) {
                logger.info("学号 '{}' 自动登录成功。", studentNumber);
                HttpSession session = req.getSession(true);
                //将登录成功的学生信息存储到Session中
                session.setAttribute("student", s);
                session.setMaxInactiveInterval(COOKIE_EXPIRATION);
                logger.debug("为学号 '{}' 设置 Session。", studentNumber);

                //更新Cookie的过期时间或路径
                Cookie auto = new Cookie("studentAutoLogin", studentNumber + ":" + password);
                auto.setMaxAge(COOKIE_EXPIRATION);
                auto.setPath("/Anotherview");
                resp.addCookie(auto);
                logger.debug("更新了 studentAutoLogin Cookie 的过期时间。");
                return true;
            } else {
                logger.warn("学号 '{}' 自动登录失败，用户名或密码不匹配。", studentNumber);
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

        //排除学生登录和注册相关的API请求，直接放行
        if (uri.equals(contextPath + "/api/studentLogin") || uri.equals(contextPath + "/api/studentRegister")) {
            logger.debug("请求 URI {} 是学生登录或注册 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }
        //排除教师相关的API请求，直接放行
        if (uri.startsWith(contextPath + "/api/teacher") || uri.startsWith(contextPath + "/api/teacher/")) {
            logger.debug("请求 URI {} 是教师相关的 API，直接放行。", uri);
            chain.doFilter(req, resp);
            return;
        }

        boolean isStudentPage = uri.startsWith(contextPath + "/html/student/");
        boolean isStudentApi = uri.startsWith(contextPath + "/api/student");
        boolean isLoginPage = uri.equals(contextPath + "/html/student/student-login.html");
        boolean isRegisterPage = uri.equals(contextPath + "/html/student/student-register.html");

        //检查当前Session中是否有学生登录信息
        HttpSession session = req.getSession(false);
        //判断用户是否已登录
        boolean isLoggedIn = (session != null && session.getAttribute("student") != null);
        logger.debug("当前 Session 中学生登录状态: {}", isLoggedIn);

        //如果是学生相关的页面或API请求
        if (isStudentPage || isStudentApi) {
            logger.debug("请求 URI {} 是学生相关的页面或 API。", uri);
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            logger.trace("已设置缓存控制头。");

            //如果当前请求是学生登录页面或注册页面
            if (isLoginPage || isRegisterPage) {
                logger.debug("请求 URI {} 是学生登录或注册页面。", uri);
                //如果用户已经登录或者尝试自动登录成功
                if (isLoggedIn || tryAutoLogin(req, resp)) {
                    logger.debug("用户已登录或自动登录成功，重定向到学生主页。");
                    resp.sendRedirect(contextPath + "/html/student/student.html");
                    return;
                }
            } else {
                logger.debug("请求 URI {} 是非登录/注册的学生页面或 API。", uri);
                //如果用户未登录并且尝试自动登录也失败了
                if (!isLoggedIn && !tryAutoLogin(req, resp)) {
                    logger.warn("用户未登录且自动登录失败，重定向到学生登录页面。");
                    resp.sendRedirect(contextPath + "/html/student/student-login.html");
                    return;
                }
            }
        }

        logger.debug("请求 URI {} 不是学生相关的，或者学生登录检查通过，继续 Filter 链。", uri);
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("StudentLoginFilter 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentLoginFilter 销毁。");
    }
}
