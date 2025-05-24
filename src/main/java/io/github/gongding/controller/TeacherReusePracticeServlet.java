package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherPracticeService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/reuse")
public class TeacherReusePracticeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherReusePracticeServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师复用练习)。", remoteAddr, requestUrl);

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授权访问");
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

            JsonNode lessonIdNode = rootNode.get("lessonId");
            JsonNode newTitleNode = rootNode.get("newTitle");
            JsonNode classIdsNode = rootNode.get("classIds");
            JsonNode newStartTimeNode = rootNode.get("newStartTime");
            JsonNode newEndTimeNode = rootNode.get("newEndTime");
            JsonNode questionIdsNode = rootNode.get("questionIds");

            if (lessonIdNode == null || !lessonIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 lessonId 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid lessonId");
                return;
            }
            if (newTitleNode == null || !newTitleNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 newTitle 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid newTitle");
                return;
            }
            if (classIdsNode == null || !classIdsNode.isArray()) {
                logger.warn("JSON 数据中缺少或格式错误的 classIds 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid classIds");
                return;
            }
            if (newStartTimeNode == null || !newStartTimeNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 newStartTime 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid newStartTime");
                return;
            }
            if (newEndTimeNode == null || !newEndTimeNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 newEndTime 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid newEndTime");
                return;
            }
            if (questionIdsNode == null || !questionIdsNode.isArray()) {
                logger.warn("JSON 数据中缺少或格式错误的 questionIds 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid questionIds");
                return;
            }

            int lessonId = lessonIdNode.asInt();
            String newTitle = newTitleNode.asText();
            List<Integer> classIds = extractClassIds(classIdsNode);
            int[] questionIds = extractQuestionIds(questionIdsNode);

            LocalDateTime newStartTime;
            LocalDateTime newEndTime;
            try {
                newStartTime = LocalDateTime.parse(newStartTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                newEndTime = LocalDateTime.parse(newEndTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logger.debug("成功解析新的开始时间: {} 和结束时间: {}", newStartTime, newEndTime);
            } catch (DateTimeParseException e) {
                logger.warn("日期或时间格式不正确。", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date or time format");
                return;
            }

            logger.debug("调用 TeacherPracticeService.reusePractice 复用练习，教师ID: {}, 课程ID: {}, 新标题: '{}', 班级ID列表: {}, 题目数量: {}",
                    teacherId, lessonId, newTitle, classIds, (questionIds != null ? questionIds.length : 0));
            int newPracticeId = teacherPracticeService.reusePractice(teacherId, lessonId, newTitle, classIds, newStartTime, newEndTime, questionIds);
            logger.debug("TeacherPracticeService.reusePractice 返回新练习 ID: {}", newPracticeId);

            resp.setContentType("application/json");
            Map<String, Object> responseMap = new HashMap<>();
            if (newPracticeId > 0) {
                responseMap.put("success", true);
                responseMap.put("message", "练习复用成功");
                responseMap.put("newPracticeId", newPracticeId);
                logger.info("教师 {} 成功复用练习，新练习 ID: {}", teacherName, newPracticeId);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回复用成功响应。");
            } else {
                logger.error("教师 {} 复用练习失败。", teacherName);
                responseMap.put("success", false);
                responseMap.put("message", "练习复用失败");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回复用失败响应。");
            }
        } catch (Exception e) {
            logger.error("教师复用练习时发生服务器错误。", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器错误: " + e.getMessage());
        }
        logger.info("完成处理 POST 请求: {} (教师复用练习)。", requestUrl);
    }

    /**
     * 从JSON节点中提取班级ID列表
     * @param arrayNode 包含班级ID的JsonNode数组
     * @return 班级ID列表
     */
    private List<Integer> extractClassIds(JsonNode arrayNode) {
        logger.trace("尝试提取班级ID列表。");
        List<Integer> classIds = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            logger.trace("找到班级ID数组，包含 {} 个元素。", arrayNode.size());
            for (JsonNode node : arrayNode) {
                if (node != null && !node.isNull()) {
                    if (node.isInt()) {
                        classIds.add(node.asInt());
                        logger.trace("添加整数类型班级ID: {}", node.asInt());
                    } else if (node.isTextual()) {
                        try {
                            int id = Integer.parseInt(node.asText());
                            classIds.add(id);
                            logger.trace("添加文本类型班级ID (解析后): {}", id);
                        } catch (NumberFormatException e) {
                            logger.warn("无效的数组元素文本格式: '{}'。", node.asText(), e);
                        }
                    } else {
                        logger.warn("数组元素节点类型不是整数或文本: {}", node.getNodeType());
                    }
                } else {
                    logger.warn("数组元素为 null 或 null 节点。");
                }
            }
            logger.trace("提取到的整数数组长度: {}", classIds.size());
        } else {
            logger.debug("未找到 JsonNode 数组或格式不正确，返回空列表。");
        }
        return classIds;
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

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherReusePracticeServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherReusePracticeServlet 销毁。");
        super.destroy();
    }
}
