package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.LessonEntity;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
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

@WebServlet("/api/student/studentLessons")
public class StudentLessonsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentLessonsServlet.class);
    private final StudentService studentService = new StudentService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentLessonsServlet() {
        logger.debug("StudentLessonsServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {}", remoteAddr, requestUrl);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }
        logger.debug("学生已登录，从 Session 获取学生信息。");

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        String studentNumber = student.getStudentNumber();
        logger.debug("Session 中的学生学号: {}", studentNumber);

        try {
            logger.debug("调用 StudentService 获取学号 {} 参与的课程列表。", studentNumber);
            List<LessonEntity> lessons = studentService.getStudentLessons(studentNumber);

            if (lessons != null) {
                responseMap.put("success", true);
                responseMap.put("lessons", lessons);
                responseMap.put("message", "成功获取学生课程列表。");
                logger.debug("成功获取学号 {} 参与的 {} 门课程，返回给客户端。", studentNumber, lessons.size());
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                responseMap.put("success", false);
                responseMap.put("message", "获取学生课程列表失败。");
                logger.warn("获取学号 {} 参与的课程列表失败。", studentNumber);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            mapper.writeValue(response.getWriter(), responseMap);

        } catch (Exception e) {
            logger.error("获取学号 {} 参与的课程列表或处理响应时发生异常。", studentNumber, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "获取学生课程列表时发生内部错误: " + e.getMessage());
            mapper.writeValue(response.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {}", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentLessonsServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentLessonsServlet 销毁。");
        super.destroy();
    }
}
