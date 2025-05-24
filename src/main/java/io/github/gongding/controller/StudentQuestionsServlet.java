package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.service.QuestionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/questions")
public class StudentQuestionsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentQuestionsServlet.class);
    private final QuestionService questionService = new QuestionService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentQuestionsServlet() {
        logger.debug("StudentQuestionsServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生练习题目)。", remoteAddr, requestUrl);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        String practiceIdStr = request.getParameter("practice_id");
        logger.debug("请求参数 - practice_id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "缺少练习 ID 参数");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceId, requestUrl);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "无效的练习 ID");
                mapper.writeValue(response.getWriter(), responseMap);
                return;
            }

            logger.debug("调用 QuestionService 获取练习 ID {} 的题目列表。", practiceId);
            List<QuestionEntity> questions = questionService.getQuestionsByPracticeId(practiceId);

            if (questions != null) {
                responseMap.put("success", true);
                responseMap.put("questions", questions);
                responseMap.put("message", "成功获取练习题目。");
                logger.debug("成功获取练习 ID {} 的 {} 道题目，返回给客户端。", practiceId, questions.size());
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                responseMap.put("success", false);
                responseMap.put("message", "获取练习题目失败。");
                logger.warn("获取练习 ID {} 的题目列表失败。", practiceId);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            mapper.writeValue(response.getWriter(), responseMap);

        } catch (NumberFormatException e) {
            logger.warn("练习ID格式不正确: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "练习 ID 格式不正确");
            mapper.writeValue(response.getWriter(), responseMap);
        } catch (Exception e) {
            logger.error("处理学生练习题目请求时发生异常，练习 ID: {}。", practiceIdStr, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "服务器内部错误: " + e.getMessage());
            mapper.writeValue(response.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {} (获取学生练习题目)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentQuestionsServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentQuestionsServlet 销毁。");
        super.destroy();
    }
}
