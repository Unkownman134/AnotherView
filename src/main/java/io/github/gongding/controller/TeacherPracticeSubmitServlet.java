package io.github.gongding.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.ClassDao;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.ClassEntity;
import io.github.gongding.entity.TeacherEntity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@WebServlet("/api/teacher/practice")
public class TeacherPracticeSubmitServlet extends HttpServlet {
    private final PracticeDao practiceDao = new PracticeDao();
    private final ObjectMapper mapper = new ObjectMapper();
    private final LessonDao lessonDao = new LessonDao();
    private final ClassDao classDao = new ClassDao(); // Add ClassDao

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            resp.sendError(401, "未授权访问");
            return;
        }

        try {
            //从请求的输入流中读取所有行，并使用换行符连接成一个完整的JSON字符串
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JsonNode rootNode = mapper.readTree(jsonBody);

            //从JSON树结构中提取参数
            String title = rootNode.get("title").asText();
            int lessonId = rootNode.get("lessonId").asInt();
            List<Integer> classIds = extractClassIds(rootNode);
            int[] questionIds = extractQuestionIds(rootNode);
            LocalDateTime startTime = LocalDateTime.parse(rootNode.get("startTime").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime endTime = LocalDateTime.parse(rootNode.get("endTime").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
            int teacherId = teacher.getId();
            int semesterId = lessonDao.getLessonById(lessonId).getSemesterId();

            //获取班级名称并格式化为逗号分隔字符串
            String classofString = "";
            if (classIds != null && !classIds.isEmpty()) {
                //创建一个列表用于存储班级名称
                List<String> classNames = new ArrayList<>();
                for (Integer classId : classIds) {
                    ClassEntity cls = classDao.getClassById(classId);
                    if (cls != null) {
                        classNames.add(cls.getName());
                    }
                }
                //使用Stream将班级名称列表连接成一个逗号分隔的字符串
                classofString = classNames.stream().collect(Collectors.joining(","));
            }

            int newPracticeId = practiceDao.createPractice(teacherId, lessonId, semesterId, title, classIds.stream().mapToInt(i -> i).toArray(), classofString, startTime, endTime, questionIds);

            if (newPracticeId > 0) {
                resp.setContentType("application/json");
                resp.getWriter().write(mapper.writeValueAsString(new Result(true, "练习创建成功")));
            } else {
                resp.sendError(500, "练习创建失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "服务器错误: " + e.getMessage());
        }
    }

    //从JSON节点中提取班级ID列表
    private List<Integer> extractClassIds(JsonNode rootNode) {
        List<Integer> classIds = new ArrayList<>();
        JsonNode classIdsNode = rootNode.get("classIds");
        //检查节点是否存在且是否是数组类型
        if (classIdsNode != null && classIdsNode.isArray()) {
            for (JsonNode node : classIdsNode) {
                if (node != null && !node.isNull()) {
                    if (node.isInt()) {
                        //将其值作为整数添加到列表中
                        classIds.add(node.asInt());
                    } else if (node.isTextual()) {
                        try {
                            classIds.add(Integer.parseInt(node.asText()));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid classId format: " + node.asText());
                        }
                    }
                }
            }
        }
        return classIds;
    }

    //从JSON节点中提取题目ID数组
    private int[] extractQuestionIds(JsonNode rootNode) {
        JsonNode questionIdsNode = rootNode.get("questionIds");
        if (questionIdsNode != null && questionIdsNode.isArray()) {
            //使用Stream API遍历JSON数组节点，将每个元素转换为整数，并收集到int[]数组中
            return StreamSupport.stream(questionIdsNode.spliterator(), false).mapToInt(JsonNode::asInt).toArray();
        }
        return new int[0];
    }

    //用于构建简单的JSON响应结构
    private static class Result {
        //表示操作是否成功
        public boolean success;
        //操作结果的消息
        public String message;

        //构造方法
        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
