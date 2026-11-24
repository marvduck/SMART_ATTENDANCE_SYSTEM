package com.dao;

import com.model.Section;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** CRUD for the sections table. Queries also fill in the adviser's name. */
public class SectionDao {

    private static final String SELECT = """
            SELECT s.section_id, s.section_name, s.grade_level, s.strand, s.school_year,
                   s.adviser_id, t.full_name AS adviser_name
            FROM sections s
            LEFT JOIN teachers t ON t.teacher_id = s.adviser_id""";

    static Section map(ResultSet rs) throws SQLException {
        return new Section(
                rs.getInt("section_id"),
                rs.getString("section_name"),
                rs.getInt("grade_level"),
                rs.getString("strand"),
                rs.getString("school_year"),
                DaoSupport.getInteger(rs, "adviser_id"),
                rs.getString("adviser_name"));
    }

    // ---- Create ----------------------------------------------------------------------------

    /** Returns the new section_id. Throws DaoException (isDuplicateKey) if name/grade/year already exist. */
    public int insert(Section s) {
        return (int) DaoSupport.insert(
                "INSERT INTO sections (section_name, grade_level, strand, school_year, adviser_id) VALUES (?, ?, ?, ?, ?)",
                s.sectionName(), s.gradeLevel(), s.strand(), s.schoolYear(), s.adviserId());
    }

    // ---- Read ------------------------------------------------------------------------------

    public Optional<Section> findById(int sectionId) {
        return DaoSupport.one(SELECT + " WHERE s.section_id = ?", SectionDao::map, sectionId);
    }

    public List<Section> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY s.grade_level, s.section_name", SectionDao::map);
    }

    public List<Section> findByGrade(int gradeLevel) {
        return DaoSupport.list(SELECT + " WHERE s.grade_level = ? ORDER BY s.section_name",
                SectionDao::map, gradeLevel);
    }

    public List<Section> findBySchoolYear(String schoolYear) {
        return DaoSupport.list(SELECT + " WHERE s.school_year = ? ORDER BY s.grade_level, s.section_name",
                SectionDao::map, schoolYear);
    }

    /** Sections advised by a teacher (the teacher's own classes). */
    public List<Section> findByAdviser(int teacherId) {
        return DaoSupport.list(SELECT + " WHERE s.adviser_id = ? ORDER BY s.grade_level, s.section_name",
                SectionDao::map, teacherId);
    }

    public List<Section> search(String keyword) {
        String k = DaoSupport.like(keyword);
        return DaoSupport.list(
                SELECT + " WHERE s.section_name LIKE ? OR s.strand LIKE ? OR t.full_name LIKE ?"
                        + " ORDER BY s.grade_level, s.section_name",
                SectionDao::map, k, k, k);
    }

    public boolean exists(String sectionName, int gradeLevel, String schoolYear) {
        return DaoSupport.count(
                "SELECT COUNT(*) FROM sections WHERE section_name = ? AND grade_level = ? AND school_year = ?",
                sectionName, gradeLevel, schoolYear) > 0;
    }

    /** Number of students currently assigned to the section (any status). */
    public int countStudents(int sectionId) {
        return DaoSupport.count("SELECT COUNT(*) FROM students WHERE section_id = ?", sectionId);
    }

    public int count() {
        return DaoSupport.count("SELECT COUNT(*) FROM sections");
    }

    // ---- Update ----------------------------------------------------------------------------

    public boolean update(Section s) {
        return DaoSupport.update(
                "UPDATE sections SET section_name = ?, grade_level = ?, strand = ?, school_year = ?, adviser_id = ?"
                        + " WHERE section_id = ?",
                s.sectionName(), s.gradeLevel(), s.strand(), s.schoolYear(), s.adviserId(), s.sectionId()) > 0;
    }

    /** Pass null to remove the adviser. */
    public boolean setAdviser(int sectionId, Integer teacherId) {
        return DaoSupport.update("UPDATE sections SET adviser_id = ? WHERE section_id = ?",
                teacherId, sectionId) > 0;
    }

    // ---- Delete ----------------------------------------------------------------------------

    /**
     * Deletes the section. Its class schedules are deleted too; its students are kept but
     * become unassigned (section_id = NULL).
     */
    public boolean delete(int sectionId) {
        return DaoSupport.update("DELETE FROM sections WHERE section_id = ?", sectionId) > 0;
    }
}
