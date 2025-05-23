package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/extendTime")
public class TeacherExtendTimeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherExtendTimeServlet.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 PUT 请求: {} (教师延长练习截止时间)。", remoteAddr, requestUrl);

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = teacher.getId();
        String teacherName = teacher.getName();
        logger.debug("教师已登录，姓名: {} (ID: {})。", teacherName, teacherId);

        try {
            //从请求的输入流中读取所有行，并使用系统换行符连接成一个完整的JSON字符串
            logger.debug("尝试从请求体读取 JSON 数据。");
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            logger.debug("请求体 JSON 字符串: {}", jsonBody);
            JsonNode rootNode = mapper.readTree(jsonBody);
            logger.debug("成功解析请求体 JSON 数据。");

            //从JSON树结构中获取"practiceId"字段的值，并将其作为整数
            JsonNode practiceIdNode = rootNode.get("practiceId");
            if (practiceIdNode == null || !practiceIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 practiceId 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid practiceId");
                return;
            }
            int practiceId = practiceIdNode.asInt();
            logger.debug("从 JSON 数据中获取 practiceId: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝处理。", practiceId);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid practice ID");
                return;
            }

            JsonNode newEndTimeNode = rootNode.get("newEndTime");
            if (newEndTimeNode == null || !newEndTimeNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 newEndTime 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid newEndTime");
                return;
            }
            String newEndTimeStr = newEndTimeNode.asText();
            logger.debug("从 JSON 数据中获取 newEndTime 字符串: {}", newEndTimeStr);

            LocalDateTime newEndTime;
            try {
                newEndTime = LocalDateTime.parse(newEndTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logger.debug("成功解析新的截止时间: {}", newEndTime);
            } catch (DateTimeParseException e) {
                logger.warn("新的截止时间格式不正确: {}", newEndTimeStr, e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid newEndTime format");
                return;
            }

            logger.debug("调用 PracticeDao.extendPracticeTime 延长练习 {} 的截止时间到 {}", practiceId, newEndTime);
            practiceDao.extendPracticeTime(practiceId, newEndTime);
            logger.info("成功延长练习 ID {} 的截止时间。", practiceId);

            resp.setContentType("application/json");
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "截止时间延长成功");
            resp.getWriter().write(mapper.writeValueAsString(successResponse));
            logger.debug("已向客户端返回成功响应。");

        } catch (Exception e) {
            logger.error("延长练习截止时间时发生异常。", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器错误: " + e.getMessage());
        }
        logger.info("完成处理 PUT 请求: {} (教师延长练习截止时间)。", requestUrl);
    }

    private static class Result {
        public boolean success;
        public String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherExtendTimeServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherExtendTimeServlet 销毁。");
        super.destroy();
    }
}
