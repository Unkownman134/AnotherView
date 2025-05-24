package io.github.gongding.service;

import io.github.gongding.dao.ClassDao;
import io.github.gongding.dao.LessonDao;
import io.github.gongding.dao.PracticeDao;
import io.github.gongding.dao.QuestionDao;
import io.github.gongding.dao.StudentDao;
import io.github.gongding.dao.SubmissionDao;
import io.github.gongding.dao.SemesterDao;
import io.github.gongding.dao.TeacherDao;

import io.github.gongding.entity.ClassEntity;
import io.github.gongding.entity.LessonEntity;
import io.github.gongding.entity.PracticeEntity;
import io.github.gongding.entity.QuestionEntity;
import io.github.gongding.entity.StudentEntity;
import io.github.gongding.entity.SemesterEntity;
import io.github.gongding.entity.TeacherEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final SemesterDao semesterDao = new SemesterDao();
    private final TeacherDao teacherDao = new TeacherDao();

    /**
     * 根据教师ID获取所有练习列表的业务逻辑。
     * 直接调用 PracticeDao 的相应方法。
     * @param teacherId 教师ID
     * @return 练习数据列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherId(int teacherId) {
        logger.info("尝试获取教师ID {} 的所有练习列表。", teacherId);
        try {
            List<Map<String, Object>> practicesData = practiceDao.getPracticesByTeacherId(teacherId);
            logger.debug("成功从 DAO 获取教师ID {} 的 {} 个练习。", teacherId, practicesData.size());
            return practicesData;
        } catch (Exception e) {
            logger.error("获取教师ID {} 的所有练习列表时发生异常。", teacherId, e);
            return null;
        }
    }

    /**
     * 根据教师ID和学期ID获取练习列表的业务逻辑。
     * 直接调用 PracticeDao 的相应方法。
     * @param teacherId 教师ID
     * @param semesterId 学期ID
     * @return 练习数据列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherIdAndSemesterId(int teacherId, int semesterId) {
        logger.info("尝试获取教师ID {} 和学期ID {} 的练习列表。", teacherId, semesterId);
        try {
            List<Map<String, Object>> practicesData = practiceDao.getPracticesByTeacherIdAndSemesterId(teacherId, semesterId);
            logger.debug("成功从 DAO 获取教师ID {} 和学期ID {} 的 {} 个练习。", teacherId, semesterId, practicesData.size());
            return practicesData;
        } catch (Exception e) {
            logger.error("获取教师ID {} 和学期ID {} 的练习列表时发生异常。", teacherId, semesterId, e);
            return null;
        }
    }

    /**
     * 根据教师ID和搜索词获取练习列表的业务逻辑。
     * 直接调用 PracticeDao 的相应方法。
     * @param teacherId 教师ID
     * @param searchTerm 搜索词
     * @return 练习数据列表，每个练习为一个Map对象
     */
    public List<Map<String, Object>> getPracticesByTeacherIdAndSearchTerm(int teacherId, String searchTerm) {
        logger.info("尝试获取教师ID {} 和搜索词 '{}' 的练习列表。", teacherId, searchTerm);
        try {
            List<Map<String, Object>> practicesData = practiceDao.getPracticesByTeacherIdAndSearchTerm(teacherId, searchTerm);
            logger.debug("成功从 DAO 获取教师ID {} 和搜索词 '{}' 的 {} 个练习。", teacherId, searchTerm, practicesData.size());
            return practicesData;
        } catch (Exception e) {
            logger.error("获取教师ID {} 和搜索词 '{}' 的练习列表时发生异常。", teacherId, searchTerm, e);
            return null;
        }
    }

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

            // 获取所属学期名称
            logger.debug("获取练习 {} 所属学期的名称，学期ID: {}", practiceId, practice.getSemesterId());
            SemesterEntity semester = semesterDao.getSemesterById(practice.getSemesterId());
            String semesterName = semester != null ? semester.getName() : "未知学期";
            if (semester == null) {
                logger.warn("未找到练习 {} 所属的学期 (ID: {})。", practiceId, practice.getSemesterId());
            } else {
                logger.debug("成功获取学期名称: {}", semesterName);
            }

            // 获取所属教师姓名
            logger.debug("获取练习 {} 所属教师的姓名，教师ID: {}", practiceId, practice.getTeacherId());
            TeacherEntity teacher = teacherDao.getTeacherById(practice.getTeacherId());
            String teacherName = teacher != null ? teacher.getName() : "未知教师";
            if (teacher == null) {
                logger.warn("未找到练习 {} 所属的教师 (ID: {})。", practiceId, practice.getTeacherId());
            } else {
                logger.debug("成功获取教师姓名: {}", teacherName);
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
                qMap.put("options", q.getOptions());
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

            Map<String, Object> practiceMap = new HashMap<>();
            practiceMap.put("id", practice.getId());
            practiceMap.put("title", practice.getTitle());
            practiceMap.put("questionNum", practice.getQuestionNum());
            practiceMap.put("classof", practice.getClassof());
            practiceMap.put("startAt", practice.getStartAt());
            practiceMap.put("endAt", practice.getEndAt());
            practiceMap.put("status", practice.getStatus());
            practiceMap.put("lessonId", practice.getLessonId());
            practiceMap.put("semesterId", practice.getSemesterId());
            practiceMap.put("teacherId", practice.getTeacherId());
            practiceMap.put("lessonName", lessonName);
            practiceMap.put("semesterName", semesterName);
            practiceMap.put("teacherName", teacherName);
            practiceMap.put("questions", questionDetails);

            gradingData.put("practice", practiceMap);
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

    /**
     * 在Service层创建新练习的业务逻辑。
     * 该方法封装了获取学期ID、构建班级信息字符串以及调用DAO层创建练习的步骤。
     *
     * @param teacherId 教师ID
     * @param title 练习标题
     * @param lessonId 课程ID
     * @param classIds 关联的班级ID列表
     * @param questionIds 包含的题目ID数组
     * @param startTime 练习开始时间
     * @param endTime 练习结束时间
     * @return 新创建练习的ID，如果创建失败则返回-1
     */
    public int createPracticeWithDetails(int teacherId, String title, int lessonId, List<Integer> classIds, int[] questionIds, LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("尝试在 Service 层创建新练习 - 标题: {}, 教师ID: {}, 课程ID: {}", title, teacherId, lessonId);
        try {
            logger.debug("调用 LessonDao 获取课程 ID {} 的信息以获取学期ID。", lessonId);
            LessonEntity lesson = lessonDao.getLessonById(lessonId);
            if (lesson == null) {
                logger.error("创建练习失败，未找到课程 ID {} 的信息。", lessonId);
                return -1;
            }
            int semesterId = lesson.getSemesterId();
            logger.debug("获取到课程 ID {} 的学期 ID: {}", lessonId, semesterId);

            String classofString = "";
            if (classIds != null && !classIds.isEmpty()) {
                List<String> classNames = new ArrayList<>();
                logger.debug("获取班级名称，共 {} 个班级ID。", classIds.size());
                for (Integer classId : classIds) {
                    logger.trace("获取班级 ID {} 的名称。", classId);
                    ClassEntity cls = classDao.getClassById(classId);
                    if (cls != null) {
                        classNames.add(cls.getName());
                        logger.trace("找到班级 ID {} 的名称: {}", classId, cls.getName());
                    } else {
                        logger.warn("创建练习时，班级 ID {} 在数据库中未找到。", classId);
                    }
                }
                classofString = classNames.stream().collect(Collectors.joining(","));
                logger.debug("格式化后的班级信息字符串: '{}'", classofString);
            } else {
                logger.debug("没有关联的班级ID。");
            }

            logger.debug("调用 PracticeDao.createPractice 创建练习，教师ID: {}, 课程ID: {}, 学期ID: {}, 标题: '{}', 班级信息: '{}', 开始时间: {}, 结束时间: {}, 题目数量: {}",
                    teacherId, lessonId, semesterId, title, classofString, startTime, endTime, (questionIds != null ? questionIds.length : 0));
            int newPracticeId = practiceDao.createPractice(teacherId, lessonId, semesterId, title, classIds.stream().mapToInt(i -> i).toArray(), classofString, startTime, endTime, questionIds);
            logger.debug("PracticeDao.createPractice 返回新练习 ID: {}", newPracticeId);

            return newPracticeId;
        } catch (Exception e) {
            logger.error("在 Service 层创建练习时发生异常。", e);
            return -1;
        }
    }

    /**
     * 更新练习及其关联的题目。
     * @param practice 包含更新信息的 PracticeEntity 对象。
     * @param questionIds 练习包含的题目ID数组。
     * @return 如果更新成功返回true，否则返回false。
     */
    public boolean updatePracticeAndQuestions(PracticeEntity practice, int[] questionIds) {
        logger.info("尝试更新练习 ID: {} 及其关联的题目。", practice.getId());
        try {
            boolean success = practiceDao.updatePracticeAndQuestions(
                    practice.getId(),
                    practice.getTitle(),
                    practice.getClassof(),
                    practice.getStartAt(),
                    practice.getEndAt(),
                    questionIds
            );
            if (success) {
                logger.info("成功更新练习 ID: {}.", practice.getId());
            } else {
                logger.warn("更新练习 ID: {} 失败，可能练习不存在或数据库操作未成功。", practice.getId());
            }
            return success;
        } catch (Exception e) {
            logger.error("更新练习 ID: {} 及其关联题目时发生异常。", practice.getId(), e);
            return false;
        }
    }

    /**
     * 在Service层复用现有练习创建新的练习记录。
     * 该方法封装了获取学期ID、构建班级信息字符串以及调用DAO层创建练习的步骤。
     *
     * @param teacherId 创建新练习的教师ID
     * @param lessonId 新练习所属的课程ID
     * @param newTitle 新练习的标题
     * @param classIds 新练习关联的班级ID列表
     * @param newStartTime 新练习的开始时间
     * @param newEndTime 新练习的结束时间
     * @param questionIds 新练习包含的题目ID数组
     * @return 新创建练习的ID，如果创建失败则返回-1。
     */
    public int reusePractice(int teacherId, int lessonId, String newTitle, List<Integer> classIds, LocalDateTime newStartTime, LocalDateTime newEndTime, int[] questionIds) {
        logger.info("尝试在 Service 层复用练习创建新练习 - 标题: {}, 教师ID: {}, 课程ID: {}", newTitle, teacherId, lessonId);
        try {
            logger.debug("调用 LessonDao 获取课程 ID {} 的信息以获取学期ID。", lessonId);
            LessonEntity lesson = lessonDao.getLessonById(lessonId);
            if (lesson == null) {
                logger.error("复用练习失败，未找到课程 ID {} 的信息。", lessonId);
                return -1;
            }
            int semesterId = lesson.getSemesterId();
            logger.debug("获取到课程 ID {} 的学期 ID: {}", lessonId, semesterId);

            String classofString = "";
            if (classIds != null && !classIds.isEmpty()) {
                List<String> classNames = new ArrayList<>();
                logger.debug("获取班级名称，共 {} 个班级ID。", classIds.size());
                for (Integer classId : classIds) {
                    logger.trace("获取班级 ID {} 的名称。", classId);
                    ClassEntity cls = classDao.getClassById(classId);
                    if (cls != null) {
                        classNames.add(cls.getName());
                        logger.trace("找到班级 ID {} 的名称: {}", classId, cls.getName());
                    } else {
                        logger.warn("复用练习时，班级 ID {} 在数据库中未找到。", classId);
                    }
                }
                classofString = classNames.stream().collect(Collectors.joining(","));
                logger.debug("格式化后的班级信息字符串: '{}'", classofString);
            } else {
                logger.debug("没有关联的班级ID。");
            }

            logger.debug("调用 PracticeDao.createPracticeFromReuse 创建练习，教师ID: {}, 课程ID: {}, 学期ID: {}, 标题: '{}', 班级信息: '{}', 开始时间: {}, 结束时间: {}, 题目数量: {}",
                    teacherId, lessonId, semesterId, newTitle, classofString, newStartTime, newEndTime, (questionIds != null ? questionIds.length : 0));
            int newPracticeId = practiceDao.createPracticeFromReuse(teacherId, lessonId, semesterId, newTitle, classIds.stream().mapToInt(i -> i).toArray(), newStartTime, newEndTime, questionIds);
            logger.debug("PracticeDao.createPracticeFromReuse 返回新练习 ID: {}", newPracticeId);

            return newPracticeId;
        } catch (Exception e) {
            logger.error("在 Service 层复用练习时发生异常。", e);
            return -1;
        }
    }

    /**
     * 延长练习的截止时间。
     * @param practiceId 练习ID。
     * @param newEndTime 新的截止时间。
     * @return 如果延长成功返回true，否则返回false。
     */
    public boolean extendPracticeTime(int practiceId, LocalDateTime newEndTime) {
        logger.info("尝试延长练习 ID {} 的截止时间到 {}.", practiceId, newEndTime);
        try {
            boolean success = practiceDao.extendPracticeTime(practiceId, newEndTime);
            if (success) {
                logger.info("成功延长练习 ID {} 的截止时间。", practiceId);
            } else {
                logger.warn("延长练习 ID {} 的截止时间失败，可能练习不存在或数据库操作未成功。", practiceId);
            }
            return success;
        } catch (Exception e) {
            logger.error("延长练习 ID {} 的截止时间时发生异常。", practiceId, e);
            return false;
        }
    }
}
