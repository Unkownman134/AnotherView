package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@WebServlet("/api/studentLogout")
public class StudentLogoutServlet extends HttpServlet {
    private ObjectMapper mapper = new ObjectMapper();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            //如果Session存在，则使其失效，移除所有存储的属性
            session.invalidate();
        }

        Cookie sessionCookie = new Cookie("JSESSIONID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/Anotherview");
        //将Session Cookie添加到响应中，发送给客户端，指示浏览器删除该Cookie
        response.addCookie(sessionCookie);

        Cookie autoLoginCookie = new Cookie("studentAutoLogin", "");
        autoLoginCookie.setMaxAge(0);
        autoLoginCookie.setPath("/Anotherview");
        //将自动登录Cookie添加到响应中，发送给客户端，指示浏览器删除该Cookie
        response.addCookie(autoLoginCookie);

        Map<String, Object> map = Collections.singletonMap("success", true);
        response.setContentType("application/json; charset=utf-8");
        mapper.writeValue(response.getWriter(), map);
    }
}