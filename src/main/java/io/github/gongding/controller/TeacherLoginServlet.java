package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/teacherLogin")
public class TeacherLoginServlet extends HttpServlet {
    private final TeacherService teacherService = new TeacherService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String password = request.getParameter("password");

        TeacherEntity teacher = teacherService.login(name, password);
        Map<String, Object> map = new HashMap<>();

        if (teacher != null) {
            HttpSession session = request.getSession();
            session.setAttribute("teacher", teacher);
            session.setMaxInactiveInterval(COOKIE_EXPIRATION);

            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setMaxAge(COOKIE_EXPIRATION);
            sessionCookie.setPath("/Anotherview");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            String autoLoginValue = name + ":" + password;
            Cookie autoLoginCookie = new Cookie("teacherAutoLogin", autoLoginValue);
            autoLoginCookie.setMaxAge(COOKIE_EXPIRATION);
            autoLoginCookie.setPath("/Anotherview");
            response.addCookie(autoLoginCookie);

            map.put("success", true);
        } else {
            map.put("success", false);
            map.put("message", "用户名或密码错误");
        }
        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}
