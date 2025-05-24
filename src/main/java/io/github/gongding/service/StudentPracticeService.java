package io.github.gongding.service;

import io.github.gongding.dao.PracticeDao;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.StudentEntity; // 完整性导入，尽管在下面的方法中不直接使用
import io.github.gongding.util.PracticeStatusUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentPracticeService {
    private static final Logger logger = LoggerFactory.getLogger(StudentPracticeService.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final SubmissionDao submissionDao = new SubmissionDao();
    private final StudentService studentService = new StudentService();

    /**
     * 获取学生在特定课程下的练习列表，并包含学生的完成情况和得分信息。
     * 此方法封装了检索学生练习的业务逻辑。
     *
     * @param studentId 学生的ID
     * @param lessonId 课程的ID
     * @return 包含练习详情和学生完成情况的列表，每个练习为一个Map对象。
     */
    public List<Map<String, Object>> getStudentPracticesWithDetails(int studentId, int lessonId) {
        logger.info("尝试检索学生 ID {} 在课程 ID {} 下的练习。", studentId, lessonId);
        List<Map<String, Object>> practicesData = new ArrayList<>();

        try {
            logger.debug("正在检索学生 ID {} 的班级 ID。", studentId);
            List<Integer> studentClassIds = studentService.getAssociatedClassIdsForStudent(studentId);

            if (studentClassIds == null || studentClassIds.isEmpty()) {
                logger.warn("学生 ID {} 未关联任何班级。无法检索练习。", studentId);
                return practicesData;
            }

            int studentClassId = studentClassIds.get(0);
            logger.debug("学生 ID {} 关联的班级 ID 为 {}。正在获取课程 ID {} 和班级 ID {} 的练习。", studentId, studentClassId, lessonId, studentClassId);

            List<PracticeEntity> practices = practiceDao.getPracticesByLessonIdAndClassId(lessonId, studentClassId);
            if (practices == null || practices.isEmpty()) {
                logger.info("未找到课程 ID {} 和班级 ID {} (学生 ID {}) 的练习。", lessonId, studentClassId, studentId);
                return practicesData;
            }
            logger.debug("成功检索到课程 ID {} 和班级 ID {} (学生 ID {}) 的 {} 个练习。", practices.size(), lessonId, studentClassId, studentId);

            LocalDateTime now = LocalDateTime.now();
            logger.debug("当前时间: {}", now);

            for (PracticeEntity practice : practices) {
                Map<String, Object> practiceMap = new HashMap<>();
                practiceMap.put("id", practice.getId());
                practiceMap.put("title", practice.getTitle());
                practiceMap.put("questionNum", practice.getQuestionNum());
                practiceMap.put("startAt", practice.getStartAt());
                practiceMap.put("endAt", practice.getEndAt());
                logger.debug("正在处理练习: ID = {}, 标题 = '{}'", practice.getId(), practice.getTitle());
                logger.debug("练习时间范围: {} - {}", practice.getStartAt(), practice.getEndAt());

                String status = PracticeStatusUtils.calculateStatus(practice.getStartAt(), practice.getEndAt());
                practiceMap.put("status", status);
                logger.debug("练习 {} 状态: {}", practice.getId(), status);

                int completedQuestions = 0;
                Double obtainedScore = null;
                Double totalScore = null;

                if (!"not_started".equals(status)) {
                    logger.debug("练习 {} 状态不是 'not_started'，正在查询学生提交详情。", practice.getId());
                    //获取学生已完成的题目数量
                    completedQuestions = submissionDao.getStudentCompletedQuestionCount(studentId, practice.getId());
                    practiceMap.put("completedQuestions", completedQuestions);
                    logger.debug("学生 ID {} 在练习 {} 中完成了 {} 道题目。", studentId, completedQuestions, practice.getId());

                    if ("ended".equals(status)) {
                        logger.debug("练习 {} 已结束，正在查询学生获得的得分和总分。", practice.getId());
                        //获取学生在此练习中获得的得分
                        obtainedScore = submissionDao.getStudentObtainedScore(studentId, practice.getId());
                        //获取此练习的总分
                        totalScore = submissionDao.getPracticeTotalScore(practice.getId());

                        practiceMap.put("obtainedScore", obtainedScore);
                        practiceMap.put("totalScore", totalScore);
                        logger.debug("学生 ID {} 在练习 {} 中得分: {} / {}", studentId, obtainedScore, totalScore, practice.getId());
                    } else {
                        //如果练习正在进行中，分数尚未最终确定
                        practiceMap.put("obtainedScore", null);
                        practiceMap.put("totalScore", null);
                        logger.debug("练习 {} 正在进行中，分数为 null。", practice.getId());
                    }
                } else {
                    //如果练习尚未开始，设置默认值
                    practiceMap.put("completedQuestions", 0);
                    practiceMap.put("obtainedScore", null);
                    practiceMap.put("totalScore", null);
                    logger.debug("练习 {} 尚未开始，已完成题目数和分数均为默认值。", practice.getId());
                }
                practicesData.add(practiceMap);
                logger.debug("完成处理练习: ID = {}", practice.getId());
            }
            logger.info("成功检索并处理了学生 ID {} 在课程 ID {} 下的 {} 个练习。", practicesData.size(), studentId, lessonId);
        } catch (Exception e) {
            logger.error("检索学生 ID {} 在课程 ID {} 下的练习时发生错误。", studentId, lessonId, e);
            return null;
        }
        return practicesData;
    }

    /**
     * 根据 ID 获取单个 PracticeEntity。
     * 此方法作为 PracticeDao 的一个传递方法。
     *
     * @param practiceId 要检索的练习的 ID。
     * @return 如果找到，返回 PracticeEntity 对象；否则返回 null。
     */
    public PracticeEntity getPracticeById(int practiceId) {
        logger.info("尝试根据 ID {} 检索练习。", practiceId);
        try {
            PracticeEntity practice = practiceDao.getPracticeById(practiceId);
            if (practice != null) {
                logger.debug("成功检索到练习 ID {}。", practiceId);
            } else {
                logger.warn("未找到练习 ID {}。", practiceId);
            }
            return practice;
        } catch (Exception e) {
            logger.error("检索练习 ID {} 时发生错误。", practiceId, e);
            return null;
        }
    }

    /**
     * 获取学生对某个练习的最新提交记录。
     * 此方法封装了从 SubmissionDao 获取最新提交的逻辑。
     *
     * @param studentId 学生的ID。
     * @param practiceId 练习的ID。
     * @return 包含最新提交详情的 Map 对象；如果未找到提交记录或发生错误，则返回 null。
     */
    public Map<String, Object> getLatestStudentSubmission(int studentId, int practiceId) {
        logger.info("尝试获取学生 ID {} 对练习 ID {} 的最新提交记录。", studentId, practiceId);
        try {
            Map<String, Object> submission = submissionDao.getLatestSubmission(studentId, practiceId);
            if (submission != null && !submission.isEmpty()) {
                logger.debug("成功获取学生 ID {} 对练习 ID {} 的最新提交记录。", studentId, practiceId);
            } else {
                logger.info("未找到学生 ID {} 对练习 ID {} 的提交记录。", studentId, practiceId);
            }
            return submission;
        } catch (Exception e) {
            logger.error("获取学生 ID {} 对练习 ID {} 的最新提交记录时发生异常。", studentId, practiceId, e);
            return null;
        }
    }

    /**
     * 提交学生对练习的答案。
     * 此方法封装了检查现有提交并调用 SubmissionDao 进行创建或更新的业务逻辑。
     *
     * @param studentId 学生的ID。
     * @param practiceId 练习的ID。
     * @param answers 包含学生答案的列表，每个答案是一个 Map。
     * @return 提交记录的 ID；如果提交失败则返回 -1。
     */
    public int submitStudentAnswers(int studentId, int practiceId, List<Map<String, Object>> answers) {
        logger.info("尝试提交学生 ID {} 对练习 ID {} 的答案。", studentId, practiceId);
        try {
            //检查是否存在现有提交记录
            logger.debug("检查学生 ID {} 对练习 ID {} 是否已存在提交记录。", studentId, practiceId);
            Map<String, Object> existingSubmission = submissionDao.getLatestSubmission(studentId, practiceId);
            boolean isUpdateOperation = (existingSubmission != null && existingSubmission.containsKey("submissionId"));
            logger.debug("是否存在现有提交记录: {}", isUpdateOperation);

            logger.debug("调用 SubmissionDao.createSubmission 保存提交记录，学生ID: {}, 练习ID: {}", studentId, practiceId);
            int submissionId = submissionDao.createSubmission(studentId, practiceId, answers);

            if (submissionId != -1) {
                logger.info("学生 ID {} 对练习 ID {} 的答案提交成功，提交 ID: {}。", studentId, practiceId, submissionId);
            } else {
                logger.error("学生 ID {} 对练习 ID {} 的答案提交失败。", studentId, practiceId);
            }
            return submissionId;
        } catch (Exception e) {
            logger.error("提交学生 ID {} 对练习 ID {} 的答案时发生异常。", studentId, practiceId, e);
            return -1;
        }
    }
}
