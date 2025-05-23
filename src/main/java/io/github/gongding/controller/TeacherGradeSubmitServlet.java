package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.TeacherPracticeService;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/grade")
public class TeacherGradeSubmitServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherGradeSubmitServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师提交批改)。", remoteAddr, requestUrl);

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "未登录或会话已过期");
            objectMapper.writeValue(out, errorResponse);
            return;
        }
        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = teacher.getId();
        String teacherName = teacher.getName();
        logger.debug("教师已登录，姓名: {} (ID: {})。", teacherName, teacherId);

        try {
            logger.debug("尝试从请求体读取并解析 JSON 数据。");
            JsonNode jsonNode = objectMapper.readTree(request.getReader());
            logger.debug("成功解析请求体 JSON 数据。");

            JsonNode submissionIdNode = jsonNode.get("submissionId");
            JsonNode questionIdNode = jsonNode.get("questionId");
            JsonNode gradeNode = jsonNode.get("grade");
            JsonNode feedbackNode = jsonNode.get("feedback");

            if (submissionIdNode == null || !submissionIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 submissionId 字段。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing or invalid submissionId");
                objectMapper.writeValue(out, errorResponse);
                return;
            }
            if (questionIdNode == null || !questionIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 questionId 字段。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing or invalid questionId");
                objectMapper.writeValue(out, errorResponse);
                return;
            }
            if (gradeNode == null || !gradeNode.isNumber()) {
                logger.warn("JSON 数据中缺少或格式错误的 grade 字段。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing or invalid grade");
                objectMapper.writeValue(out, errorResponse);
                return;
            }
            if (feedbackNode == null || !feedbackNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 feedback 字段。");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Missing or invalid feedback");
                objectMapper.writeValue(out, errorResponse);
                return;
            }


            int submissionId = submissionIdNode.asInt();
            int questionId = questionIdNode.asInt();
            double grade = gradeNode.asDouble();
            String feedback = feedbackNode.asText();

            logger.debug("接收到批改数据 - 提交ID: {}, 题目ID: {}, 评分: {}, 反馈: '{}'", submissionId, questionId, grade, feedback);

            logger.debug("调用 TeacherPracticeService.saveSubmissionGrade 保存提交记录 {} 中题目 {} 的评分。", submissionId, questionId);
            boolean success = teacherPracticeService.saveSubmissionGrade(submissionId, questionId, grade, feedback);
            logger.debug("TeacherPracticeService.saveSubmissionGrade 返回结果: {}", success);

            Map<String, Object> jsonResponse = new HashMap<>();
            if (success) {
                jsonResponse.put("success", true);
                jsonResponse.put("message", "批改保存成功");
                response.setStatus(HttpServletResponse.SC_OK);
                logger.info("提交记录 {} 中题目 {} 的批改保存成功。", submissionId, questionId);
            } else {
                jsonResponse.put("success", false);
                jsonResponse.put("message", "批改保存失败");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                logger.warn("提交记录 {} 中题目 {} 的批改保存失败。", submissionId, questionId);
            }
            objectMapper.writeValue(out, jsonResponse);
            logger.debug("已向客户端返回批改结果响应。");

        } catch (Exception e) {
            logger.error("保存批改时发生内部错误。", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "保存批改时发生内部错误: " + e.getMessage());
            objectMapper.writeValue(out, errorResponse);
        } finally {
            out.close();
            logger.debug("PrintWriter 已关闭。");
        }
        logger.info("完成处理 POST 请求: {} (教师提交批改)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherGradeSubmitServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherGradeSubmitServlet 销毁。");
        super.destroy();
    }
}
