package com.model;

public record Teacher(Integer teacherId, Integer userId, String employeeNo, String fullName,
                      String email, String contactNo) {

    /** Creates a teacher without an account; insertWithAccount fills userId. */
    public static Teacher create(String employeeNo, String fullName, String email, String contactNo) {
        return new Teacher(null, null, employeeNo, fullName, email, contactNo);
    }
}
