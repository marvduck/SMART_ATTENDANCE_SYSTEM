package com.model;

/** Daily dashboard totals. */
public record DailyCounts(int enrolled, int present, int late, int absent, int excused) {

    /** Present-or-late percentage of enrolled students; 0 when none are enrolled. */
    public double attendanceRate() {
        return enrolled == 0 ? 0.0 : (present + late) * 100.0 / enrolled;
    }
}
