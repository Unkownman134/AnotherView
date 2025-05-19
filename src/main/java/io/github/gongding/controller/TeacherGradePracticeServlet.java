package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@WebServlet("/api/teacher/practice/grading")
public class TeacherGradePracticeServlet extends HttpServlet {
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper objectMapper;

    public TeacherGradePracticeServlet() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String practiceIdParam = request.getParameter("practiceId");

        if (practiceIdParam == null || practiceIdParam.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "缺少练习ID参数");
            objectMapper.writeValue(out, errorResponse);
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdParam);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "无效的练习ID格式");
            objectMapper.writeValue(out, errorResponse);
            return;
        }

        try {
            Map<String, Object> gradingData = teacherPracticeService.getPracticeGradingData(practiceId);

            if (gradingData == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "未找到该练习的批改数据");
                objectMapper.writeValue(out, errorResponse);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                objectMapper.writeValue(out, gradingData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "获取批改数据时发生内部错误: " + e.getMessage());
            objectMapper.writeValue(out, errorResponse);
        }
    }
}
