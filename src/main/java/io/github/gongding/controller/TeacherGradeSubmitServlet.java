package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.TeacherPracticeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/teacher/practice/grade")
public class TeacherGradeSubmitServlet extends HttpServlet {
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            JsonNode jsonNode = objectMapper.readTree(request.getReader());

            int submissionId = jsonNode.get("submissionId").asInt();
            int questionId = jsonNode.get("questionId").asInt();
            double grade = jsonNode.get("grade").asDouble();
            String feedback = jsonNode.get("feedback").asText();

            boolean success = teacherPracticeService.saveSubmissionGrade(submissionId, questionId, grade, feedback);

            Map<String, Object> jsonResponse = new HashMap<>();
            if (success) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "批改保存成功");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "批改保存失败");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            objectMapper.writeValue(out, jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "保存批改时发生内部错误: " + e.getMessage());
            objectMapper.writeValue(out, errorResponse);
        } finally {
            out.close();
        }
    }
}
