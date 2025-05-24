package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacherInfo")
public class TeacherInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherInfoServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final TeacherService teacherService = new TeacherService();

    public TeacherInfoServlet() {
        logger.debug("TeacherInfoServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师信息)。", remoteAddr, requestUrl);

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        TeacherEntity sessionTeacher = (TeacherEntity) session.getAttribute("teacher");
        String teacherName = sessionTeacher.getName();
        logger.debug("教师已登录，Session 中的教师姓名: {}", teacherName);

        try {
            logger.debug("调用 TeacherService 获取教师姓名 '{}' 的最新信息。", teacherName);
            TeacherEntity freshTeacher = teacherService.getTeacherByTeacherName(teacherName);

            response.setContentType("application/json;charset=utf-8");
            if (freshTeacher != null) {
                logger.debug("成功获取教师姓名 '{}' 的最新信息，返回给客户端。", teacherName);
                mapper.writeValue(response.getWriter(), freshTeacher);
            } else {
                logger.error("数据异常：Session 中的教师姓名 '{}' 在数据库中未找到。", teacherName);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            logger.error("获取教师信息或处理响应时发生异常，教师姓名: {}", teacherName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        logger.info("完成处理 GET 请求: {} (获取教师信息)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherInfoServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherInfoServlet 销毁。");
        super.destroy();
    }
}
