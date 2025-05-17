package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@WebServlet("/api/teacher/practice/update")
public class TeacherPracticeUpdateServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401, "Unauthorized");
            return;
        }

        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonNode rootNode = mapper.readTree(jsonBody);

            int practiceId = rootNode.get("id").asInt();
            String title = rootNode.get("title").asText();
            String classof = rootNode.get("classof").asText();
            LocalDateTime startTime = LocalDateTime.parse(rootNode.get("startTime").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endTime = LocalDateTime.parse(rootNode.get("endTime").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            int[] questionIds = extractQuestionIds(rootNode);

            boolean success = practiceDao.updatePracticeAndQuestions(practiceId, title, classof, startTime, endTime, questionIds);

            resp.setContentType("application/json");
            if (success) {
                resp.getWriter().write(mapper.writeValueAsString(new Result(true, "练习修改成功")));
            } else {
                resp.sendError(500, "练习修改失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "服务器错误: " + e.getMessage());
        }
    }

    private int[] extractQuestionIds(JsonNode rootNode) {
        JsonNode questionIdsNode = rootNode.get("questionIds");
        if (questionIdsNode != null && questionIdsNode.isArray()) {
            return StreamSupport.stream(questionIdsNode.spliterator(), false)
                    .mapToInt(JsonNode::asInt)
                    .toArray();
        }
        return new int[0];
    }

    private static class Result {
        public boolean success;
        public String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
