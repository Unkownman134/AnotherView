package io.github.gongding.dao;

import io.github.gongding.entity.SemesterEntity;
import io.github.gongding.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SemesterDao {
    /**
     * 获取所有学期
     * @return 学期列表
     */
    public List<SemesterEntity> getAllSemesters() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<SemesterEntity> semesters = new ArrayList<>();

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM semester";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                SemesterEntity semester = new SemesterEntity();
                semester.setId(rs.getInt("semester_id"));
                semester.setName(rs.getString("name"));
                semester.setStartDate(rs.getDate("start_date").toLocalDate());
                semester.setEndDate(rs.getDate("end_date").toLocalDate());
                semester.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                semesters.add(semester);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return semesters;
    }

    /**
     * 根据学期ID获取学期
     * @param id 学期ID
     * @return 学期实体
     */
    public SemesterEntity getSemesterById(int id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        SemesterEntity semester = null;

        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM semester WHERE semester_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                semester = new SemesterEntity();
                semester.setId(rs.getInt("semester_id"));
                semester.setName(rs.getString("name"));
                semester.setStartDate(rs.getDate("start_date").toLocalDate());
                semester.setEndDate(rs.getDate("end_date").toLocalDate());
                semester.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.close(conn, pstmt, rs);
        }
        return semester;
    }
}

