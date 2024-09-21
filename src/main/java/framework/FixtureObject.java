package framework;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FixtureObject {

    final SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    String homeTeam;
    String awayTeam;
    String category;
    Date date;

    public FixtureObject(String homeTeam, String awayTeam, String category, String date, String dateFormat, String timeZone) throws ParseException {
        this.setHomeTeam(homeTeam)
                .setAwayTeam(awayTeam)
                .setCategory(category)
                .setDate(date, dateFormat, timeZone);
    }
    public FixtureObject() {

    }

    public FixtureObject setDate(String date, String dateFormat, String timeZone) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        this.date = simpleDateFormat.parse(date);
        return this;
    }

    public String toString() {
        return "Category: " + getCategory() + " | Date: " + outputFormat.format(getDate()) + " | Home: " + getHomeTeam() + " | Away: " + getAwayTeam();
    }
}
