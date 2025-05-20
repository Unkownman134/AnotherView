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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/reuse")
public class TeacherReusePracticeServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherReusePracticeServlet.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public TeacherReusePracticeServlet() {
        logger.debug("TeacherReusePracticeServlet 构造方法执行。");
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override // 明确表示是重写父类方法
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师复用练习)。", remoteAddr, requestUrl);

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        //从Session中获取当前登录的教师实体，并获取其ID
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

                if (originalPracticeIdNode == null) logger.warn("JSON 数据中缺少 originalPracticeId 字段。");
                if (lessonIdNode == null) logger.warn("JSON 数据中缺少 lessonId 字段。");
                if (semesterIdNode == null) logger.warn("JSON 数据中缺少 semesterId 字段。");
                if (newTitleNode == null) logger.warn("JSON 数据中缺少 title 字段。");
                if (startTimeNode == null) logger.warn("JSON 数据中缺少 startTime 字段。");
                if (endTimeNode == null) logger.warn("JSON 数据中缺少 endTime 字段。");
                if (classIdsNode == null) logger.warn("JSON 数据中缺少 classIds 字段。");
                if (questionIdsNode == null) logger.warn("JSON 数据中缺少 questionIds 字段。");

                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required practice data");
                return;
            }

            //获取原练习ID
            if (!originalPracticeIdNode.isInt()) {
                logger.warn("originalPracticeId 格式错误: {}", originalPracticeIdNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid originalPracticeId format");
                return;
            }
            int originalPracticeId = originalPracticeIdNode.asInt();
            logger.debug("原练习 ID: {}", originalPracticeId);

            //获取新练习课程ID
            if (!lessonIdNode.isInt()) {
                logger.warn("lessonId 格式错误: {}", lessonIdNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid lessonId format");
                return;
            }
            int lessonId = lessonIdNode.asInt();
            logger.debug("新练习课程 ID: {}", lessonId);

            if (!semesterIdNode.isInt()) {
                logger.warn("semesterId 格式错误: {}", semesterIdNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid semesterId format");
                return;
            }
            int semesterId = semesterIdNode.asInt();
            logger.debug("新练习学期 ID: {}", semesterId);

            if (!newTitleNode.isTextual()) {
                logger.warn("title 格式错误: {}", newTitleNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid title format");
                return;
            }
            String newTitle = newTitleNode.asText();
            logger.debug("新练习标题: '{}'", newTitle);

            if (!classIdsNode.isArray()) {
                logger.warn("classIds 格式错误 (非数组): {}", classIdsNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid classIds format");
                return;
            }
            int[] classIds = extractIntArray(classIdsNode);
            logger.debug("新练习关联的班级 ID 数量: {}", classIds.length);

            if (!questionIdsNode.isArray()) {
                logger.warn("questionIds 格式错误 (非数组): {}", questionIdsNode.getNodeType());
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid questionIds format");
                return;
            }
            int[] questionIds = extractIntArray(questionIdsNode);
            logger.debug("新练习包含的题目 ID 数量: {}", questionIds.length);

            LocalDateTime newStartTime;
            LocalDateTime newEndTime;
            try {
                newStartTime = LocalDateTime.parse(startTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                newEndTime = LocalDateTime.parse(endTimeNode.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                logger.debug("成功解析新练习开始时间: {} 和结束时间: {}", newStartTime, newEndTime);
            } catch (DateTimeParseException e) {
                logger.warn("新的开始或结束时间格式不正确。", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date or time format");
                return;
            }

            logger.debug("调用 PracticeDao.createPracticeFromReuse 创建复用练习，教师ID: {}, 课程ID: {}, 学期ID: {}, 标题: '{}', 开始时间: {}, 结束时间: {}, 题目数量: {}",
                    teacherId, lessonId, semesterId, newTitle, newStartTime, newEndTime, questionIds.length);
            int newPracticeId = practiceDao.createPracticeFromReuse(teacherId, lessonId, semesterId, newTitle, classIds, newStartTime, newEndTime, questionIds);
            logger.debug("PracticeDao.createPracticeFromReuse 返回新练习 ID: {}", newPracticeId);

            resp.setContentType("application/json");
            Map<String, Object> responseMap = new HashMap<>();
            if (newPracticeId > 0) {
                responseMap.put("success", true);
                responseMap.put("message", "练习复用成功");
                responseMap.put("newPracticeId", newPracticeId);
                logger.info("教师 {} 成功复用练习 {} 创建新练习，ID: {}", teacherName, originalPracticeId, newPracticeId);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回复用成功响应。");
            } else {
                logger.error("教师 {} 复用练习 {} 创建新练习失败。", teacherName, originalPracticeId);
                responseMap.put("success", false);
                responseMap.put("message", "练习复用失败");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回复用失败响应。");
            }

        } catch (Exception e) {
            logger.error("教师复用练习时发生服务器错误。", e);
            String errorMessage = "服务器错误";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += ": " + e.getMessage();
            }
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
        }
        logger.info("完成处理 POST 请求: {} (教师复用练习)。", requestUrl);
    }

    /**
     * 辅助方法：从JsonNode数组中提取整数数组
     * @param arrayNode 包含整数的JsonNode数组
     * @return 整数数组
     */
    private int[] extractIntArray(JsonNode arrayNode) {
        logger.trace("尝试从 JsonNode 数组提取整数数组。");
        if (arrayNode != null && arrayNode.isArray()) {
            logger.trace("找到 JsonNode 数组，包含 {} 个元素。", arrayNode.size());
            List<Integer> intList = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                if (node != null && !node.isNull()) {
                    if (node.isInt()) {
                        intList.add(node.asInt());
                        logger.trace("添加整数类型元素: {}", node.asInt());
                    } else if (node.isTextual()) {
                        try {
                            int id = Integer.parseInt(node.asText());
                            intList.add(id);
                            logger.trace("添加文本类型元素 (解析后): {}", id);
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
            int[] result = intList.stream().mapToInt(i -> i).toArray();
            logger.trace("提取到的整数数组长度: {}", result.length);
            return result;
        }
        logger.debug("未找到 JsonNode 数组或格式不正确，返回空数组。");
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
