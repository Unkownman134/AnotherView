package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.AdminEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@WebServlet("/api/adminLogout")
public class AdminLogoutServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminLogoutServlet.class);
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (管理员登出)。", remoteAddr, requestUrl);

        HttpSession session = request.getSession(false);
        if (session != null) {
            String adminName = (session.getAttribute("admin") instanceof AdminEntity) ?
                    ((AdminEntity) session.getAttribute("admin")).getName() : "Unknown Name";
            logger.info("使管理员姓名 '{}' 的 Session 失效。", adminName);
            session.invalidate();
        } else {
            logger.debug("请求中没有找到 Session，无需使之失效。");
        }

        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/Anotherview");
        response.addCookie(sessionCookie);
        logger.debug("已发送删除 JSESSIONID Cookie 的指令。");

        Cookie autoLoginCookie = new Cookie("adminAutoLogin", "");
        autoLoginCookie.setMaxAge(0);
        autoLoginCookie.setPath("/Anotherview");
        response.addCookie(autoLoginCookie);
        logger.debug("已发送删除 adminAutoLogin Cookie 的指令。");

        Map<String, Object> map = Collections.singletonMap("success", true);
        response.setContentType("application/json; charset=utf-8");
        try {
            mapper.writeValue(response.getWriter(), map);
            logger.debug("已向客户端返回登出成功响应。");
        } catch (IOException e) {
            logger.error("发送登出成功响应时发生 IOException。", e);
        }
        logger.info("完成处理 POST 请求: {} (管理员登出)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("AdminLogoutServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherLogoutServlet 销毁。");
        super.destroy();
    }
}