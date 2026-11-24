package com.model;

/** Headline numbers for one day (the overview cards on the dashboards). */
public record DailyCounts(int enrolled, int present, int late, int absent, int excused) {

    /** Percentage of enrolled students who are present or late (0 when nobody is enrolled). */
    public double attendanceRate() {
        return enrolled == 0 ? 0.0 : (present + late) * 100.0 / enrolled;
    }
}
