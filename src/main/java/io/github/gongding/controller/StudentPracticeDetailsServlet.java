package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity;
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
    private final PracticeDao practiceDao = new PracticeDao();
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

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话已过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录或会话已过期");
            return;
        }
        StudentEntity student = (StudentEntity) session.getAttribute("student");
        logger.debug("学生已登录，学号: {}", student.getStudentNumber());

        String practiceIdStr = req.getParameter("practice_id");
        logger.debug("请求参数 - practice_id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.trim().isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少练习ID参数");
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID格式 (非正数): {}，拒绝访问 {}。", practiceIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的练习ID");
                return;
            }

            logger.debug("调用 PracticeDao 获取练习 ID {} 的详情。", practiceId);
            PracticeEntity practice = practiceDao.getPracticeById(practiceId);

            if (practice != null) {
                logger.debug("成功获取练习 ID {} 的详情，返回给客户端。", practiceId);
                Map<String, Object> practiceDetails = new HashMap<>();
                practiceDetails.put("id", practice.getId());
                practiceDetails.put("title", practice.getTitle());
                practiceDetails.put("questionNum", practice.getQuestionNum());
                practiceDetails.put("startAt", practice.getStartAt());
                practiceDetails.put("endAt", practice.getEndAt());

                mapper.writeValue(resp.getWriter(), practiceDetails);
            } else {
                logger.warn("未找到练习 ID {} 的详情。", practiceId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "未找到该练习");
            }

        } catch (NumberFormatException e) {
            logger.warn("练习ID格式不正确: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "练习ID格式不正确");
        } catch (Exception e) {
            logger.error("处理学生练习详情请求时发生异常，练习ID: {}", practiceIdStr, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
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
