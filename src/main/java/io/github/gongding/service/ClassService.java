package io.github.gongding.service;

import io.github.gongding.dao.ClassDao;
import io.github.gongding.entity.ClassEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClassService {
    private static final Logger logger = LoggerFactory.getLogger(ClassService.class);
    private ClassDao classDao = new ClassDao();

    /**
     * 获取所有班级的业务逻辑
     * @return 班级实体列表
     */
    public List<ClassEntity> getAllClasses() {
        logger.info("尝试获取所有班级。");
        try {
            List<ClassEntity> classes = classDao.getAllClasses();
            logger.debug("成功从 DAO 获取 {} 个班级。", classes.size());
            return classes;
        } catch (Exception e) {
            logger.error("获取所有班级时发生异常。", e);
            return null;
        }
    }
}
