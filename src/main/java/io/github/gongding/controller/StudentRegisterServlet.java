package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.StudentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/studentRegister")
public class StudentRegisterServlet extends HttpServlet {
    private StudentService studentService = new StudentService();
    //创建ObjectMapper实例，用于将Java对象转换为JSON格式，或将JSON转换为Java对象
    private ObjectMapper mapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String studentNumber = request.getParameter("studentNumber");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String school = request.getParameter("school");
        String classof = request.getParameter("classof");
        String password = request.getParameter("password");

        boolean ok = studentService.register(studentNumber, name, email, school, classof, password);
        //创建一个Map对象用于构建JSON响应数据
        Map<String, Object> map = new HashMap<>();
        if (ok) {
            map.put("success", true);
        } else {
            map.put("success", false);
            map.put("message", "学生已存在");
        }
        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}
