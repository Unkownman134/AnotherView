package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.TeacherPracticeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practice/details")
public class TeacherPracticeDetailsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticeDetailsServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public TeacherPracticeDetailsServlet() {
        logger.debug("TeacherPracticeDetailsServlet 构造方法执行。");
        logger.debug("ObjectMapper 配置完成。");
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师练习详情)。", remoteAddr, requestUrl);

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("teacher") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        TeacherEntity loggedInTeacher = (TeacherEntity) session.getAttribute("teacher");
        int teacherId = loggedInTeacher.getId();
        String teacherName = loggedInTeacher.getName();
        logger.debug("教师已登录，姓名: {} (ID: {})。", teacherName, teacherId);
        String practiceIdStr = req.getParameter("id");
        logger.debug("请求参数 - id: {}", practiceIdStr);

        if (practiceIdStr == null || practiceIdStr.isEmpty()) {
            logger.warn("缺少练习ID参数，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing practice ID");
            return;
        }

        try {
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid practice ID");
                return;
            }

            logger.debug("调用 TeacherPracticeService 获取练习 ID {} 的详情。", practiceId);
            Map<String, Object> practiceDetailsData = teacherPracticeService.getPracticeGradingData(practiceId);

            if (practiceDetailsData == null || practiceDetailsData.isEmpty()) {
                logger.warn("未找到练习 ID {} 的详情或获取失败。", practiceId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Practice not found or details not available");
                return;
            }

            Map<String, Object> practiceMap = (Map<String, Object>) practiceDetailsData.get("practice");
            if (practiceMap == null) {
                logger.error("Service 返回的练习详情数据结构异常，缺少 'practice' 键。");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error: Invalid practice data structure");
                return;
            }

            Integer practiceTeacherId = (Integer) practiceMap.get("teacherId");
            if (practiceTeacherId == null || practiceTeacherId != teacherId) {
                logger.warn("练习 ID {} 不属于当前教师 (ID: {})，拒绝访问。", practiceId, teacherId);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Practice not accessible");
                return;
            }
            logger.debug("成功找到练习 ID {} 的详情，且属于当前教师。", practiceId);

            resp.setContentType("application/json;charset=utf-8");
            logger.debug("成功组织练习详情数据，返回给客户端。");
            mapper.writeValue(resp.getWriter(), practiceDetailsData.get("practice"));

        } catch (NumberFormatException e) {
            logger.warn("无效的练习ID格式: {}，拒绝访问 {}。", practiceIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid practice ID format");
        } catch (Exception e) {
            logger.error("获取教师练习详情时发生错误，练习ID: {}", practiceIdStr, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching practice details: " + e.getMessage());
        }
        logger.info("完成处理 GET 请求: {} (获取教师练习详情)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherPracticeDetailsServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherPracticeDetailsServlet 销毁。");
        super.destroy();
    }
}
