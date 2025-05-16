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

@WebFilter("/*")
public class StudentLoginFilter implements Filter {
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    /**
     * 尝试使用自动登录Cookie进行登录
     * @param req HTTP请求对象
     * @param resp HTTP响应对象
     * @return 如果自动登录成功返回true，否则返回false
     * @throws IOException 输入输出异常
     */
    private boolean tryAutoLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //获取请求中所有的 Cookie
        Cookie[] cookies = req.getCookies();
        String studentNumber = null, password = null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                //查找名为"studentAutoLogin"的Cookie
                if ("studentAutoLogin".equals(c.getName())) {
                    String[] p = c.getValue().split(":");
                    if (p.length == 2) {
                        studentNumber = p[0];
                        password = p[1];
                    }
                    break;
                }
            }
        }
        if (studentNumber != null && password != null) {
            StudentEntity s = new StudentService().login(studentNumber, password);
            //验证通过
            if (s != null) {
                HttpSession session = req.getSession(true);
                //将登录成功的学生信息存储到Session中
                session.setAttribute("student", s);
                session.setMaxInactiveInterval(COOKIE_EXPIRATION);
                //更新Cookie的过期时间或路径
                Cookie auto = new Cookie("studentAutoLogin", studentNumber + ":" + password);
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

        //排除学生登录和注册相关的API请求，直接放行
        if (uri.equals(contextPath + "/api/studentLogin") || uri.equals(contextPath + "/api/studentRegister")) {
            chain.doFilter(req, resp);
            return;
        }
        //排除教师相关的API请求，直接放行
        if (uri.startsWith(contextPath + "/api/teacher") || uri.startsWith(contextPath + "/api/teacher/")) {
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

        //如果是学生相关的页面或API请求
        if (isStudentPage || isStudentApi) {
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            //如果当前请求是学生登录页面或注册页面
            if (isLoginPage || isRegisterPage) {
                //如果用户已经登录或者尝试自动登录成功
                if (isLoggedIn || tryAutoLogin(req, resp)) {
                    resp.sendRedirect(contextPath + "/html/student/student.html");
                    return;
                }
            } else {
                //如果用户未登录并且尝试自动登录也失败了
                if (!isLoggedIn && !tryAutoLogin(req, resp)) {
                    resp.sendRedirect(contextPath + "/html/student/student-login.html");
                    return;
                }
            }
        }
        chain.doFilter(req, resp);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}