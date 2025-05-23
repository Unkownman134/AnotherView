package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacherLogin")
public class TeacherLoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherLoginServlet.class);
    private final TeacherService teacherService = new TeacherService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (教师登录)。", remoteAddr, requestUrl);

        String name = request.getParameter("name");
        String password = request.getParameter("password");

        logger.debug("尝试登录的教师姓名: {}", name);

        logger.debug("调用 TeacherService 进行登录，姓名: {}", name);
        TeacherEntity teacher = teacherService.login(name, password);
        logger.debug("TeacherService.login 返回结果: {}", (teacher != null ? "成功" : "失败"));

        Map<String, Object> map = new HashMap<>();

        if (teacher != null) {
            logger.info("教师姓名 '{}' 登录成功。", name);

            HttpSession session = request.getSession();
            session.setAttribute("teacher", teacher);
            session.setMaxInactiveInterval(COOKIE_EXPIRATION);
            logger.debug("为教师 '{}' 设置 Session。", name);


            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setMaxAge(COOKIE_EXPIRATION);
            sessionCookie.setPath("/Anotherview");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);
            logger.debug("已发送 JSESSIONID Cookie。");

            String autoLoginValue = name + ":" + password;
            Cookie autoLoginCookie = new Cookie("teacherAutoLogin", autoLoginValue);
            autoLoginCookie.setMaxAge(COOKIE_EXPIRATION);
            autoLoginCookie.setPath("/Anotherview");
            response.addCookie(autoLoginCookie);
            logger.debug("已发送 teacherAutoLogin Cookie。");

            map.put("success", true);
            logger.debug("已向客户端返回登录成功响应。");
        } else {
            logger.warn("教师姓名 '{}' 登录失败，用户名或密码错误。", name);
            map.put("success", false);
            map.put("message", "用户名或密码错误");
            logger.debug("已向客户端返回登录失败响应。");
        }
        response.setContentType("application/json;charset=utf-8");
        try {
            mapper.writeValue(response.getWriter(), map);
        } catch (IOException e) {
            logger.error("发送登录结果响应时发生 IOException。", e);
        }
        logger.info("完成处理 POST 请求: {} (教师登录)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherLoginServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherLoginServlet 销毁。");
        super.destroy();
    }
}
