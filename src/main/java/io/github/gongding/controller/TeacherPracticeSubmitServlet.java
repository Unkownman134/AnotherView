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

@WebServlet("/api/teacher/practice")
public class TeacherPracticeSubmitServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticeSubmitServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师创建练习)。", remoteAddr, requestUrl);

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

            JsonNode titleNode = rootNode.get("title");
            JsonNode lessonIdNode = rootNode.get("lessonId");
            JsonNode classIdsNode = rootNode.get("classIds");
            JsonNode questionIdsNode = rootNode.get("questionIds");
            JsonNode startTimeNode = rootNode.get("startTime");
            JsonNode endTimeNode = rootNode.get("endTime");

            if (titleNode == null || !titleNode.isTextual()) {
                logger.warn("JSON 数据中缺少或格式错误的 title 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid title");
                return;
            }
            if (lessonIdNode == null || !lessonIdNode.isInt()) {
                logger.warn("JSON 数据中缺少或格式错误的 lessonId 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid lessonId");
                return;
            }
            if (classIdsNode == null || !classIdsNode.isArray()) {
                logger.warn("JSON 数据中缺少或格式错误的 classIds 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid classIds");
                return;
            }
            if (questionIdsNode == null || !questionIdsNode.isArray()) {
                logger.warn("JSON 数据中缺少或格式错误的 questionIds 字段。");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid questionIds");
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

            String title = titleNode.asText();
            int lessonId = lessonIdNode.asInt();
            List<Integer> classIds = extractClassIds(classIdsNode);
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

            logger.debug("调用 TeacherPracticeService.createPracticeWithDetails 创建练习，教师ID: {}, 课程ID: {}, 标题: '{}', 班级ID列表: {}, 题目数量: {}",
                    teacherId, lessonId, title, classIds, (questionIds != null ? questionIds.length : 0));
            int newPracticeId = teacherPracticeService.createPracticeWithDetails(teacherId, title, lessonId, classIds, questionIds, startTime, endTime);
            logger.debug("TeacherPracticeService.createPracticeWithDetails 返回新练习 ID: {}", newPracticeId);

            resp.setContentType("application/json");
            Map<String, Object> responseMap = new HashMap<>();
            if (newPracticeId > 0) {
                responseMap.put("success", true);
                responseMap.put("message", "练习创建成功");
                responseMap.put("practiceId", newPracticeId);
                logger.info("教师 {} 成功创建新练习，ID: {}", teacherName, newPracticeId);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回创建成功响应。");
            } else {
                logger.error("教师 {} 创建练习失败。", teacherName);
                responseMap.put("success", false);
                responseMap.put("message", "练习创建失败");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(mapper.writeValueAsString(responseMap));
                logger.debug("已向客户端返回创建失败响应。");
            }
        } catch (Exception e) {
            logger.error("教师创建练习时发生服务器错误。", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器错误: " + e.getMessage());
        }
        logger.info("完成处理 POST 请求: {} (教师创建练习)。", requestUrl);
    }

    /**
     * 从JSON节点中提取班级ID列表
     * @param arrayNode 包含班级ID的JsonNode数组
     * @return 班级ID列表
     */
    private List<Integer> extractClassIds(JsonNode arrayNode) {
        logger.trace("尝试提取班级ID列表。");
        List<Integer> classIds = new ArrayList<>();
        //检查节点是否存在且是否是数组类型
        if (arrayNode != null && arrayNode.isArray()) {
            logger.trace("找到班级ID数组，包含 {} 个元素。", arrayNode.size());
            for (JsonNode node : arrayNode) {
                if (node != null && !node.isNull()) {
                    if (node.isInt()) {
                        //将其值作为整数添加到列表中
                        classIds.add(node.asInt());
                        logger.trace("添加整数类型班级ID: {}", node.asInt());
                    } else if (node.isTextual()) {
                        try {
                            int id = Integer.parseInt(node.asText());
                            classIds.add(id);
                            logger.trace("添加文本类型班级ID (解析后): {}", id);
                        } catch (NumberFormatException e) {
                            logger.warn("无效的班级ID文本格式: '{}'。", node.asText(), e);
                        }
                    } else {
                        logger.warn("班级ID节点类型不是整数或文本: {}", node.getNodeType());
                    }
                } else {
                    logger.warn("班级ID节点为 null 或 null 节点。");
                }
            }
        } else {
            logger.debug("未找到班级ID数组或格式不正确。");
        }
        logger.trace("提取到的班级ID列表: {}", classIds);
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
            //使用Stream API遍历JSON数组节点，将每个元素转换为整数，并收集到int[]数组中
            int[] questionIds = StreamSupport.stream(questionIdsNode.spliterator(), false)
                    .mapToInt(JsonNode::asInt)
                    .toArray();
            logger.trace("提取到的题目ID数组长度: {}", questionIds.length);
            return questionIds;
        }
        logger.debug("未找到题目ID数组或格式不正确，返回空数组。");
        return new int[0];
    }

//    private static class Result {
//        //表示操作是否成功
//        public boolean success;
//        //操作结果的消息
//        public String message;
//
//        //构造方法
//        public Result(boolean success, String message) {
//            this.success = success;
//            this.message = message;
//        }
//    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherPracticeSubmitServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherPracticeSubmitServlet 销毁。");
        super.destroy();
    }
}
