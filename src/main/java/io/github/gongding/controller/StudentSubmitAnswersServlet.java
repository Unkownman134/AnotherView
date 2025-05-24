package io.github.gongding.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentPracticeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/submit")
public class StudentSubmitAnswersServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentSubmitAnswersServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudentPracticeService studentPracticeService = new StudentPracticeService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (学生提交答案)。", remoteAddr, requestUrl);

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话已过期，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "用户未登录或会话已过期");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        int studentId = student.getId();
        String studentNumber = student.getStudentNumber();
        logger.debug("学生已登录，学号: {} (ID: {})。", studentNumber, studentId);

        Map<String, Object> requestData;
        try {
            logger.debug("尝试从请求体读取并解析 JSON 数据。");
            requestData = mapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
            logger.debug("成功解析请求体 JSON 数据。");
        } catch (IOException e) {
            logger.warn("请求数据格式错误，无法解析 JSON。", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "请求数据格式错误");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        Object practiceIdObj = requestData.get("practiceId");
        logger.debug("从 JSON 数据中获取 practiceId: {}", practiceIdObj);
        if (!(practiceIdObj instanceof Integer)) {
            logger.warn("practiceId 格式错误或缺失，拒绝处理。practiceIdObj 类型: {}", (practiceIdObj != null ? practiceIdObj.getClass().getName() : "null"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "practiceId 格式错误或缺失");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }
        int practiceId = (Integer) practiceIdObj;
        logger.debug("解析的练习ID: {}", practiceId);

        if (practiceId <= 0) {
            logger.warn("无效的练习ID (非正数): {}，拒绝处理。", practiceId);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "无效的练习 ID");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        Object answersObj = requestData.get("answers");
        logger.debug("从 JSON 数据中获取 answers 列表，数量: {}", (answersObj instanceof List ? ((List<?>) answersObj).size() : "非列表"));
        if (!(answersObj instanceof List)) {
            logger.warn("answers 格式错误或缺失，拒绝处理。answersObj 类型: {}", (answersObj != null ? answersObj.getClass().getName() : "null"));
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "answers 格式错误或缺失");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        List<Map<String, Object>> answers;
        try {
            answers = mapper.convertValue(answersObj, new TypeReference<List<Map<String, Object>>>() {});
            logger.debug("成功将 answersObj 转换为 List<Map<String, Object>>。");
        } catch (IllegalArgumentException e) {
            logger.warn("answers 内容格式不正确，无法转换为 List<Map<String, Object>>。", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "answers 内容格式不正确");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        if (answers == null || answers.isEmpty()) {
            logger.warn("答案列表为空，拒绝处理。");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "答案列表不能为空");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }
        logger.debug("答案列表包含 {} 个答案。", answers.size());

        try {
            logger.debug("调用 StudentPracticeService.submitStudentAnswers 提交答案，学生ID: {}, 练习ID: {}", studentId, practiceId);
            int submissionId = studentPracticeService.submitStudentAnswers(studentId, practiceId, answers);
            logger.debug("StudentPracticeService.submitStudentAnswers 返回 submissionId: {}", submissionId);

            if (submissionId != -1) {
                Map<String, Object> existingSubmission = studentPracticeService.getLatestStudentSubmission(studentId, practiceId);
                boolean isUpdateOperation = (existingSubmission != null && existingSubmission.containsKey("submissionId") && (Integer)existingSubmission.get("submissionId") == submissionId);

                String message = isUpdateOperation ? "修改成功" : "提交成功";
                responseMap.put("success", true);
                responseMap.put("message", message);
                responseMap.put("submissionId", submissionId);
                responseMap.put("operation", isUpdateOperation ? "updated" : "created");
                logger.info("学生 {} 对练习 {} 提交处理成功，操作: {}，新/更新提交ID: {}", studentId, practiceId, (isUpdateOperation ? "更新" : "创建"), submissionId);
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.error("学生 {} 对练习 {} 提交处理失败，未能保存提交记录。", studentId, practiceId);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "提交处理失败，未能保存提交记录");
            }
            mapper.writeValue(resp.getWriter(), responseMap);

        } catch (Exception e) {
            logger.error("提交处理时发生未知异常。学生ID: {}, 练习ID: {}", studentId, practiceId, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "服务器内部错误，提交失败: " + e.getMessage());
            mapper.writeValue(resp.getWriter(), responseMap);
        }
        logger.info("完成处理 POST 请求: {} (学生提交答案)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentSubmitAnswersServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentSubmitAnswersServlet 销毁。");
        super.destroy();
    }
}
