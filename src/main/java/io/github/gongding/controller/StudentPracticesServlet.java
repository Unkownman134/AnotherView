package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/practices")
public class StudentPracticesServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentPracticesServlet.class);
    private final StudentPracticeService studentPracticeService = new StudentPracticeService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentPracticesServlet() {
        logger.debug("StudentPracticesServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生练习列表)。", remoteAddr, requestUrl);

        resp.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        int studentId = student.getId();
        String studentNumber = student.getStudentNumber();
        logger.debug("学生已登录，学号: {} (ID: {})。", studentNumber, studentId);

        String lessonIdStr = null;
        try {
            lessonIdStr = req.getParameter("lesson_id");
            logger.debug("请求参数 - lesson_id: {}", lessonIdStr);

            if (lessonIdStr == null || lessonIdStr.isEmpty()) {
                logger.warn("缺少课程ID参数，拒绝访问 {}。", requestUrl);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "缺少课程ID参数");
                mapper.writeValue(resp.getWriter(), responseMap);
                return;
            }
            int lessonId = Integer.parseInt(lessonIdStr);
            logger.debug("解析的课程ID: {}", lessonId);

            if (lessonId <= 0) {
                logger.warn("无效的课程ID (非正数): {}，拒绝访问 {}。", lessonIdStr, requestUrl);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "无效的课程ID");
                mapper.writeValue(resp.getWriter(), responseMap);
                return;
            }

            logger.debug("调用 StudentPracticeService 获取学生 ID {} 在课程 ID {} 下的练习详情。", studentId, lessonId);
            List<Map<String, Object>> practicesData = studentPracticeService.getStudentPracticesWithDetails(studentId, lessonId);

            if (practicesData != null) {
                responseMap.put("success", true);
                responseMap.put("practices", practicesData);
                responseMap.put("message", "成功获取学生练习列表。");
                logger.debug("成功获取学生 ID {} 在课程 ID {} 下的 {} 个练习，返回给客户端。", studentId, lessonId, practicesData.size());
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                responseMap.put("success", false);
                responseMap.put("message", "获取学生练习列表失败。");
                logger.warn("获取学生 ID {} 在课程 ID {} 下的练习列表失败。", studentId, lessonId);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            mapper.writeValue(resp.getWriter(), responseMap);

        } catch (NumberFormatException e) {
            logger.warn("课程ID格式不正确: {}，拒绝访问 {}。", lessonIdStr, requestUrl, e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "参数错误: 无效的课程ID");
            mapper.writeValue(resp.getWriter(), responseMap);
        } catch (Exception e) {
            logger.error("处理学生练习列表请求时发生异常，课程ID: {}", lessonIdStr, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "服务器错误: " + e.getMessage());
            mapper.writeValue(resp.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {} (获取学生练习列表)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentPracticesServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentPracticesServlet 销毁。");
        super.destroy();
    }
}
