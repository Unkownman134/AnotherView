package io.github.gongding.service;

import io.github.gongding.dao.ClassDao;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.dao.QuestionDao;
import io.github.gongding.dao.StudentDao;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.entity.ClassEntity;
import io.github.gongding.entity.LessonEntity;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.entity.StudentEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeacherPracticeService {
    private static final Logger logger = LoggerFactory.getLogger(TeacherPracticeService.class);
    private final PracticeDao practiceDao = new PracticeDao();
    private final LessonDao lessonDao = new LessonDao();
    private final ClassDao classDao = new ClassDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final StudentDao studentDao = new StudentDao();
    private final SubmissionDao submissionDao = new SubmissionDao();

    /**
     * 获取练习的批改所需的所有数据：练习详情、所有相关班级的学生及其提交情况。
     *
     * @param practiceId 练习ID
     * @return 包含练习详情和学生提交列表的Map
     */
    public Map<String, Object> getPracticeGradingData(int practiceId) {
        logger.info("尝试获取练习 {} 的批改数据。", practiceId);
        Map<String, Object> gradingData = new HashMap<>();

        try {
            //获取练习详情
            logger.debug("获取练习 {} 的详情。", practiceId);
            PracticeEntity practice = practiceDao.getPracticeById(practiceId);
            if (practice == null) {
                logger.warn("未找到练习 ID {} 的详情。", practiceId);
                return null;
            }
            logger.debug("成功获取练习 {} 的详情。", practiceId);

            //获取所属课程名称
            logger.debug("获取练习 {} 所属课程的名称，课程ID: {}", practiceId, practice.getLessonId());
            LessonEntity lesson = lessonDao.getLessonById(practice.getLessonId());
            String lessonName = lesson != null ? lesson.getTitle() : "未知课程";
            if (lesson == null) {
                logger.warn("未找到练习 {} 所属的课程 (ID: {})。", practiceId, practice.getLessonId());
            } else {
                logger.debug("成功获取课程名称: {}", lessonName);
            }

            //获取关联班级名称列表
            logger.debug("获取练习 {} 关联的班级信息。", practiceId);
            List<ClassEntity> classes = classDao.getClassesByPracticeId(practiceId);
            List<String> classNames = classes.stream()
                    .map(ClassEntity::getName)
                    .collect(Collectors.toList());
            List<Integer> classIds = classes.stream()
                    .map(ClassEntity::getId)
                    .collect(Collectors.toList());
            logger.debug("练习 {} 关联的班级 IDs: {}", practiceId, classIds);
            logger.debug("练习 {} 关联的班级名称: {}", practiceId, classNames);

            //获取练习包含的所有题目
            logger.debug("获取练习 {} 包含的所有题目。", practiceId);
            List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);
            if (questions == null || questions.isEmpty()) {
                logger.warn("练习 {} 未包含任何题目。", practiceId);
            } else {
                logger.debug("练习 {} 包含 {} 道题目。", practiceId, questions.size());
            }
            List<Map<String, Object>> questionDetails = questions.stream().map(q -> {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("type", q.getType());
                qMap.put("score", q.getScore());
                qMap.put("content", q.getContent());
                qMap.put("correctAnswer", q.getCorrectAnswer());
                return qMap;
            }).collect(Collectors.toList());

            //获取关联班级的所有学生
            logger.debug("获取关联班级 {} 的所有学生。", classIds);
            List<StudentEntity> students = studentDao.getStudentsByClassIds(classIds);
            if (students == null || students.isEmpty()) {
                logger.warn("关联班级 {} 未找到任何学生。", classIds);
            } else {
                logger.debug("关联班级 {} 找到 {} 名学生。", classIds, students.size());
            }

            //组织学生提交数据
            List<Map<String, Object>> studentSubmissions = new ArrayList<>();
            //获取练习的总分
            logger.debug("获取练习 {} 的总分。", practiceId);
            double totalPracticeScore = submissionDao.getPracticeTotalScore(practiceId);
            logger.debug("练习 {} 总分为: {}", practiceId, totalPracticeScore);

            //遍历与练习关联的每个学生
            logger.debug("开始遍历学生提交数据，共 {} 名学生。", students.size());
            for (StudentEntity student : students) {
                Map<String, Object> studentData = new HashMap<>();
                studentData.put("id", student.getId());
                studentData.put("studentNumber", student.getStudentNumber());
                studentData.put("name", student.getName());
                studentData.put("className", student.getClassof() != null ? student.getClassof() : "未知班级");
                logger.debug("处理学生: {} (学号: {})", student.getName(), student.getStudentNumber());

                //获取该学生对该练习的最新提交记录
                logger.debug("获取学生 {} (ID: {}) 对练习 {} 的最新提交记录。", student.getName(), student.getId(), practiceId);
                Map<String, Object> latestSubmission = submissionDao.getLatestSubmission(student.getId(), practiceId);

                int completedQuestions = 0;
                double obtainedScore = 0.0;
                Map<Integer, Map<String, Object>> answerStatuses = new HashMap<>();

                if (latestSubmission != null) {
                    logger.debug("找到学生 {} 对练习 {} 的最新提交记录。", student.getName(), practiceId);
                    List<Map<String, Object>> submissionAnswers = (List<Map<String, Object>>) latestSubmission.get("answers");
                    int submissionId = (Integer) latestSubmission.get("submissionId");
                    logger.debug("提交记录 ID: {}", submissionId);

                    //如果找到了学生的最新提交记录
                    if (submissionAnswers != null) {
                        logger.debug("提交记录 {} 包含 {} 个答案。", submissionId, submissionAnswers.size());
                        for (Map<String, Object> answer : submissionAnswers) {
                            int questionId = (Integer) answer.get("questionId");
                            String studentAnswer = (String) answer.get("studentAnswer");
                            Boolean isCorrect = (Boolean) answer.get("isCorrect");
                            Object gradeObj = answer.get("grade");
                            Double grade = null;
                            if (gradeObj instanceof BigDecimal) {
                                grade = ((BigDecimal) gradeObj).doubleValue();
                            } else if (gradeObj instanceof Number) {
                                grade = ((Number) gradeObj).doubleValue();
                            }
                            String feedback = (String) answer.get("feedback");

                            logger.debug("处理提交记录 {} 中的题目 {} 答案。", submissionId, questionId);
                            logger.trace("题目 {} 答案详情 - 学生答案: {}, 是否正确: {}, 评分: {}, 反馈: {}", questionId, studentAnswer, isCorrect, grade, feedback); // 更详细的答案信息，TRACE级别

                            //统计已完成题目数量
                            if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
                                completedQuestions++;
                                logger.trace("题目 {} 被标记为已完成。", questionId);
                            }

                            //计算学生在该练习中获得的总分
                            if (isCorrect != null && isCorrect) {
                                //查找对应的题目实体，获取其分数
                                QuestionEntity q = questions.stream().filter(qi -> qi.getId() == questionId).findFirst().orElse(null);
                                if (q != null) {
                                    //加上题目的分数
                                    obtainedScore += q.getScore();
                                    logger.trace("题目 {} (自动评分) 得分 {}，当前总分 {}", questionId, q.getScore(), obtainedScore);
                                } else {
                                    logger.warn("提交记录 {} 中的题目 {} 未在练习 {} 的题目列表中找到。", submissionId, questionId, practiceId);
                                }
                            } else if (grade != null) {
                                //加上手动评分的分数
                                obtainedScore += grade;
                                logger.trace("题目 {} (手动评分) 得分 {}，当前总分 {}", questionId, grade, obtainedScore);
                            }
                            Map<String, Object> status = new HashMap<>();
                            status.put("submissionId", submissionId);
                            status.put("studentAnswer", studentAnswer);
                            status.put("isCorrect", isCorrect);
                            status.put("grade", grade);
                            status.put("feedback", feedback);
                            answerStatuses.put(questionId, status);
                        }
                    } else {
                        logger.warn("找到学生 {} 对练习 {} 的提交记录 {}，但答案列表为空。", student.getName(), practiceId, submissionId);
                    }
                } else {
                    logger.debug("学生 {} (ID: {}) 对练习 {} 没有提交记录。", student.getName(), student.getId(), practiceId);
                }

                //将学生的完成情况和得分信息添加到学生数据Map中
                studentData.put("completedQuestions", completedQuestions);
                studentData.put("obtainedScore", obtainedScore);
                studentData.put("totalPracticeScore", totalPracticeScore);
                studentData.put("answerStatuses", answerStatuses);

                studentSubmissions.add(studentData);
                logger.debug("完成处理学生: {} (学号: {}) 的提交数据。", student.getName(), student.getStudentNumber());
            }

            gradingData.put("practice", Map.of(
                    "id", practice.getId(),
                    "title", practice.getTitle(),
                    "lessonName", lessonName,
                    "classNames", classNames,
                    "questionNum", practice.getQuestionNum(),
                    "endAt", practice.getEndAt(),
                    "questions", questionDetails
            ));
            gradingData.put("students", studentSubmissions);

            logger.info("成功获取练习 {} 的批改数据。", practiceId);
            return gradingData;

        } catch (Exception e) {
            logger.error("获取练习 {} 的批改数据过程中发生异常。", practiceId, e);
            return null;
        }
    }

    /**
     * 保存特定提交答案的评分和反馈。
     * 这个方法用于教师手动批改单个题目答案时调用，更新数据库中`submission_answer`表的评分和反馈字段。
     *
     * @param submissionId 提交记录的唯一标识符ID。
     * @param questionId 题目答案所属的题目的唯一标识符ID。
     * @param grade 教师给出的分数。
     * @param feedback 教师给出的反馈信息（可以为 null）。
     * @return 如果更新成功返回true，否则返回false。
     */
    public boolean saveSubmissionGrade(int submissionId, int questionId, double grade, String feedback) {
        logger.info("尝试保存提交记录 {} 中题目 {} 的评分和反馈。", submissionId, questionId);
        logger.debug("评分: {}, 反馈: {}", grade, feedback);

        try {
            logger.debug("调用 SubmissionDao 更新提交记录 {} 中题目 {} 的评分。", submissionId, questionId);
            boolean success = submissionDao.updateSubmissionAnswerGrade(submissionId, questionId, grade, feedback);

            if (success) {
                logger.info("成功保存提交记录 {} 中题目 {} 的评分。", submissionId, questionId);
            } else {
                logger.error("保存提交记录 {} 中题目 {} 的评分失败，数据库操作可能出现问题或返回false。", submissionId, questionId);
            }
            return success;
        } catch (Exception e) {
            logger.error("保存提交记录 {} 中题目 {} 的评分过程中发生异常。", submissionId, questionId, e);
            return false;
        }
    }
}
