package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@WebServlet("/api/teacher/practice/extendTime")
public class TeacherExtendTimeServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401, "Unauthorized");
            return;
        }

        try {
            //从请求的输入流中读取所有行，并使用系统换行符连接成一个完整的JSON字符串
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonNode rootNode = mapper.readTree(jsonBody);

            //从JSON树结构中获取"practiceId"字段的值，并将其作为整数
            int practiceId = rootNode.get("practiceId").asInt();
            String newEndTimeStr = rootNode.get("newEndTime").asText();
            LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            practiceDao.extendPracticeTime(practiceId, newEndTime);

            resp.setContentType("application/json");
            resp.getWriter().write(mapper.writeValueAsString(new Result(true, "截止时间延长成功")));

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "服务器错误: " + e.getMessage());
        }
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
