package com.model;

import java.time.LocalDateTime;

/** studentId and studentName are null for unregistered cards. */
public record ScanLog(Long logId, String cardUid, Integer studentId, LocalDateTime scanTime,
                      ScanResult result, String studentName) {
}
