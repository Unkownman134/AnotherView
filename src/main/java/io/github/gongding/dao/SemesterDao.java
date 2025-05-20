package io.github.gongding.dao;

import io.github.gongding.entity.SemesterEntity;
import io.github.gongding.util.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemesterDao {
    private static final Logger logger = LoggerFactory.getLogger(SemesterDao.class);

    /**
     * 获取所有学期
     * @return 学期列表
     */
    public List<SemesterEntity> getAllSemesters() {
        logger.debug("尝试获取所有学期列表。");
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<SemesterEntity> semesters = new ArrayList<>();

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM semester";
            logger.debug("执行 SQL: {}", sql);
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                SemesterEntity semester = new SemesterEntity();
                semester.setId(rs.getInt("semester_id"));
                semester.setName(rs.getString("name"));
                Date startDate = rs.getDate("start_date");
                semester.setStartDate(startDate != null ? startDate.toLocalDate() : null);
                Date endDate = rs.getDate("end_date");
                semester.setEndDate(endDate != null ? endDate.toLocalDate() : null);
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                semester.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
                semesters.add(semester);
                logger.trace("找到学期: ID = {}, Name = {}", semester.getId(), semester.getName());
            }
            logger.debug("成功找到 {} 个学期。", semesters.size());
        } catch (SQLException e) {
            logger.error("获取所有学期列表时发生数据库异常。", e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成获取所有学期列表操作。", semesters.size());
        return semesters;
    }

    /**
     * 根据学期ID获取学期
     * @param id 学期ID
     * @return 学期实体
     */
    public SemesterEntity getSemesterById(int id) {
        logger.debug("尝试根据学期ID {} 获取学期实体。", id);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        SemesterEntity semester = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM semester WHERE semester_id = ?";
            logger.debug("执行 SQL: {} with id = {}", sql, id);
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                semester = new SemesterEntity();
                semester.setId(rs.getInt("semester_id"));
                semester.setName(rs.getString("name"));
                Date startDate = rs.getDate("start_date");
                semester.setStartDate(startDate != null ? startDate.toLocalDate() : null);
                Date endDate = rs.getDate("end_date");
                semester.setEndDate(endDate != null ? endDate.toLocalDate() : null);
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                semester.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);
                logger.debug("成功找到学期 ID {} 的实体，名称: {}", id, semester.getName());
            } else {
                logger.debug("未找到学期 ID {} 的实体。", id);
            }
        } catch (SQLException e) {
            logger.error("根据学期ID {} 获取学期实体时发生数据库异常。", id, e);
        } finally {
            DBUtils.close(conn, pstmt, rs);
            logger.debug("关闭数据库资源。");
        }
        logger.debug("完成根据学期ID {} 获取学期实体操作。", id);
        return semester;
    }
}
