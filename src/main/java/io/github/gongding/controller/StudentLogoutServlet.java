package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/studentLogout")
public class StudentLogoutServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentLogoutServlet.class);
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 POST 请求: {} (学生登出)。", remoteAddr, requestUrl);

        HttpSession session = request.getSession(false);
        if (session != null) {
            //如果Session存在，则使其失效，移除所有存储的属性
            String studentNumber = (session.getAttribute("student") instanceof io.github.gongding.entity.StudentEntity) ?
                    ((io.github.gongding.entity.StudentEntity) session.getAttribute("student")).getStudentNumber() : "未知学号";
            logger.info("使学生学号 {} 的 Session 失效。", studentNumber);
            session.invalidate();
        } else {
            logger.debug("请求中没有找到 Session，无需使之失效。");
        }

        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/Anotherview");
        //将Session Cookie添加到响应中，发送给客户端，指示浏览器删除该Cookie
        response.addCookie(sessionCookie);
        logger.debug("已发送删除 JSESSIONID Cookie 的指令。");

        Cookie autoLoginCookie = new Cookie("studentAutoLogin", "");
        autoLoginCookie.setMaxAge(0);
        autoLoginCookie.setPath("/Anotherview");
        //将自动登录Cookie添加到响应中，发送给客户端，指示浏览器删除该Cookie
        response.addCookie(autoLoginCookie);
        logger.debug("已发送删除 studentAutoLogin Cookie 的指令。");

        Map<String, Object> map = Collections.singletonMap("success", true);
        response.setContentType("application/json; charset=utf-8");
        try {
            mapper.writeValue(response.getWriter(), map);
            logger.debug("已向客户端返回登出成功响应。");
        } catch (IOException e) {
            logger.error("发送登出成功响应时发生 IOException。", e);
        }
        logger.info("完成处理 POST 请求: {} (学生登出)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentLogoutServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentLogoutServlet 销毁。");
        super.destroy();
    }
}
