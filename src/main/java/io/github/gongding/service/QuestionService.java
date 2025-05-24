package io.github.gongding.service;

import io.github.gongding.dao.QuestionDao;
import io.github.gongding.entity.QuestionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    private final QuestionDao questionDao = new QuestionDao();

    /**
     * 获取所有题目的业务逻辑
     * @return 题目实体列表
     */
    public List<QuestionEntity> getAllQuestions() {
        logger.info("尝试检索所有题目。");
        try {
            List<QuestionEntity> questions = questionDao.getAllQuestions();
            logger.debug("成功从DAO检索 {} 个题目.", questions.size());
            return questions;
        } catch (Exception e) {
            logger.error("检索所有题目时发生异常。", e);
            return null;
        }
    }

    /**
     * 根据课程ID获取题目列表的业务逻辑
     * @param lessonId 课程ID
     * @return 题目实体列表
     */
    public List<QuestionEntity> getQuestionsByLessonId(int lessonId) {
        logger.info("尝试根据课程ID {} 检索题目。", lessonId);
        try {
            List<QuestionEntity> questions = questionDao.getQuestionsByLessonId(lessonId);
            logger.debug("成功从DAO检索课程ID {} 的 {} 个题目.", lessonId, questions.size());
            return questions;
        } catch (Exception e) {
            logger.error("检索课程ID {} 的题目时发生异常。", lessonId, e);
            return null;
        }
    }

    /**
     * 根据练习ID获取题目列表的业务逻辑。
     * 此方法封装了从 QuestionDao 获取特定练习题目的逻辑。
     *
     * @param practiceId 练习ID
     * @return 题目实体列表
     */
    public List<QuestionEntity> getQuestionsByPracticeId(int practiceId) {
        logger.info("尝试根据练习ID {} 检索题目。", practiceId);
        try {
            List<QuestionEntity> questions = questionDao.getQuestionsByPracticeId(practiceId);
            logger.debug("成功从DAO检索练习ID {} 的 {} 个题目。", practiceId, questions.size());
            return questions;
        } catch (Exception e) {
            logger.error("检索练习ID {} 的题目时发生异常。", practiceId, e);
            return null;
        }
    }
}
