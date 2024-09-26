package crawlers;

import framework.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SoccerwayCrawler extends CrawlerInterface {

    static final List<LiveScoreObject> FIXTURE_LIST = new ArrayList<>();

    static final String CATEGORY = "Soccer.Finland.Kolomonen.Pohjoinen";

    static final String TIMEZONE = "Europe/Helsinki";

    static final String DATEFORMAT = "dd/MM/yyyy HH:mm";

    static final String API_URL = "https://de.soccerway.com/national/finland/kolmonen/2024/pohjoinen/r79202/";

    static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    static String date;

    @Override
    public void downLoad() {
        try {
            Document document = getDocument();
            if (document != null) {
                parseDocument(document);
            }
            debug("Found Fixtures: ", FIXTURE_LIST.size());
            FIXTURE_LIST.forEach(fixture -> {
                debug(fixture.toString());
                fixture.printEvents();
            });
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
            String home = match.select("td[class*='team team-a']").text();
            String away = match.select("td[class*='team team-b']").text();
            LiveScoreObject matchObject = (LiveScoreObject) new LiveScoreObject()
                    .setHomeTeam(home)
                    .setAwayTeam(away)
                    .setCategory(CATEGORY);
            if (match.attr("data-status").equalsIgnoreCase("played")) {
                String localTime = LocalTime.now(Clock.systemUTC()).toString();
                matchObject.setDate(date + " " + localTime, DATEFORMAT, TIMEZONE);
                String[] Scores = match.select("span.extra_time_score").text().split("-");
                String scoreHome = Scores[1].trim();
                String scoreAway = Scores[0].trim();
                LiveEvent event = new LiveEvent()
                        .setEventType(EventType.FULLTIME_SCORE)
                        .setParam1(scoreHome)
                        .setParam2(scoreAway);
                matchObject.addLiveEvent(event);
            } else {
                String time = match.select("div[class='match-card match-hour']").text();
                matchObject.setDate(date + " " + time, DATEFORMAT, TIMEZONE);
            }
            FIXTURE_LIST.add(matchObject);
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
