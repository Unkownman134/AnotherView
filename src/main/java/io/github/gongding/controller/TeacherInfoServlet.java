package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.TeacherDao;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/teacherInfo")
public class TeacherInfoServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TeacherDao teacherDao = new TeacherDao();

    public TeacherInfoServlet() {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        TeacherEntity sessionTeacher = (TeacherEntity) session.getAttribute("teacher");
        TeacherEntity freshTeacher = teacherDao.getTeacherByTeacherName(sessionTeacher.getName());

        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), freshTeacher);
    }
}