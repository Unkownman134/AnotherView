package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.QuestionDao;
import io.github.gongding.entity.QuestionEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/teacher/questions")
public class TeacherQuestionsServlet extends HttpServlet {
    private final QuestionDao questionDao = new QuestionDao();
    private final ObjectMapper mapper = new ObjectMapper();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int lessonId = Integer.parseInt(req.getParameter("lessonId"));

        List<QuestionEntity> questions = questionDao.getQuestionsByLessonId(lessonId);
        
        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getWriter(), questions);
    }
}