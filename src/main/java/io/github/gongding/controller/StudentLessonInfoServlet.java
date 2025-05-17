package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.entity.LessonEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/student/lessonInfo")
public class StudentLessonInfoServlet extends HttpServlet {
    private final LessonDao lessonDao = new LessonDao();
    private final ObjectMapper mapper = new ObjectMapper();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            int lessonId = Integer.parseInt(req.getParameter("lesson_id"));
            LessonEntity lesson = lessonDao.getLessonById(lessonId);

            resp.setContentType("application/json;charset=utf-8");
            mapper.writeValue(resp.getWriter(), lesson);

        } catch (NumberFormatException e) {
            resp.sendError(400, "无效课程ID");
        }
    }
}
