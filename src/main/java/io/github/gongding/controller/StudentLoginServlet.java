package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/studentLogin")
public class StudentLoginServlet extends HttpServlet {
    private final StudentService studentService = new StudentService();
    private final ObjectMapper mapper = new ObjectMapper();
    //表示Cookie的过期时间（秒），这里设置为7天
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String studentNumber = request.getParameter("studentNumber");
        String password = request.getParameter("password");

        StudentEntity student = studentService.login(studentNumber, password);
        Map<String, Object> map = new HashMap<>();

        if (student != null) {
            HttpSession session = request.getSession();
            session.setAttribute("student", student);
            session.setMaxInactiveInterval(COOKIE_EXPIRATION);

            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setMaxAge(COOKIE_EXPIRATION);
            sessionCookie.setPath("/Anotherview");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            String autoLoginValue = studentNumber + ":" + password;
            Cookie autoLoginCookie = new Cookie("studentAutoLogin", autoLoginValue);
            autoLoginCookie.setMaxAge(COOKIE_EXPIRATION);
            autoLoginCookie.setPath("/Anotherview");
            response.addCookie(autoLoginCookie);

            map.put("success", true);
        } else {
            map.put("success", false);
            map.put("message", "学号或密码错误");
        }
        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}