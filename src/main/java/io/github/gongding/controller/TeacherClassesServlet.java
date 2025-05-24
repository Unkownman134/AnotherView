package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gongding.entity.ClassEntity;
import io.github.gongding.entity.TeacherEntity;
import io.github.gongding.service.ClassService;
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

@WebServlet("/api/teacher/classes")
public class TeacherClassesServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherClassesServlet.class);
    private final ClassService classService = new ClassService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取教师班级列表)。", remoteAddr, requestUrl);

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


        try {
            logger.debug("调用 ClassService 获取教师 ID {} 关联的班级列表。", teacherId);
            List<ClassEntity> classes = classService.getClassesByTeacherId(teacherId);
            logger.debug("成功获取教师 ID {} 关联的 {} 个班级。", teacherId, (classes != null ? classes.size() : 0));

            resp.setContentType("application/json;charset=utf-8");
            logger.debug("成功组织班级列表数据，返回给客户端。");
            mapper.writeValue(resp.getWriter(), classes);

        } catch (Exception e) {
            logger.error("获取教师 ID {} 关联的班级列表或处理响应时发生异常。", teacherId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        logger.info("完成处理 GET 请求: {} (获取教师班级列表)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("TeacherClassesServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("TeacherClassesServlet 销毁。");
        super.destroy();
    }
}
