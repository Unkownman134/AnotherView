package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/api/teacher/practice/reuse")
public class TeacherReusePracticeServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401, "Unauthorized");
            return;
        }

        //从Session中获取当前登录的教师实体，并获取其ID
        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = teacher.getId();

        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonNode rootNode = mapper.readTree(jsonBody);

            JsonNode originalPracticeIdNode = rootNode.get("originalPracticeId");
            JsonNode lessonIdNode = rootNode.get("lessonId");
            JsonNode semesterIdNode = rootNode.get("semesterId");
            JsonNode newTitleNode = rootNode.get("title");
            JsonNode startTimeNode = rootNode.get("startTime");
            JsonNode endTimeNode = rootNode.get("endTime");
            JsonNode classIdsNode = rootNode.get("classIds");
            JsonNode questionIdsNode = rootNode.get("questionIds");

            if (originalPracticeIdNode == null || lessonIdNode == null || semesterIdNode == null ||
                    newTitleNode == null || startTimeNode == null || endTimeNode == null ||
                    classIdsNode == null || questionIdsNode == null) {
                if (originalPracticeIdNode == null) System.err.println("  - originalPracticeId is null");
                if (lessonIdNode == null) System.err.println("  - lessonId is null");
                if (semesterIdNode == null) System.err.println("  - semesterId is null");
                if (newTitleNode == null) System.err.println("  - title is null");
                if (startTimeNode == null) System.err.println("  - startTime is null");
                if (endTimeNode == null) System.err.println("  - endTime is null");
                if (classIdsNode == null) System.err.println("  - classIds is null");
                if (questionIdsNode == null) System.err.println("  - questionIds is null");

                resp.sendError(400, "Missing required practice data");
                return;
            }

            //获取原练习ID
            int originalPracticeId = originalPracticeIdNode.asInt();
            //获取新练习课程ID
            int lessonId = lessonIdNode.asInt();
            int semesterId = semesterIdNode.asInt();
            String newTitle = newTitleNode.asText();

            LocalDateTime newStartTime;
            LocalDateTime newEndTime;
            try {
                newStartTime = LocalDateTime.parse(startTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                newEndTime = LocalDateTime.parse(endTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception dateParseE) {
                dateParseE.printStackTrace();
                resp.sendError(400, "Invalid date or time format");
                return;
            }

            //提取新练习关联的班级ID数组
            int[] classIds = extractIntArray(classIdsNode);
            //提取新练习包含的题目ID数组
            int[] questionIds = extractIntArray(questionIdsNode);

            //创建新的复用练习记录
            int newPracticeId = practiceDao.createPracticeFromReuse(teacherId, lessonId, semesterId, newTitle, classIds, newStartTime, newEndTime, questionIds);

            if (newPracticeId > 0) {
                resp.setContentType("application/json");
                resp.getWriter().write(mapper.writeValueAsString(new Result(true, "练习复用成功", newPracticeId)));
            } else {
                resp.sendError(500, "练习复用失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "服务器错误";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += ": " + e.getMessage();
            }
            resp.sendError(500, errorMessage);
        }
    }

    private int[] extractIntArray(JsonNode arrayNode) {
        if (arrayNode != null && arrayNode.isArray()) {
            List<Integer> intList = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                if (node != null && !node.isNull()) {
                    if (node.isInt()) {
                        intList.add(node.asInt());
                    } else if (node.isTextual()) {
                        try {
                            intList.add(Integer.parseInt(node.asText()));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {

                    }
                } else {

                }
            }
            return intList.stream().mapToInt(i -> i).toArray();
        }
        return new int[0];
    }

    private static class Result {
        public boolean success;
        public String message;
        public Integer newPracticeId;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public Result(boolean success, String message, Integer newPracticeId) {
            this.success = success;
            this.message = message;
            this.newPracticeId = newPracticeId;
        }
    }
}
