package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.entity.LessonEntity;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/teacher/lessons")
public class TeacherLessonsServlet extends HttpServlet {
    private final LessonDao lessonDao = new LessonDao();
    private final ObjectMapper mapper = new ObjectMapper();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401);
            return;
        }

        int teacherId = ((TeacherEntity) session.getAttribute("teacher")).getId();
        List<LessonEntity> lessons = lessonDao.getLessonsByTeacherId(teacherId);

        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getWriter(), lessons);
    }
}