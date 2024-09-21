package crawlers;

import framework.CrawlerInterface;
import framework.FixtureObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Array;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoccerwayCrawler extends CrawlerInterface {

    private static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    private static final String CATEGORY = "Soccer.Finland.Kolomonen.Pohjoinen";

    private static final String TIMEZONE = "Europe/Helsinki";

    private static final String DATEFORMAT = "dd/MM/yyyy HH:mm";

    private static final String API_URL = "https://de.soccerway.com/national/finland/kolmonen/2024/pohjoinen/r79202/";

    private static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    private static String date;

    @Override
    public void downLoad() {
        try {
            Document document = getDocument();
            if (document != null) {
                parseDocument(document);
            }
            debug("Found Fixtures: ", FIXTURE_LIST.size());
            FIXTURE_LIST.forEach(fixture -> debug(fixture.toString()));
        } catch (Exception e) {
            debug("downLoad: ", e.getMessage());
        }
    }

    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("table.matches tbody tr");
            matches.forEach(match -> {
                if (match.hasClass("no-date-repetition-new")) {
                    date = match.select("td.date").text().replaceAll("(?:\\w+\\s)?", "");
                } else parseMatch(match);
            });
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }

    private static void parseMatch (Element match) {
        try {
            String home = match.select("td[class='team team-a']").text();
            String away = match.select("td[class='team team-b']").text();
            String localTime = LocalTime.now(Clock.systemUTC()).toString();
            if (match.attr("data-status").equalsIgnoreCase("played")) {
                String[] Scores = match.select("span.extra_time_score").text().split("-");
                String scoreHome = Scores[1].trim();
                String scoreAway = Scores[0].trim();
                FixtureObject fixture = new FixtureObject()
                        .setHomeTeam(home)
                        .setAwayTeam(away)
                        .setCategory(CATEGORY)
                        .setDate(date + " " + localTime, DATEFORMAT, TIMEZONE)
                        .setScore(scoreHome + ":" + scoreAway);
                FIXTURE_LIST.add(fixture);
            } else {
                String time = match.select("div[class='match-card match-hour']").text();
                FixtureObject fixture = new FixtureObject()
                        .setHomeTeam(home)
                        .setAwayTeam(away)
                        .setCategory(CATEGORY)
                        .setDate(date + " " + time, DATEFORMAT, TIMEZONE);
                FIXTURE_LIST.add(fixture);
            }


        } catch (Exception e) {
            debug("parseMatch: ", e.getMessage());
        }
    }

    private static Document getDocument() {
        try {
            return Jsoup.connect(API_URL).userAgent(DEFAULT_UA).get();
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
