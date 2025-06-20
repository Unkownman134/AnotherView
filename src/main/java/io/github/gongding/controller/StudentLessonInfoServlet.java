package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.LessonEntity;
import io.github.gongding.service.LessonService;
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

@WebServlet("/api/student/lessonInfo")
public class StudentLessonInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentLessonInfoServlet.class);
    private final LessonService lessonService = new LessonService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentLessonInfoServlet() {
        logger.debug("StudentLessonInfoServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生课程信息)。", remoteAddr, requestUrl);

        resp.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        String lessonIdStr = req.getParameter("lesson_id");
        logger.debug("请求参数 - lesson_id: {}", lessonIdStr);

        if (lessonIdStr == null || lessonIdStr.trim().isEmpty()) {
            logger.warn("缺少课程ID参数，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "缺少课程ID参数");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        try {
            int lessonId = Integer.parseInt(lessonIdStr);
            logger.debug("解析的课程ID: {}", lessonId);

            if (lessonId <= 0) {
                logger.warn("无效的课程ID格式 (非正数): {}，拒绝访问 {}。", lessonIdStr, requestUrl);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "无效课程ID");
                mapper.writeValue(resp.getWriter(), responseMap);
                return;
            }

            logger.debug("调用 LessonService 获取课程 ID {} 的信息。", lessonId);
            LessonEntity lesson = lessonService.getLessonById(lessonId);

            if (lesson != null) {
                logger.debug("成功获取课程 ID {} 的信息，返回给客户端。", lessonId);
                responseMap.put("success", true);
                responseMap.put("lesson", lesson);
                responseMap.put("message", "成功获取课程信息。");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.warn("未找到课程 ID {} 的信息。", lessonId);
                responseMap.put("success", false);
                responseMap.put("message", "未找到该课程");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            mapper.writeValue(resp.getWriter(), responseMap);

        } catch (NumberFormatException e) {
            logger.warn("课程ID格式不正确: {}，拒绝访问 {}。", lessonIdStr, requestUrl, e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "无效课程ID格式");
            mapper.writeValue(resp.getWriter(), responseMap);
        } catch (Exception e) {
            logger.error("处理课程信息请求时发生异常，课程ID: {}", lessonIdStr, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "服务器内部错误: " + e.getMessage());
            mapper.writeValue(resp.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {}", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentLessonInfoServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentLessonInfoServlet 销毁。");
        super.destroy();
    }
}
