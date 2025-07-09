package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class UAEProCrawler extends CrawlerInterface {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final List<LiveScoreObject> LIVESCOREOBJECT_LIST = new ArrayList<>();

    static final Connection SESSION = Jsoup.newSession();

    static final String DATEFORMAT = "EEE dd MMM yyyy HH:mm";

    static final String TIMEZONE = "Asia/Dubai";

    static final String TIMEZONE_DEF = "Etc/UTC+1";

    static final String BASE_URL = "https://www.uaeproleague.ae/en/fixtures/matches?";

    static String CATEGORY;

    static final Map<String, String> LEAGUE_MAP = Map.ofEntries(
            Map.entry("ADNOC.Pro.League", "seasonCompetitionId=4191a490-3c62-11ef-9586-1d1cfcc58710&weekNumber=&teamId="),
            Map.entry("ADIB.Cup", "seasonCompetitionId=144e8160-3c63-11ef-af8c-43946313a4d9&weekNumber=&teamId="),
            Map.entry("Pro.League.U23", "seasonCompetitionId=b7e08b70-3c67-11ef-b3b5-d15903e9a4e4&weekNumber=&teamId="),
            Map.entry("EMAAR.Super.Cup", "seasonCompetitionId=bec6c6f0-a401-11ef-984b-71c9d3bffab8&weekNumber=&teamId=")
    );


    @Override
    public void downLoad() {
        try {
            LEAGUE_MAP.forEach((category, url) -> {
                CATEGORY = category;
                Document document = getDocument(BASE_URL + url);
                if (document != null) {
                    parseDocument(document);
                }
            });
            debug("Found Fixtures: ", LIVESCOREOBJECT_LIST.size());
            LIVESCOREOBJECT_LIST.forEach(fixture -> {
                debug(fixture.toString());
                fixture.printEvents();
            });
        } catch (Exception e) {
            debug("downLoad: ", e.getMessage());
        }
    }
    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("div[class='upcomingMatches__box']");
            matches.forEach(UAEProCrawler::parseMatch);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }

    private static void parseMatch(Element match) {
        try {
            if (!match.select("span[class='upcomingMatches__time-info-num']").text().equalsIgnoreCase("TBC")) {
                String homeTeam = match.select("span[class='upcomingMatches__club-name']").text();
                String awayTeam = match.select("span[class*='upcomingMatches__club-name--1']").text();
                String date = match.select("time span:eq(2)").text();
                LiveScoreObject liveScoreObject = (LiveScoreObject) new LiveScoreObject()
                        .setHomeTeam(homeTeam)
                        .setAwayTeam(awayTeam)
                        .setCategory(CATEGORY);
                if (match.select("span[class='upcomingMatches__time-top']").text().equalsIgnoreCase("FT")) {
                    String time = LocalTime.now(Clock.systemUTC()).toString();
                    liveScoreObject.setDate(date + " " + time, DATEFORMAT, TIMEZONE_DEF);
                    String[] scores = match.select("span[class='upcomingMatches__score']").text().split(":");
                    String homeScore = scores[0].trim();
                    String awayScore = scores[1].trim();
                    LiveEvent fTScore = new LiveEvent()
                            .setEventType(EventType.FULLTIME_SCORE)
                            .setParam1(homeScore)
                            .setParam2(awayScore);
                    liveScoreObject.addLiveEvent(fTScore);
                    LIVESCOREOBJECT_LIST.add(liveScoreObject);
                } else {
                    String time = match.select("span[class='upcomingMatches__time-info-num']").text();
                    liveScoreObject.setDate(date + " " + time, DATEFORMAT, TIMEZONE);
                    LIVESCOREOBJECT_LIST.add(liveScoreObject);
                }
            }
        } catch (Exception e) {
            debug("parseMatch: ", e.getMessage());
        }
    }

    private static Document getDocument(String url) {
        try {
            String json = SESSION.newRequest(url).ignoreContentType(true).execute().body();
            JsonNode jsonNode = MAPPER.readTree(json);
            String html = jsonNode.get("html").asText();
            return Jsoup.parse(html);
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
