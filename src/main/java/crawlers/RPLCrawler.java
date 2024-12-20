package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RPLCrawler extends CrawlerInterface {

    static final List<LiveScoreObject> LIVESCOREOBJECT_LIST = new ArrayList<>();

    static final String CATEGORY = "Russian.Premier.League";

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    static final String TIMEZONE = "Europe/Moscow";

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String API_URL = "https://eng.premierliga.ru/ajax/match/";

    @Override
    public void downLoad() {
       try {
           JsonNode document = getDocument();
           if (document != null) {
               parseDocument(document);
           }
           debug("Found Fixtures: ", LIVESCOREOBJECT_LIST.size());
           LIVESCOREOBJECT_LIST.forEach(fixture -> {
               debug(fixture.toString());
               fixture.printEvents();
           });
       } catch (Exception e) {
           debug("downLoad: ", e.getMessage());
       }
    }

    private static void parseDocument(JsonNode document) {
        try {
            JsonNode matches = document.get("contents");
            matches.forEach(RPLCrawler::parseMatch);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }

    private static void parseMatch(JsonNode match) {
        try {
            String homeTeam = match.get("name1").asText();
            String awayTeam = match.get("name2").asText();
            String date = match.get("date").asText();
            LiveScoreObject liveScoreObject = (LiveScoreObject) new LiveScoreObject()
                    .setHomeTeam(homeTeam)
                    .setAwayTeam(awayTeam)
                    .setCategory(CATEGORY)
                    .setDate(date, DATEFORMAT, TIMEZONE);
            if (match.get("dopClass").asText().trim().equalsIgnoreCase("final")) {
                String scoreHome = match.get("goal1").asText();
                String scoreAway = match.get("goal2").asText();
                LiveEvent ftScore = new LiveEvent()
                        .setEventType(EventType.FULLTIME_SCORE)
                        .setParam1(scoreHome)
                        .setParam2(scoreAway);
                liveScoreObject.addLiveEvent(ftScore);
                LIVESCOREOBJECT_LIST.add(liveScoreObject);
            } else {
                LIVESCOREOBJECT_LIST.add(liveScoreObject);
            }
        } catch (Exception e) {
            debug("parseMatch: ", e.getMessage());
        }
    }

    private static JsonNode getDocument() {
        try {
            String json = Jsoup.connect(API_URL).ignoreContentType(true).data("ajaxAction", "getHeaderCalendar", "tournament", "1").post().text();
            return MAPPER.readTree(json);
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
