package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FieldDefaults(level=AccessLevel.PRIVATE)
public class EuroLeagueBasketballCrawler extends CrawlerInterface {

    static final List<LiveScoreObject> LIVE_SCORE_OBJECT_LIST = new ArrayList<>();

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String BASE_URL = "https://feeds.incrowdsports.com/provider/euroleague-feeds/v2/competitions/E/seasons/E2024/games?teamCode=&phaseTypeCode=RS&roundNumber=";

    static String CATEGORY;

    static final String TIMEZONE = "Etc/UTC-1";

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm";

    static final Map<String,String> LEAGUE_MAP = Map.ofEntries(
            Map.entry("Euro.League.Basketball.Regular.Season.Round.1", "1"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.2", "2"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.3", "3"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.4", "4"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.5", "5"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.6", "6"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.7", "7"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.8", "8"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.9", "9"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.10", "10"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.11", "11"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.12", "12"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.13", "13"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.14", "14"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.15", "15"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.16", "16"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.17", "17"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.18", "18"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.19", "19"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.20", "20"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.21", "21"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.22", "22"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.23", "23"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.24", "24"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.25", "25"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.26", "26"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.27", "27"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.28", "28"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.29", "29"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.30", "30"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.31", "31"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.32", "32"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.33", "33"),
            Map.entry("Euro.League.Basketball.Regular.Season.Round.34", "34")
    );

    @Override
    public void downLoad() {
       try {
           LEAGUE_MAP.forEach((category, url) -> {
               CATEGORY = category;
               JsonNode document = getDocument(BASE_URL + url);
               if (document != null) {
                   parseDocument(document);
               }
           });
           debug("Found Fixtures: ", LIVE_SCORE_OBJECT_LIST.size());
           LIVE_SCORE_OBJECT_LIST.forEach(fixture -> {
               debug(fixture.toString());
               fixture.printEvents();
           });
       } catch (Exception e) {
           debug("downLoad: ", e.getMessage());
       }
    }
    private static void parseDocument(JsonNode document) {
        try {
            JsonNode matches = document.get("data");
            matches.forEach(EuroLeagueBasketballCrawler::parseMatch);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }
    private static void parseMatch (JsonNode match) {
        try {
           String homeTeam = match.at("/home/name").asText();
           String awayTeam = match.at("/away/name").asText();
           String date = match.at("/date").asText().replace("T"," ");
           LiveScoreObject matchObject = (LiveScoreObject) new LiveScoreObject()
                   .setHomeTeam(homeTeam)
                   .setAwayTeam(awayTeam)
                   .setCategory(CATEGORY)
                   .setDate(date, DATEFORMAT, TIMEZONE);
           if (match.at("/status").asText().equalsIgnoreCase("result")) {
               String homeScore = match.at("/home/score").asText();
               String awayScore = match.at("/away/score").asText();
               LiveEvent event = new LiveEvent()
                       .setEventType(EventType.FULLTIME_SCORE)
                       .setParam1(homeScore)
                       .setParam2(awayScore);
               matchObject.addLiveEvent(event);
               LIVE_SCORE_OBJECT_LIST.add(matchObject);
           } else {
               LIVE_SCORE_OBJECT_LIST.add(matchObject);
           }
        } catch (Exception e) {
            debug("parseMatches: ", e.getMessage());
        }
    }
    private static JsonNode getDocument(String url) {
        try {
            return MAPPER.readTree(URI.create(url).toURL());
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
