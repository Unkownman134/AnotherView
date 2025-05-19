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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeacherPracticeService {
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
        Map<String, Object> gradingData = new HashMap<>();

        //获取练习详情
        PracticeEntity practice = practiceDao.getPracticeById(practiceId);
        if (practice == null) {
            return null;
        }

        //获取所属课程名称
        LessonEntity lesson = lessonDao.getLessonById(practice.getLessonId());
        String lessonName = lesson != null ? lesson.getTitle() : "未知课程";

        //获取关联班级名称列表
        List<ClassEntity> classes = classDao.getClassesByPracticeId(practiceId);
        List<String> classNames = classes.stream()
                .map(ClassEntity::getName)
                .collect(Collectors.toList());
        List<Integer> classIds = classes.stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toList());

        //获取练习包含的所有题目
        List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);
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
        List<StudentEntity> students = studentDao.getStudentsByClassIds(classIds);

        //组织学生提交数据
        List<Map<String, Object>> studentSubmissions = new ArrayList<>();
        //获取练习的总分
        double totalPracticeScore = submissionDao.getPracticeTotalScore(practiceId);

        //遍历与练习关联的每个学生
        for (StudentEntity student : students) {
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("id", student.getId());
            studentData.put("studentNumber", student.getStudentNumber());
            studentData.put("name", student.getName());
            studentData.put("className", student.getClassof() != null ? student.getClassof() : "未知班级");

            //获取该学生对该练习的最新提交记录
            Map<String, Object> latestSubmission = submissionDao.getLatestSubmission(student.getId(), practiceId);

            int completedQuestions = 0;
            double obtainedScore = 0.0;
            Map<Integer, Map<String, Object>> answerStatuses = new HashMap<>();

            if (latestSubmission != null) {
                List<Map<String, Object>> submissionAnswers = (List<Map<String, Object>>) latestSubmission.get("answers");
                int submissionId = (Integer) latestSubmission.get("submissionId");

                //如果找到了学生的最新提交记录
                if (submissionAnswers != null) {
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

                        //统计已完成题目数量
                        if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
                            completedQuestions++;
                        }

                        //计算学生在该练习中获得的总分
                        if (isCorrect != null && isCorrect) {
                            //查找对应的题目实体，获取其分数
                            QuestionEntity q = questions.stream().filter(qi -> qi.getId() == questionId).findFirst().orElse(null);
                            if (q != null) {
                                //加上题目的分数
                                obtainedScore += q.getScore();
                            }
                        } else if (grade != null) {
                            //加上手动评分的分数
                            obtainedScore += grade;
                        }
                        Map<String, Object> status = new HashMap<>();
                        status.put("submissionId", submissionId);
                        status.put("studentAnswer", studentAnswer);
                        status.put("isCorrect", isCorrect);
                        status.put("grade", grade);
                        status.put("feedback", feedback);
                        answerStatuses.put(questionId, status);
                    }
                }
            }

            //将学生的完成情况和得分信息添加到学生数据Map中
            studentData.put("completedQuestions", completedQuestions);
            studentData.put("obtainedScore", obtainedScore);
            studentData.put("totalPracticeScore", totalPracticeScore);
            studentData.put("answerStatuses", answerStatuses);

            studentSubmissions.add(studentData);
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

        return gradingData;
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
        boolean success = submissionDao.updateSubmissionAnswerGrade(submissionId, questionId, grade, feedback);
        return success;
    }
}
