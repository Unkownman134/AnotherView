package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.*;
import io.github.gongding.entity.*;
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

@WebServlet("/api/teacher/practice/details")
public class TeacherPracticeDetailsServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticeDetailsServlet.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final LessonDao lessonDao = new LessonDao();
    private final SemesterDao semesterDao = new SemesterDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final TeacherDao teacherDao = new TeacherDao();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public TeacherPracticeDetailsServlet() {
        logger.debug("TeacherPracticeDetailsServlet 构造方法执行。");
        // ObjectMapper 配置已在字段初始化时完成
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
        //尝试将practiceIdStr字符串解析为整数类型的practiceId
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
            //尝试将practiceIdStr字符串解析为整数类型的practiceId
            int practiceId = Integer.parseInt(practiceIdStr);
            logger.debug("解析的练习ID: {}", practiceId);

            if (practiceId <= 0) {
                logger.warn("无效的练习ID (非正数): {}，拒绝访问 {}。", practiceIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid practice ID");
                return;
            }

            //根据练习ID从数据库获取PracticeEntity对象
            logger.debug("调用 PracticeDao 获取练习 ID {} 的详情。", practiceId);
            PracticeEntity practice = practiceDao.getPracticeById(practiceId);


            //检查是否找到了练习并且该练习的教师ID与当前登录教师的ID一致
            if (practice == null) {
                logger.warn("未找到练习 ID {} 的详情。", practiceId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Practice not found");
                return;
            }
            if (practice.getTeacherId() != teacherId) {
                logger.warn("练习 ID {} 不属于当前教师 (ID: {})，拒绝访问。", practiceId, teacherId);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Practice not accessible");
                return;
            }
            logger.debug("成功找到练习 ID {} 的详情，且属于当前教师。", practiceId);

            //如果练习存在且属于当前教师，则继续获取关联的详细信息
            logger.debug("获取练习 {} 关联的课程、学期、教师信息和题目列表。", practiceId);
            LessonEntity lesson = lessonDao.getLessonById(practice.getLessonId());
            SemesterEntity semester = semesterDao.getSemesterById(practice.getSemesterId());
            TeacherEntity teacher = teacherDao.getTeacherById(practice.getTeacherId());
            //获取练习包含的题目列表
            List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);

            if (lesson == null) logger.warn("未找到练习 {} 关联的课程 (ID: {})。", practiceId, practice.getLessonId());
            if (semester == null) logger.warn("未找到练习 {} 关联的学期 (ID: {})。", practiceId, practice.getSemesterId());
            if (teacher == null) logger.error("未找到练习 {} 关联的教师 (ID: {})，数据可能异常。", practiceId, practice.getTeacherId()); // 这通常是数据问题
            if (questions == null) logger.warn("获取练习 {} 的题目列表返回 null。", practiceId);

            Map<String, Object> practiceDetails = new HashMap<>();
            practiceDetails.put("id", practice.getId());
            practiceDetails.put("title", practice.getTitle());
            practiceDetails.put("questionNum", practice.getQuestionNum());
            practiceDetails.put("classof", practice.getClassof());
            practiceDetails.put("startAt", practice.getStartAt());
            practiceDetails.put("endAt", practice.getEndAt());
            practiceDetails.put("status", practice.getStatus());
            practiceDetails.put("lessonId", practice.getLessonId());
            practiceDetails.put("semesterId", practice.getSemesterId());
            practiceDetails.put("teacherId", practice.getTeacherId());


            practiceDetails.put("lessonName", lesson != null ? lesson.getTitle() : "未知课程");
            practiceDetails.put("semesterName", semester != null ? semester.getName() : "未知学期");
            practiceDetails.put("teacherName", teacher != null ? teacher.getName() : "未知教师");

            practiceDetails.put("questions", questions);

            resp.setContentType("application/json;charset=utf-8");
            logger.debug("成功组织练习详情数据，返回给客户端。");
            mapper.writeValue(resp.getWriter(), practiceDetails);

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
