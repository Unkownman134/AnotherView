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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/teacher/practices")
public class TeacherPracticesServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticesServlet.class);
    private final TeacherPracticeService teacherPracticeService = new TeacherPracticeService();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public TeacherPracticesServlet() {
        logger.debug("TeacherPracticesServlet 构造方法执行。");
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师练习列表)。", remoteAddr, requestUrl);

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

        String semesterIdStr = req.getParameter("semesterId");
        String searchTerm = req.getParameter("searchTerm");

        List<Map<String, Object>> practicesData;

        try {
            if (semesterIdStr != null && !semesterIdStr.trim().isEmpty()) {
                int semesterId = Integer.parseInt(semesterIdStr);
                logger.debug("请求参数 - semesterId: {}", semesterId);
                logger.debug("调用 TeacherPracticeService 获取教师 ID {} 和学期 ID {} 的练习列表。", teacherId, semesterId);
                practicesData = teacherPracticeService.getPracticesByTeacherIdAndSemesterId(teacherId, semesterId);
                logger.debug("成功找到 {} 个与教师 ID {} 和学期 ID {} 关联的练习。", (practicesData != null ? practicesData.size() : 0), teacherId, semesterId);

            } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                logger.debug("请求参数 - searchTerm: '{}'", searchTerm);
                logger.debug("调用 TeacherPracticeService 获取教师 ID {} 和搜索词 '{}' 的练习列表。", teacherId, searchTerm);
                practicesData = teacherPracticeService.getPracticesByTeacherIdAndSearchTerm(teacherId, searchTerm);
                logger.debug("成功找到 {} 个与搜索词 '{}' 匹配的练习。", (practicesData != null ? practicesData.size() : 0), searchTerm);

            } else {
                logger.debug("获取教师 ID {} 的所有练习列表。", teacherId);
                logger.debug("调用 TeacherPracticeService 获取教师 ID {} 的所有练习列表。", teacherId);
                practicesData = teacherPracticeService.getPracticesByTeacherId(teacherId);
                logger.debug("成功获取教师 ID {} 的所有 {} 个练习。", teacherId, (practicesData != null ? practicesData.size() : 0));
            }

            resp.setContentType("application/json;charset=utf-8");
            logger.debug("成功组织练习列表数据，返回给客户端。");
            mapper.writeValue(resp.getWriter(), practicesData);

        } catch (NumberFormatException e) {
            logger.warn("无效的学期ID格式: {}，拒绝访问 {}。", semesterIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid semester ID format");
        } catch (Exception e) {
            logger.error("获取教师练习列表时发生异常。", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error fetching practices: " + e.getMessage());
        }
        logger.info("完成处理 GET 请求: {} (获取教师练习列表)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherPracticesServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherPracticesServlet 销毁。");
        super.destroy();
    }
}
