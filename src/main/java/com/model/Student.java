package com.model;

/** sectionName is query-filled and ignored on insert/update. */
public record Student(Integer studentId, Integer userId, String lrn, String fullName, int gradeLevel,
                      Integer sectionId, String guardianContact, StudentStatus status, String sectionName) {

    public static Student create(String lrn, String fullName, int gradeLevel,
                                 Integer sectionId, String guardianContact) {
        return new Student(null, null, lrn, fullName, gradeLevel, sectionId, guardianContact,
                StudentStatus.ENROLLED, null);
    }
}
