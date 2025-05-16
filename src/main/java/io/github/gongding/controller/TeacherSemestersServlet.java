package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.SemesterDao;
import io.github.gongding.entity.SemesterEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/teacher/semesters")
public class TeacherSemestersServlet extends HttpServlet {
    private final SemesterDao semesterDao = new SemesterDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401);
            return;
        }

        List<SemesterEntity> semesters = semesterDao.getAllSemesters();
        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getWriter(), semesters);
    }
}

