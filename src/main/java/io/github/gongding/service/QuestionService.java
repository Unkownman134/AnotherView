package io.github.gongding.service;

import io.github.gongding.dao.QuestionDao;
import io.github.gongding.entity.QuestionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    private final QuestionDao questionDao = new QuestionDao();

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
}
