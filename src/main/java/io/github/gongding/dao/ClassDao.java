package io.github.gongding.dao;

import io.github.gongding.entity.ClassEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassDao {
    private static final Logger logger = LoggerFactory.getLogger(ClassDao.class);

    /**
     * 根据班级ID获取班级实体
     * @param id 班级ID
     * @return 班级实体，如果未找到则返回null
     */
    public ClassEntity getClassById(int id) {
        logger.debug("尝试根据班级ID {} 获取班级实体。", id);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ClassEntity cls = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT class_id, name FROM class WHERE class_id = ?";
            logger.debug("执行 SQL: {} with id = {}", sql, id);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                logger.debug("成功找到班级 ID {} 的实体。", id);
            } else {
                logger.debug("未找到班级 ID {} 的实体。", id);
            }
        } catch (SQLException e) {
            // 记录数据库异常
            logger.error("根据班级ID {} 获取班级实体时发生数据库异常。", id, e);
            // Removed: e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs); // 假设 DBUtils.close() 会处理关闭连接的日志
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据班级ID {} 获取班级实体操作。", id);
        return cls;
    }

    /**
     * 根据教师ID获取班级列表
     * @param teacherId 教师ID
     * @return 班级实体列表
     */
    public List<ClassEntity> getClassesByTeacherId(int teacherId) {
        logger.debug("尝试根据教师ID {} 获取班级列表。", teacherId);
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT c.class_id, c.name FROM class c JOIN class_teacher ct ON c.class_id = ct.class_id WHERE ct.teacher_id = ?";
            logger.debug("执行 SQL: {} with teacherId = {}", sql, teacherId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, teacherId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
                logger.trace("找到班级: ID = {}, Name = {}", cls.getId(), cls.getName());
            }
            logger.debug("成功找到 {} 个班级与教师 ID {} 关联。", classes.size(), teacherId);
        } catch (SQLException e) {
            logger.error("根据教师ID {} 获取班级列表时发生数据库异常。", teacherId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据教师ID {} 获取班级列表操作。", teacherId);
        return classes;
    }

    /**
     * 根据练习ID获取关联的班级列表
     * @param practiceId 练习ID
     * @return 班级实体列表
     */
    public List<ClassEntity> getClassesByPracticeId(int practiceId) {
        logger.debug("尝试根据练习ID {} 获取关联班级列表。", practiceId);
        List<ClassEntity> classes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT c.class_id, c.name FROM class c JOIN practice_class pc ON c.class_id = pc.class_id WHERE pc.practice_id = ?";
            logger.debug("执行 SQL: {} with practiceId = {}", sql, practiceId);

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, practiceId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                ClassEntity cls = new ClassEntity();
                cls.setId(rs.getInt("class_id"));
                cls.setName(rs.getString("name"));
                classes.add(cls);
                logger.trace("找到关联班级: ID = {}, Name = {}", cls.getId(), cls.getName());
            }
            logger.debug("成功找到 {} 个班级与练习 ID {} 关联。", classes.size(), practiceId);
        } catch (SQLException e) {
            logger.error("根据练习ID {} 获取关联班级列表时发生数据库异常。", practiceId, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据练习ID {} 获取关联班级列表操作。", practiceId);
        return classes;
    }
}
