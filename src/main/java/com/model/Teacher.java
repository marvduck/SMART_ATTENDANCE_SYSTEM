package com.model;

public record Teacher(Integer teacherId, Integer userId, String employeeNo, String fullName,
                      String email, String contactNo) {

    /** A new teacher. userId is filled in when it is saved with its account. */
    public static Teacher create(String employeeNo, String fullName, String email, String contactNo) {
        return new Teacher(null, null, employeeNo, fullName, email, contactNo);
    }
}
