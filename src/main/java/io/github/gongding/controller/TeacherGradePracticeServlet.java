package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@WebServlet("/api/teacher/practice/grading")
public class TeacherGradePracticeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherGradePracticeServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper objectMapper;

    public TeacherGradePracticeServlet() {
        logger.debug("TeacherGradePracticeServlet 构造方法执行。");
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师练习批改数据)。", remoteAddr, requestUrl);

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "未登录或会话已过期");
            objectMapper.writeValue(out, errorResponse);
            return;
        }
        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = teacher.getId();
        String teacherName = teacher.getName();
        logger.debug("教师已登录，姓名: {} (ID: {})。", teacherName, teacherId);

        String practiceIdParam = request.getParameter("practiceId");
        logger.debug("请求参数 - practiceId: {}", practiceIdParam);

        if (practiceIdParam == null || practiceIdParam.isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "缺少练习ID参数");
            objectMapper.writeValue(out, errorResponse);
            return;
        }

        int practiceId;
        try {
            practiceId = Integer.parseInt(practiceIdParam);
            logger.debug("解析的练习ID: {}", practiceId);
            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceIdParam, requestUrl);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "无效的练习ID");
                objectMapper.writeValue(out, errorResponse);
                return;
            }
        } catch (NumberFormatException e) {
            logger.warn("无效的练习ID格式: {}，拒绝访问 {}。", practiceIdParam, requestUrl, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "无效的练习ID格式");
            objectMapper.writeValue(out, errorResponse);
            return;
        }

        try {
            logger.debug("调用 TeacherPracticeService 获取练习 ID {} 的批改数据。", practiceId);
            Map<String, Object> gradingData = teacherPracticeService.getPracticeGradingData(practiceId);

            if (gradingData == null) {
                logger.warn("未找到练习 ID {} 的批改数据或数据不可访问。", practiceId);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "未找到该练习的批改数据");
                objectMapper.writeValue(out, errorResponse);
            } else {
                logger.debug("成功获取练习 ID {} 的批改数据，返回给客户端。", practiceId);
                response.setStatus(HttpServletResponse.SC_OK);
                objectMapper.writeValue(out, gradingData);
            }

        } catch (Exception e) {
            logger.error("获取练习 ID {} 批改数据时发生内部错误。", practiceId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "获取批改数据时发生内部错误: " + e.getMessage());
            objectMapper.writeValue(out, errorResponse);
        }
        logger.info("完成处理 GET 请求: {} (获取教师练习批改数据)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherGradePracticeServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherGradePracticeServlet 销毁。");
        super.destroy();
    }
}
