package io.github.gongding.service;

import io.github.gongding.dao.SemesterDao;
import io.github.gongding.entity.SemesterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SemesterService {
    private static final Logger logger = LoggerFactory.getLogger(SemesterService.class);
    private SemesterDao semesterDao = new SemesterDao();

    /**
     * 获取所有学期的业务逻辑
     * @return 学期实体列表
     */
    public List<SemesterEntity> getAllSemesters() {
        logger.info("尝试获取所有学期。");
        try {
            List<SemesterEntity> semesters = semesterDao.getAllSemesters();
            logger.debug("成功从 DAO 获取 {} 个学期。", semesters.size());
            return semesters;
        } catch (Exception e) {
            logger.error("获取所有学期时发生异常。", e);
            return null;
        }
    }
}
