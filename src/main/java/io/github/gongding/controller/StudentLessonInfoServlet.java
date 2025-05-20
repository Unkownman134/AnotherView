package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.entity.LessonEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/lessonInfo")
public class StudentLessonInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentLessonInfoServlet.class);
    private final LessonDao lessonDao = new LessonDao();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {}", remoteAddr, requestUrl);

        String lessonIdStr = req.getParameter("lesson_id");
        logger.debug("请求参数 - lesson_id: {}", lessonIdStr);

        if (lessonIdStr == null || lessonIdStr.trim().isEmpty()) {
            logger.warn("缺少课程ID参数，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少课程ID参数");
            return;
        }

        try {
            int lessonId = Integer.parseInt(lessonIdStr);
            logger.debug("解析的课程ID: {}", lessonId);

            if (lessonId <= 0) {
                logger.warn("无效的课程ID格式 (非正数): {}，拒绝访问 {}。", lessonIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效课程ID");
                return;
            }

            logger.debug("调用 LessonDao 获取课程 ID {} 的信息。", lessonId);
            LessonEntity lesson = lessonDao.getLessonById(lessonId);

            resp.setContentType("application/json;charset=utf-8");
            if (lesson != null) {
                logger.debug("成功获取课程 ID {} 的信息，返回给客户端。", lessonId);
                mapper.writeValue(resp.getWriter(), lesson);
            } else {
                logger.warn("未找到课程 ID {} 的信息。", lessonId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该课程");
            }

        } catch (NumberFormatException e) {
            logger.warn("课程ID格式不正确: {}，拒绝访问 {}。", lessonIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效课程ID格式");
        } catch (Exception e) {
            logger.error("处理课程信息请求时发生异常，课程ID: {}", lessonIdStr, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
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
