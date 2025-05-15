package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.StudentDao;
import io.github.gongding.entity.StudentEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/studentInfo")
public class StudentInfoServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudentDao studentDao = new StudentDao();

    //构造方法
    public StudentInfoServlet() {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            //如果未登录，设置响应状态码为401未授权
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        StudentEntity sessionStudent = (StudentEntity) session.getAttribute("student");
        StudentEntity freshStudent = studentDao.getStudentByStudentNumber(sessionStudent.getStudentNumber());

        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), freshStudent);
    }
}