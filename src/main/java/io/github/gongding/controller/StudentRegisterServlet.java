package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.StudentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/studentRegister")
public class StudentRegisterServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentRegisterServlet.class);
    private StudentService studentService = new StudentService();
    //创建ObjectMapper实例，用于将Java对象转换为JSON格式，或将JSON转换为Java对象
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (学生注册)。", remoteAddr, requestUrl);

        String studentNumber = request.getParameter("studentNumber");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String school = request.getParameter("school");
        String classof = request.getParameter("classof");
        String password = request.getParameter("password");

        logger.debug("接收到学生注册信息 - 学号: {}, 姓名: {}, 邮箱: {}, 学校: {}, 班级: {}", studentNumber, name, email, school, classof);

        logger.debug("调用 StudentService 进行注册，学号: {}", studentNumber);
        String registrationResult = studentService.register(studentNumber, name, email, school, classof, password);
        logger.debug("StudentService.register 返回结果: {}", registrationResult);

        //创建一个Map对象用于构建JSON响应数据
        Map<String, Object> map = new HashMap<>();
        if ("success".equals(registrationResult)) {
            map.put("success", true);
            logger.info("学号 {} 注册成功，返回成功响应。", studentNumber);
        } else {
            map.put("success", false);
            map.put("message", registrationResult);
            logger.warn("学号 {} 注册失败，原因: {}，返回失败响应。", studentNumber, registrationResult);
        }
        response.setContentType("application/json;charset=utf-8");
        try {
            mapper.writeValue(response.getWriter(), map);
            logger.debug("已向客户端返回注册结果响应。");
        } catch (IOException e) {
            logger.error("发送注册结果响应时发生 IOException。", e);
        }
        logger.info("完成处理 POST 请求: {} (学生注册)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentRegisterServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentRegisterServlet 销毁。");
        super.destroy();
    }
}
