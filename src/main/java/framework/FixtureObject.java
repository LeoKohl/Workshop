package framework;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class FixtureObject {

    private String homeTeam;
    private String awayTeam;
    private String category;
    private Date date;
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private String score;

    public FixtureObject(String homeTeam, String awayTeam, String category, String date, String dateFormat, String timeZone) throws ParseException {
        this.setHomeTeam(homeTeam)
                .setAwayTeam(awayTeam)
                .setCategory(category)
                .setDate(date, dateFormat, timeZone);
    }
    public FixtureObject() {

    }

    public FixtureObject setAwayTeam(String awayTeam) {
        this.awayTeam = awayTeam;
        return this;
    }

    public FixtureObject setHomeTeam(String homeTeam) {
        this.homeTeam = homeTeam;
        return this;
    }

    public FixtureObject setCategory(String category) {
        this.category = category;
        return this;
    }

    public FixtureObject setDate(String date, String dateFormat, String timeZone) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        this.date = simpleDateFormat.parse(date);
        return this;
    }
    public FixtureObject setScore(String score) {
        this.score = score;
        return this;
    }

    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public String getCategory() {
        return category;
    }

    public Date getDate() { return date; }

    public String getScore(){return score;}

    public String toString() {
        return "Category: " + getCategory() + " | Date: " + outputFormat.format(getDate()) + " | Home: " + getHomeTeam() + " | Away: " + getAwayTeam() + " | Score: " + getScore();
    }
}
