package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentPracticeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/practiceDetails")
public class StudentPracticeDetailsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentPracticeDetailsServlet.class);
    private final StudentPracticeService studentPracticeService = new StudentPracticeService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentPracticeDetailsServlet() {
        logger.debug("StudentPracticeDetailsServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生练习详情)。", remoteAddr, requestUrl);

        resp.setContentType("application/json;charset=utf-8");
        Map<String, Object> responseMap = new HashMap<>();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话已过期，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseMap.put("success", false);
            responseMap.put("message", "未登录或会话已过期");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }
        StudentEntity student = (StudentEntity) session.getAttribute("student");
        logger.debug("学生已登录，学号: {}", student.getStudentNumber());

        String practiceIdStr = req.getParameter("practice_id");
        logger.debug("请求参数 - practice_id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "缺少练习 ID 参数");
            mapper.writeValue(resp.getWriter(), responseMap);
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID格式 (非正数): {}，拒绝访问 {}。", practiceIdStr, requestUrl);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseMap.put("success", false);
                responseMap.put("message", "无效练习 ID");
                mapper.writeValue(resp.getWriter(), responseMap);
                return;
            }

            logger.debug("调用 StudentPracticeService 获取练习 ID {} 的详情。", practiceId);
            PracticeEntity practice = studentPracticeService.getPracticeById(practiceId);

            if (practice != null) {
                logger.debug("成功获取练习 ID {} 的详情，返回给客户端。", practiceId);
                Map<String, Object> practiceDetails = new HashMap<>();
                practiceDetails.put("id", practice.getId());
                practiceDetails.put("title", practice.getTitle());
                practiceDetails.put("questionNum", practice.getQuestionNum());
                practiceDetails.put("startAt", practice.getStartAt());
                practiceDetails.put("endAt", practice.getEndAt());

                responseMap.put("success", true);
                responseMap.put("practiceDetails", practiceDetails);
                responseMap.put("message", "成功检索到练习详情。");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                logger.warn("未找到练习 ID {} 的详情。", practiceId);
                responseMap.put("success", false);
                responseMap.put("message", "未找到练习");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            mapper.writeValue(resp.getWriter(), responseMap);

        } catch (NumberFormatException e) {
            logger.warn("无效的练习 ID 格式: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseMap.put("success", false);
            responseMap.put("message", "无效练习 ID 格式");
            mapper.writeValue(resp.getWriter(), responseMap);
        } catch (Exception e) {
            logger.error("处理学生练习详情请求时发生错误，练习 ID: {}。", practiceIdStr, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseMap.put("success", false);
            responseMap.put("message", "内部服务器错误: " + e.getMessage());
            mapper.writeValue(resp.getWriter(), responseMap);
        }
        logger.info("完成处理 GET 请求: {} (获取学生练习详情)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentPracticeDetailsServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentPracticeDetailsServlet 销毁。");
        super.destroy();
    }
}
