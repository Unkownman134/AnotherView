package io.github.gongding.controller;

import io.github.gongding.dao.StudentDao;
import io.github.gongding.entity.LessonEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/student/studentLessons")
public class StudentLessonsServlet extends HttpServlet {
    private final StudentDao studentDao = new StudentDao();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String studentNumber = request.getParameter("studentNumber");
        List<LessonEntity> lessons = studentDao.getStudentLessons(studentNumber);

        response.setContentType("application/json;charset=utf-8");
        new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValue(response.getWriter(), lessons);
    }
}