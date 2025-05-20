package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/studentLogin")
public class StudentLoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentLoginServlet.class);
    private final StudentService studentService = new StudentService();
    private final ObjectMapper mapper = new ObjectMapper();
    //表示Cookie的过期时间（秒），这里设置为7天
    private final int COOKIE_EXPIRATION = 7 * 24 * 60 * 60;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("收到来自 IP 地址 {} 的学生登录 POST 请求。", request.getRemoteAddr());

        String studentNumber = request.getParameter("studentNumber");
        String password = request.getParameter("password");

        logger.debug("尝试登录的学号: {}", studentNumber);

        StudentEntity student = studentService.login(studentNumber, password);
        Map<String, Object> map = new HashMap<>();

        if (student != null) {
            logger.info("学号 {} 登录成功。", studentNumber);

            HttpSession session = request.getSession();
            session.setAttribute("student", student);
            session.setMaxInactiveInterval(COOKIE_EXPIRATION);

            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setMaxAge(COOKIE_EXPIRATION);
            sessionCookie.setPath("/Anotherview");
            sessionCookie.setHttpOnly(true);
            response.addCookie(sessionCookie);

            String autoLoginValue = studentNumber + ":" + password;
            Cookie autoLoginCookie = new Cookie("studentAutoLogin", autoLoginValue);
            autoLoginCookie.setMaxAge(COOKIE_EXPIRATION);
            autoLoginCookie.setPath("/Anotherview");
            response.addCookie(autoLoginCookie);

            map.put("success", true);
            logger.debug("为学号 {} 设置了 Session 和自动登录 Cookie。", studentNumber);

        } else {
            logger.warn("学号 {} 登录失败，学号或密码错误。", studentNumber);
            map.put("success", false);
            map.put("message", "学号或密码错误");
        }

        response.setContentType("application/json;charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
        logger.debug("已向客户端返回登录结果。");
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentLoginServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentLoginServlet 销毁。");
        super.destroy();
    }
}
