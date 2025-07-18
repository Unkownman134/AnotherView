package io.github.gongding.controller;

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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/previous-answers")
public class StudentPreviousAnswersServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentPreviousAnswersServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudentPracticeService studentPracticeService = new StudentPracticeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生之前提交的答案)。", remoteAddr, requestUrl);

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
        if (student == null || student.getId() <= 0) {
            logger.error("Session 中的学生信息无效，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "学生信息无效，请重新登录");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }
        int studentId = student.getId();
        String studentNumber = student.getStudentNumber();
        logger.debug("学生已登录，学号: {} (ID: {})。", studentNumber, studentId);

        String practiceIdStr = req.getParameter("practice_id");
        logger.debug("请求参数 - practice_id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "缺少练习 ID 参数");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);
            if (practiceId <= 0) {
                logger.warn("无效的练习 ID (非正数): {}，拒绝访问 {}。", practiceId, requestUrl);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "无效的练习 ID");
                mapper.writeValue(resp.getWriter(), responseMap);
                return;
            }
        } catch (NumberFormatException e) {
            logger.warn("练习ID格式不正确: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "练习 ID 格式不正确");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        try {
            logger.debug("调用 StudentPracticeService 获取学生 {} 对练习 {} 的最近一次提交记录。", studentId, practiceId);
            Map<String, Object> submission = studentPracticeService.getLatestStudentSubmission(studentId, practiceId);

            if (submission == null || submission.isEmpty()) {
                logger.debug("未找到学生 {} 对练习 {} 的提交记录，返回空 JSON。", studentId, practiceId);
                responseMap.put("success", true);
                responseMap.put("message", "未找到提交记录");
                responseMap.put("submission", new HashMap<>());
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.debug("成功获取学生 {} 对练习 {} 的提交记录，返回给客户端。", studentId, practiceId);
                responseMap.put("success", true);
                responseMap.put("message", "成功获取提交记录");
                responseMap.put("submission", submission);
                resp.setStatus(HttpServletResponse.SC_OK);
            }
            mapper.writeValue(resp.getWriter(), responseMap);

        } catch (Exception e) {
            logger.error("获取学生 {} 对练习 {} 之前提交的答案时发生异常。", studentId, practiceId, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "服务器内部错误：" + e.getMessage());
            mapper.writeValue(resp.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {} (获取学生之前提交的答案)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentPreviousAnswersServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentPreviousAnswersServlet 销毁。");
        super.destroy();
    }
}
