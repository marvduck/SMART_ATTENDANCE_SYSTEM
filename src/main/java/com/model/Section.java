package com.model;

/** adviserName is display-only: it is filled by queries and ignored on insert/update. */
public record Section(Integer sectionId, String sectionName, int gradeLevel, String strand,
                      String schoolYear, Integer adviserId, String adviserName) {

    public static Section create(String sectionName, int gradeLevel, String strand,
                                 String schoolYear, Integer adviserId) {
        return new Section(null, sectionName, gradeLevel, strand, schoolYear, adviserId, null);
    }
}
