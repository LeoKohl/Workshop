package framework;

public enum Period {
    FIRST_PERIOD("1"),
    SECOND_PERIOD("2"),
    THIRD_PERIOD("3"),
    FOURTH_PERIOD("4"),
    FIRST_INTERMISSION("31"),
    SECOND_INTERMISSION("32"),
    THIRD_INTERMISSION("33"),
    OVERTIME_PERIOD("51"),
    PENALTY_SHOOTOUT("52");

    final String periodId;


    Period(String periodId) {
        this.periodId = periodId;
    }
}
