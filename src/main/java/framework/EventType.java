package framework;

public enum EventType {
    SCORECHANGE(1),     //param1 = home score, param2 = away score, param3 = goalscorer, param4 = assisting scorer
    CURRENT_PERIOD(2),  //param1 = periodId
    HALFTIME_SCORE(3),  //param1 = home score, param2 = away score
    FULLTIME_SCORE(4),  //param1 = home score, param2 = away score
    OVERTIME_SCORE(5),  //param1 = home score, param2 = away score
    YELLOW_CARD(6),     //param1 = player name, param2 = team
    RED_CARD(7),        //param1 = player name, param2 = team
    SUBSTITUTION(8),    //param1 = player subbed in, param2 = player subbed out, param3 = team
    LINEUP(9),          //param1 = player name, param2 = team, param3 = starting lineup yes/no
    PERIOD_SCORE(10);   //param1 = periodId, param2 = home score, param3 = away score

    final int id;

    EventType(int id) {
        this.id = id;
    }
}
