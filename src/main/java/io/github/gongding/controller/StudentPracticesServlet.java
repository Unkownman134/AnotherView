package io.github.gongding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.service.StudentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/student/practices")
public class StudentPracticesServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StudentPracticesServlet.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final SubmissionDao submissionDao = new SubmissionDao();
    private final StudentService studentService = new StudentService();
    private final ObjectMapper mapper = new ObjectMapper();

    public StudentPracticesServlet() {
        logger.debug("StudentPracticesServlet 构造方法执行。");
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        logger.debug("ObjectMapper 配置完成。");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String requestUrl = req.getRequestURL().toString();
        String remoteAddr = req.getRemoteAddr();
        logger.info("收到来自 IP 地址 {} 的 GET 请求: {} (获取学生练习列表)。", remoteAddr, requestUrl);

        resp.setContentType("application/json;charset=utf-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("student") == null) {
            logger.warn("未登录或会话过期，拒绝访问 {}。", requestUrl);
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }

        StudentEntity student = (StudentEntity) session.getAttribute("student");
        int studentId = student.getId();
        String studentNumber = student.getStudentNumber();
        logger.debug("学生已登录，学号: {} (ID: {})。", studentNumber, studentId);

        String lessonIdStr = null;
        try {
            //从请求参数中获取名为"lessonId" 的值，即课程的ID字符串
            lessonIdStr = req.getParameter("lesson_id");
            logger.debug("请求参数 - lesson_id: {}", lessonIdStr);

            if (lessonIdStr == null || lessonIdStr.isEmpty()) {
                logger.warn("缺少课程ID参数，拒绝访问 {}。", requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "缺少课程ID参数");
                return;
            }
            int lessonId = Integer.parseInt(lessonIdStr);
            logger.debug("解析的课程ID: {}", lessonId);

            if (lessonId <= 0) {
                logger.warn("无效的课程ID格式 (非正数): {}，拒绝访问 {}。", lessonIdStr, requestUrl);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "无效的课程ID");
                return;
            }

            // 获取学生所属的班级ID列表
            List<Integer> studentClassIds = studentService.getAssociatedClassIdsForStudent(studentId);
            if (studentClassIds == null || studentClassIds.isEmpty()) {
                logger.warn("学生 ID {} 未关联任何班级，无法获取其练习列表。", studentId);
                mapper.writeValue(resp.getWriter(), new ArrayList<>());
                return;
            }
            int studentClassId = studentClassIds.get(0);

            logger.debug("调用 PracticeDao 获取课程 ID {} 和班级 ID {} 的练习列表。", lessonId, studentClassId);
            List<PracticeEntity> practices = practiceDao.getPracticesByLessonIdAndClassId(lessonId, studentClassId);
            logger.debug("成功获取课程 ID {} 和班级 ID {} 的 {} 个练习。", lessonId, studentClassId, (practices != null ? practices.size() : 0));

            List<Map<String, Object>> practicesData = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();
            logger.debug("当前时间: {}", now);

            //遍历获取到的练习列表
            logger.debug("开始处理练习列表，共 {} 个练习。", (practices != null ? practices.size() : 0));
            if (practices != null) {
                for (PracticeEntity practice : practices) {
                    //创建一个HashMap，用于存储当前练习的详细信息和学生完成情况
                    Map<String, Object> practiceMap = new HashMap<>();
                    practiceMap.put("id", practice.getId());
                    practiceMap.put("title", practice.getTitle());
                    practiceMap.put("questionNum", practice.getQuestionNum());
                    practiceMap.put("startAt", practice.getStartAt());
                    practiceMap.put("endAt", practice.getEndAt());
                    logger.debug("处理练习: ID = {}, Title = '{}'", practice.getId(), practice.getTitle());
                    logger.debug("练习时间范围: {} - {}", practice.getStartAt(), practice.getEndAt());


                    String status;
                    if (practice.getStartAt() != null && now.isBefore(practice.getStartAt())) {
                        status = "not_started";
                    } else if (practice.getEndAt() != null && now.isAfter(practice.getEndAt())) {
                        status = "ended";
                    } else {
                        status = "in_progress";
                    }
                    practiceMap.put("status", status);
                    logger.debug("练习 {} 状态: {}", practice.getId(), status);


                    int completedQuestions = 0;
                    double obtainedScore = 0.0;
                    double totalScore = 0.0;

                    if (!"not_started".equals(status)) {
                        logger.debug("练习 {} 状态不是 'not_started'，查询学生提交情况。", practice.getId());
                        //获取学生已完成的题目数量
                        logger.debug("调用 SubmissionDao 获取学生 {} 在练习 {} 中已完成题目数量。", studentId, practice.getId());
                        completedQuestions = submissionDao.getStudentCompletedQuestionCount(studentId, practice.getId());
                        practiceMap.put("completedQuestions", completedQuestions);
                        logger.debug("学生 {} 在练习 {} 中已完成 {} 道题目。", studentId, practice.getId(), completedQuestions);


                        if ("ended".equals(status)) {
                            logger.debug("练习 {} 已截止，查询学生得分和总分。", practice.getId());
                            //获取学生在该练习中获得的得分
                            logger.debug("调用 SubmissionDao 获取学生 {} 在练习 {} 中获得的总分数。", studentId, practice.getId());
                            obtainedScore = submissionDao.getStudentObtainedScore(studentId, practice.getId());
                            //获取该练习的总分
                            logger.debug("调用 SubmissionDao 获取练习 {} 的总分数。", practice.getId());
                            totalScore = submissionDao.getPracticeTotalScore(practice.getId());

                            practiceMap.put("obtainedScore", obtainedScore);
                            practiceMap.put("totalScore", totalScore);
                            logger.debug("学生 {} 在练习 {} 中得分: {} / {}", studentId, practice.getId(), obtainedScore, totalScore);
                        } else {
                            //如果练习未结束，得分和总分设为null
                            practiceMap.put("obtainedScore", null);
                            practiceMap.put("totalScore", null);
                            logger.debug("练习 {} 未截止，得分和总分设为 null。", practice.getId());
                        }
                    } else {
                        //如果练习未开始，已完成题目数量、得分和总分都设为默认值或null
                        practiceMap.put("completedQuestions", 0);
                        practiceMap.put("obtainedScore", null);
                        practiceMap.put("totalScore", null);
                        logger.debug("练习 {} 未开始，完成题目数、得分、总分设为默认值。", practice.getId());
                    }

                    practicesData.add(practiceMap);
                    logger.debug("完成处理练习: ID = {}", practice.getId());
                }
            }

            mapper.writeValue(resp.getWriter(), practicesData);
            logger.debug("成功组织练习列表数据，返回给客户端。");

        } catch (NumberFormatException e) {
            logger.warn("课程ID格式不正确: {}，拒绝访问 {}。", lessonIdStr, requestUrl, e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "参数错误: 无效的课程ID");
        } catch (Exception e) {
            logger.error("处理学生练习列表请求时发生异常，课程ID: {}", req.getParameter("lesson_id"), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器错误: " + e.getMessage());
        }
        logger.info("完成处理 GET 请求: {} (获取学生练习列表)。", requestUrl);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("StudentPracticesServlet 初始化成功。");
    }

    @Override
    public void destroy() {
        logger.info("StudentPracticesServlet 销毁。");
        super.destroy();
    }
}
