package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// 移除对 PracticeDao 的直接引用
// import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.PracticeEntity; // 导入 PracticeEntity
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherPracticeService; // 导入 TeacherPracticeService
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
import java.util.stream.StreamSupport;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/update")
public class TeacherPracticeUpdateServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticeUpdateServlet.class);
    // 将直接使用 PracticeDao 改为使用 TeacherPracticeService
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 PUT 请求: {} (教师更新练习)。", remoteAddr, requestUrl);

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
            logger.debug("尝试从请求体读取并解析 JSON 数据。");
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            logger.debug("请求体 JSON 字符串: {}", jsonBody);
            JsonNode rootNode = mapper.readTree(jsonBody);
            logger.debug("成功解析请求体 JSON 数据。");

            JsonNode practiceIdNode = rootNode.get("id");
            JsonNode titleNode = rootNode.get("title");
            JsonNode classofNode = rootNode.get("classof");
            JsonNode startTimeNode = rootNode.get("startTime");
            JsonNode endTimeNode = rootNode.get("endTime");
            JsonNode questionIdsNode = rootNode.get("questionIds");

            if (practiceIdNode == null || !practiceIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 id 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid practice ID");
                return;
            }
            if (titleNode == null || !titleNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 title 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid title");
                return;
            }
            if (classofNode == null || !classofNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 classof 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid classof");
                return;
            }
            if (startTimeNode == null || !startTimeNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 startTime 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid startTime");
                return;
            }
            if (endTimeNode == null || !endTimeNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 endTime 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid endTime");
                return;
            }
            if (questionIdsNode == null || !questionIdsNode.isArray()) {
                logger.warn("JSON 数据中缺少或格式错误的 questionIds 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid questionIds");
                return;
            }

            int practiceId = practiceIdNode.asInt();
            String title = titleNode.asText();
            String classof = classofNode.asText();
            int[] questionIds = extractQuestionIds(questionIdsNode);

            LocalDateTime startTime;
            LocalDateTime endTime;
            try {
                startTime = LocalDateTime.parse(startTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                endTime = LocalDateTime.parse(endTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logger.debug("成功解析开始时间: {} 和结束时间: {}", startTime, endTime);
            } catch (DateTimeParseException e) {
                logger.warn("日期或时间格式不正确。", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date or time format");
                return;
            }

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝处理。", practiceId);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid practice ID");
                return;
            }

            // 构建 PracticeEntity 对象
            PracticeEntity practiceToUpdate = new PracticeEntity();
            practiceToUpdate.setId(practiceId);
            practiceToUpdate.setTitle(title);
            practiceToUpdate.setClassof(classof);
            practiceToUpdate.setStartAt(startTime);
            practiceToUpdate.setEndAt(endTime);
            // 注意：lessonId, semesterId, teacherId, questionNum, status, createdAt 等字段
            // 如果在更新时不需要修改，则不需要从请求中获取并设置。
            // 如果需要修改，则需要从请求中获取。这里只处理了当前Servlet中已有的字段。

            // 调用 Service 层更新练习
            logger.debug("调用 TeacherPracticeService.updatePracticeAndQuestions 更新练习 ID {}。", practiceId);
            boolean success = teacherPracticeService.updatePracticeAndQuestions(practiceToUpdate, questionIds);
            logger.debug("TeacherPracticeService.updatePracticeAndQuestions 返回结果: {}", success);

            resp.setContentType("application/json");
            Map<String, Object> responseMap = new HashMap<>();
            if (success) {
                responseMap.put("success", true);
                responseMap.put("message", "练习修改成功");
                logger.info("教师 {} 成功修改练习 ID {}。", teacherName, practiceId);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回修改成功响应。");
            } else {
                logger.warn("教师 {} 修改练习 ID {} 失败。", teacherName, practiceId);
                responseMap.put("success", false);
                responseMap.put("message", "练习修改失败");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回修改失败响应。");
            }

        } catch (Exception e) {
            logger.error("教师更新练习时发生服务器错误。", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器错误: " + e.getMessage());
        }
        logger.info("完成处理 PUT 请求: {} (教师更新练习)。", requestUrl);
    }

    /**
     * 从JSON节点中提取题目ID数组
     * @param questionIdsNode 包含题目ID的JsonNode数组
     * @return 题目ID数组
     */
    private int[] extractQuestionIds(JsonNode questionIdsNode) {
        logger.trace("尝试提取题目ID数组。");
        if (questionIdsNode != null && questionIdsNode.isArray()) {
            logger.trace("找到题目ID数组，包含 {} 个元素。", questionIdsNode.size());
            int[] questionIds = StreamSupport.stream(questionIdsNode.spliterator(), false)
                    .mapToInt(JsonNode::asInt)
                    .toArray();
            logger.trace("提取到的题目ID数组长度: {}", questionIds.length);
            return questionIds;
        }
        logger.debug("未找到题目ID数组或格式不正确，返回空数组。");
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

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherPracticeUpdateServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherPracticeUpdateServlet 销毁。");
        super.destroy();
    }
}
