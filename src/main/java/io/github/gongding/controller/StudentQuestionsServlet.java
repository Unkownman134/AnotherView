package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.QuestionDao;
import io.github.gongding.entity.QuestionEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/student/questions")
public class StudentQuestionsServlet extends HttpServlet {
    private final QuestionDao questionDao = new QuestionDao();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int practiceId = Integer.parseInt(request.getParameter("practice_id"));
        List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);

        response.setContentType("application/json;charset=utf-8");
        new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValue(response.getWriter(), questions);
    }
}