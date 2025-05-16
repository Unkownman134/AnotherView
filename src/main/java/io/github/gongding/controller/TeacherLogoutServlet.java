package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@WebServlet("/api/teacherLogout")
public class TeacherLogoutServlet extends HttpServlet {
    private ObjectMapper mapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/Anotherview");
        response.addCookie(sessionCookie);

        Cookie autoLoginCookie = new Cookie("teacherAutoLogin", "");
        autoLoginCookie.setMaxAge(0);
        autoLoginCookie.setPath("/Anotherview");
        response.addCookie(autoLoginCookie);

        Map<String, Object> map = Collections.singletonMap("success", true);
        response.setContentType("application/json; charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}