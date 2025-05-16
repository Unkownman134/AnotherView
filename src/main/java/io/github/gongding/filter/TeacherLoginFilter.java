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

@WebFilter("/*")
public class TeacherLoginFilter implements Filter {
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    private boolean tryAutoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Cookie[] cookies = req.getCookies();
        String name = null, password = null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("teacherAutoLogin".equals(c.getName())) {
                    String[] p = c.getValue().split(":");
                    if (p.length == 2) {
                        name = p[0];
                        password = p[1];
                    }
                    break;
                }
            }
        }
        if (name != null && password != null) {
            TeacherEntity t = new TeacherService().login(name, password);
            if (t != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("teacher", t);
                session.setMaxInactiveInterval(COOKIE_EXPIRATION);
                Cookie auto = new Cookie("teacherAutoLogin", name + ":" + password);
                auto.setMaxAge(COOKIE_EXPIRATION);
                auto.setPath("/Anotherview");
                resp.addCookie(auto);
                return true;
            }
        }
        return false;
    }

    public void doFilter(ServletRequest req0, ServletResponse resp0, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) req0;
        HttpServletResponse resp = (HttpServletResponse) resp0;
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (uri.equals(contextPath + "/api/teacherLogin") || uri.equals(contextPath + "/api/teacherRegister")) {
            chain.doFilter(req, resp);
            return;
        }
        if (uri.startsWith(contextPath + "/api/student")) {
            chain.doFilter(req, resp);
            return;
        }

        boolean isTeacherPage = uri.startsWith(contextPath + "/html/teacher/");
        boolean isTeacherApi = uri.startsWith(contextPath + "/api/teacher")||uri.startsWith(contextPath + "/api/teacher/");
        boolean isLoginPage = uri.equals(contextPath + "/html/teacher/teacher-login.html");
        boolean isRegisterPage = uri.equals(contextPath + "/html/teacher/teacher-register.html");

        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("teacher") != null);

        if (isTeacherPage || isTeacherApi) {
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            if (isLoginPage || isRegisterPage) {
                if (isLoggedIn || tryAutoLogin(req, resp)) {
                    resp.sendRedirect(contextPath + "/html/teacher/teacher.html");
                    return;
                }
            } else {
                if (!isLoggedIn && !tryAutoLogin(req, resp)) {
                    resp.sendRedirect(contextPath + "/html/teacher/teacher-login.html");
                    return;
                }
            }
        }
        chain.doFilter(req, resp);
    }
}
