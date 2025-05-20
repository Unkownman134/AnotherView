package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.QuestionDao;
import io.github.gongding.entity.QuestionEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/questions")
public class StudentQuestionsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentQuestionsServlet.class);
    private final QuestionDao questionDao = new QuestionDao();
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

        String practiceIdStr = request.getParameter("practice_id");
        logger.debug("请求参数 - practice_id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceId, requestUrl);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
                return;
            }

            logger.debug("调用 QuestionDao 获取练习 ID {} 的题目列表。", practiceId);
            List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);

            response.setContentType("application/json;charset=utf-8");
            logger.debug("成功获取练习 ID {} 的 {} 道题目，返回给客户端。", practiceId, (questions != null ? questions.size() : 0));
            mapper.writeValue(response.getWriter(), questions);

        } catch (NumberFormatException e) {
            logger.warn("练习ID格式不正确: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
        } catch (Exception e) {
            logger.error("处理学生练习题目请求时发生异常，练习ID: {}", practiceIdStr, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
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
