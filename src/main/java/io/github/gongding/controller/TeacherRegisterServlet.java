package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.TeacherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/teacherRegister")
public class TeacherRegisterServlet extends HttpServlet {
    private TeacherService teacherService = new TeacherService();
    private ObjectMapper mapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        boolean ok = teacherService.register(name, email, password);
        Map<String, Object> map = new HashMap<>();
        if (ok) {
            map.put("success", true);
        } else {
            map.put("success", false);
            map.put("message", "教师已存在");
        }
        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}
