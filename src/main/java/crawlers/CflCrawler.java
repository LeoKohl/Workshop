package crawlers;

import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CflCrawler extends CrawlerInterface {

    static final String CATEGORY = "Canadian.football.league";

    static final List<LiveScoreObject> LIVE_OBJECT_LIST = new ArrayList<>();

    static final String API_URL = "https://www.cfl.ca/schedule/2024/";

    @Override
    public void downLoad() {
        try {
            Document document = getDocument();
            if (document != null) {
            parseDocument(document);
            }
            debug("Found Objects: ", LIVE_OBJECT_LIST.size());
            LIVE_OBJECT_LIST.forEach(object -> {
                debug(object.toString());
                object.printEvents();
            });
        } catch (Exception e) {
            debug("download: ", e.getMessage());
        }
    }

    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("div[class='schedule-wrapper'] li[class='week-row']");
            matches.forEach(CflCrawler::parseMatch);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }

    private static void parseMatch (Element match) {
        try {
            String home = match.select("div.matchup span.host span:eq(0)").text();
            String away = match.select("div.matchup span.visitor span.text").text();
            String timestampString = match.select("div.date-time script").toString();
            Pattern pattern = Pattern.compile("\\((\\d+)\\)");
            Matcher matcher = pattern.matcher(timestampString);
            long timestamp = 0;
            while (matcher.find()) {
                 timestamp = Long.parseLong(matcher.group(1));
            }
            Date date = new Date(timestamp * 1000);
            if (!home.equals("TBD") && !away.equals("TBD")) {
                LiveScoreObject matchObject = (LiveScoreObject) new LiveScoreObject()
                        .setHomeTeam(home)
                        .setAwayTeam(away)
                        .setCategory(CATEGORY)
                        .setDate(date);
                if (match.select("div[class='date-time'] span.status").text().equalsIgnoreCase("Final")) {
                    String scoreHome = match.select("span[class='host-score']").text();
                    String scoreAway = match.select("span[class='visitor-score']").text();
                    LiveEvent event = new LiveEvent()
                            .setEventType(EventType.FULLTIME_SCORE)
                            .setParam1(scoreHome)
                            .setParam2(scoreAway);
                    matchObject.addLiveEvent(event);
                }
                LIVE_OBJECT_LIST.add(matchObject);
            }
        } catch (Exception e) {
            debug("parseMatch: ", e.getMessage());
        }
    }

    private static Document getDocument() {
        try {
            return Jsoup.connect(API_URL).get();
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}



