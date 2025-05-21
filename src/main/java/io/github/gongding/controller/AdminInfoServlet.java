package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.AdminEntity;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.service.QuestionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/adminInfo")
public class AdminInfoServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminInfoServlet.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final QuestionService questionService = new QuestionService();

    public AdminInfoServlet() {
        logger.debug("AdminInfoServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestUrl = request.getRequestURL().toString();
        String remoteAddr = request.getRemoteAddr();
        String action = request.getParameter("action");
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (Action: {}).", remoteAddr, requestUrl, action);

        response.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("admin") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(response.getWriter(), responseMap);
            return;
        }
        logger.debug("管理员已登录，从 Session 获取管理员信息。");

        if ("getQuestions".equals(action)) {
            try {
                List<QuestionEntity> questions = questionService.getAllQuestions();
                responseMap.put("success", true);
                responseMap.put("questions", questions);
                responseMap.put("message", "题目数据加载成功。");
                logger.debug("成功获取 {} 个题目。", questions.size());
            } catch (Exception e) {
                logger.error("获取题目数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取题目数据时发生内部错误。");
            }
        } else if ("getOnlineUsersSummary".equals(action)) {
            try {
                responseMap.put("success", true);
                responseMap.put("message", "在线用户汇总数据加载成功。");
            } catch (Exception e) {
                logger.error("获取在线用户汇总数据时发生异常。", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取在线用户汇总数据时发生内部错误。");
            }
        } else {
            AdminEntity sessionAdmin = (AdminEntity) session.getAttribute("admin");
            String adminName = sessionAdmin.getName();
            logger.debug("Session 中的管理员姓名: {}", adminName);

            try {
                responseMap.put("success", true);
                responseMap.put("admin", sessionAdmin);
                responseMap.put("message", "管理员信息加载成功。");

                logger.debug("成功获取学号 {} 的最新信息，返回给客户端。", adminName);
            } catch (Exception e) {
                logger.error("获取管理员信息或处理响应时发生异常，姓名: {}", adminName, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseMap.put("success", false);
                responseMap.put("message", "获取管理员信息时发生内部错误。");
            }
        }

        mapper.writeValue(response.getWriter(), responseMap);
        logger.info("完成处理 GET 请求: {}", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("AdminInfoServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("AdminInfoServlet 销毁。");
        super.destroy();
    }
}
