package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
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

@WebServlet("/api/studentInfo")
public class StudentInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentInfoServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final StudentService studentService = new StudentService();

    //构造方法
    public StudentInfoServlet() {
        logger.debug("StudentInfoServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生信息)。", remoteAddr, requestUrl);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            //如果未登录，设置响应状态码为401未授权
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }
        logger.debug("学生已登录，从 Session 获取学生信息。");

        StudentEntity sessionStudent = (StudentEntity) session.getAttribute("student");
        String studentNumber = sessionStudent.getStudentNumber();
        logger.debug("Session 中的学生学号: {}", studentNumber);

        try {
            logger.debug("调用 StudentService 获取学号 {} 的最新信息。", studentNumber);
            StudentEntity freshStudent = studentService.getStudentByStudentNumber(studentNumber);

            if (freshStudent != null) {
                logger.debug("成功获取学号 {} 的最新信息，返回给客户端。", studentNumber);
                responseMap.put("success", true);
                responseMap.put("student", freshStudent);
                responseMap.put("message", "成功获取学生信息。");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.error("数据异常：Session 中的学生学号 {} 在数据库中未找到。", studentNumber);
                responseMap.put("success", false);
                responseMap.put("message", "数据异常：未找到学生信息");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            mapper.writeValue(response.getWriter(), responseMap);

        } catch (Exception e) {
            logger.error("获取学生信息或处理响应时发生异常，学号: {}", studentNumber, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "获取学生信息时发生内部错误: " + e.getMessage());
            mapper.writeValue(response.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {}", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentInfoServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentInfoServlet 销毁。");
        super.destroy();
    }
}
