package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.AdminEntity;
import io.github.gongding.service.AdminService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/adminLogin")
public class AdminLoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminLoginServlet.class);
    private final AdminService adminService = new AdminService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60; // 7 days

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (管理员登录)。", remoteAddr, requestUrl);

        String name = request.getParameter("name");
        String password = request.getParameter("password");

        logger.debug("尝试登录的管理员姓名: {}", name);

        logger.debug("调用 AdminService 进行登录，姓名: {}", name);
        AdminEntity admin = adminService.login(name, password);
        logger.debug("AdminService.login 返回结果: {}", (admin != null ? "成功" : "失败"));

        Map<String, Object> map = new HashMap<>();

        if (admin != null) {
            logger.info("管理员姓名 '{}' 登录成功。", name);

            HttpSession session = request.getSession(true);
            session.setAttribute("admin", admin);
            session.setMaxInactiveInterval(COOKIE_EXPIRATION);
            logger.debug("为管理员 '{}' 设置 Session。", name);


            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setMaxAge(COOKIE_EXPIRATION);
            sessionCookie.setPath("/Anotherview");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);
            logger.debug("已发送 JSESSIONID Cookie。");

            String autoLoginValue = name + ":" + password;
            Cookie autoLoginCookie = new Cookie("adminAutoLogin", autoLoginValue);
            autoLoginCookie.setMaxAge(COOKIE_EXPIRATION);
            autoLoginCookie.setPath("/Anotherview");
            response.addCookie(autoLoginCookie);
            logger.debug("已发送 adminAutoLogin Cookie。");

            map.put("success", true);
            logger.debug("已向客户端返回登录成功响应。");
        } else {
            logger.warn("管理员姓名 '{}' 登录失败，用户名或密码错误。", name);
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
        logger.info("完成处理 POST 请求: {} (管理员登录)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("AdminLoginServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("AdminLoginServlet 销毁。");
        super.destroy();
    }
}