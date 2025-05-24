package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.QuestionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/questions")
public class TeacherQuestionsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherQuestionsServlet.class);
    private final QuestionService questionService = new QuestionService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师题目列表)。", remoteAddr, requestUrl);

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        TeacherEntity teacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = teacher.getId();
        String teacherName = teacher.getName();
        logger.debug("教师已登录，姓名: {} (ID: {})。", teacherName, teacherId);

        String lessonIdStr = req.getParameter("lessonId");
        logger.debug("请求参数 - lessonId: {}", lessonIdStr);

        if (lessonIdStr == null || lessonIdStr.trim().isEmpty()) {
            logger.warn("缺少课程ID参数，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing lesson ID");
            return;
        }

        int lessonId;
        try {
            lessonId = Integer.parseInt(lessonIdStr);
            logger.debug("解析的课程ID: {}", lessonId);
            if (lessonId <= 0) {
                logger.warn("无效的课程ID (非正数): {}，拒绝访问 {}。", lessonIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid lesson ID");
                return;
            }
        } catch (NumberFormatException e) {
            logger.warn("无效的课程ID格式: {}，拒绝访问 {}。", lessonIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid lesson ID format");
            return;
        }

        try {
            logger.debug("调用 QuestionService 获取课程 ID {} 的题目列表。", lessonId);
            List<QuestionEntity> questions = questionService.getQuestionsByLessonId(lessonId);
            logger.debug("成功获取课程 ID {} 的 {} 道题目。", lessonId, (questions != null ? questions.size() : 0));

            resp.setContentType("application/json;charset=utf-8");
            logger.debug("成功组织题目列表数据，返回给客户端。");
            mapper.writeValue(resp.getWriter(), questions);

        } catch (Exception e) {
            logger.error("获取课程 ID {} 题目列表时发生异常。", lessonId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching questions: " + e.getMessage());
        }
        logger.info("完成处理 GET 请求: {} (获取教师题目列表)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherQuestionsServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherQuestionsServlet 销毁。");
        super.destroy();
    }
}
