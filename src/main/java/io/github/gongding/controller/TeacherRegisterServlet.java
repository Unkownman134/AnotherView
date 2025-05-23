package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.service.TeacherService;
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

@WebServlet("/api/teacherRegister")
public class TeacherRegisterServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherRegisterServlet.class);
    private TeacherService teacherService = new TeacherService();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师注册)。", remoteAddr, requestUrl);

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        logger.debug("接收到教师注册信息 - 姓名: {}, 邮箱: {}", name, email);

        logger.debug("调用 TeacherService 进行注册，姓名: {}", name);
        boolean ok = teacherService.register(name, email, password);
        logger.debug("TeacherService.register 返回结果: {}", ok);

        Map<String, Object> map = new HashMap<>();
        if (ok) {
            map.put("success", true);
            logger.info("教师姓名 '{}' 注册成功，返回成功响应。", name);
        } else {
            map.put("success", false);
            map.put("message", "教师已存在");
            logger.warn("教师姓名 '{}' 注册失败，教师已存在，返回失败响应。", name);
        }
        response.setContentType("application/json;charset=utf-8");
        try {
            mapper.writeValue(response.getWriter(), map);
            logger.debug("已向客户端返回注册结果响应。");
        } catch (IOException e) {
            logger.error("发送注册结果响应时发生 IOException。", e);
        }
        logger.info("完成处理 POST 请求: {} (教师注册)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherRegisterServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherRegisterServlet 销毁。");
        super.destroy();
    }
}
