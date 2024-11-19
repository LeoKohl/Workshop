package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BetclicCrawler extends CrawlerInterface {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String CATEGORY = "Betclic.Élite";

    static final List<LiveScoreObject> LIVE_SCORE_OBJECT_LIST = new ArrayList<>();

    static final String TIMEZONE = "Europe/Paris";

    static final String DATEFORMAT = "dd MMMM yyyy HH:mm";

    static final String API_URL = "https://www.lnb.fr/elite/wp-admin/admin-ajax.php";

    static final OkHttpClient CLIENT = new OkHttpClient();

    //static String date;

    @Override
    public void downLoad() {
        try {
            Document document = getDocument(API_URL);
            if (document != null) {
                System.out.println(document);
            }
        } catch (Exception e) {
            debug("downLoad: ", e.getMessage());
        }
    }
    private static void parseMatches(Document document) {
        try {
            Elements gameDays = document.select("div[class='game-day']");
            gameDays.forEach(gameDay -> {
                String date = gameDay.select("div.date").text();
                Elements matches = gameDay.select("div.game");
                matches.forEach(match -> parseMatch(match, date));
            });
        } catch (Exception e) {
            debug("parseMatches: ", e.getMessage());
        }
    }
    private static void parseMatch(Element match, String date) {
       try {
           String homeTeam = match.select("div[class*='home-team'] > a").text();
           String awayTeam = match.select("div[class*='road-team'] > a").text();
           LiveScoreObject liveScoreObject = (LiveScoreObject) new LiveScoreObject()
                   .setHomeTeam(homeTeam)
                   .setAwayTeam(awayTeam)
                   .setCategory(CATEGORY);
           if (match.attr("btn1").equalsIgnoreCase("Game Center")) {
               String url = match.select("div[class*='ticketing'] > a").attr("href");
               String localTime = LocalTime.now(Clock.systemUTC()).toString();
               liveScoreObject.setDate(date + " " + localTime, DATEFORMAT, TIMEZONE);
               String scoreHome = match.select("div[class*='hour-score'] div:eq(0) > a").text();
               String scoreAway = match.select("div[class*='hour-score'] div:eq(1) > a").text();
               LiveEvent event = new LiveEvent()
                       .setEventType(EventType.FULLTIME_SCORE)
                       .setParam1(scoreHome)
                       .setParam2(scoreAway);
               liveScoreObject.addLiveEvent(event);
               getTeamComp(liveScoreObject, url);
               LIVE_SCORE_OBJECT_LIST.add(liveScoreObject);
           } else if (match.attr("btn1").equalsIgnoreCase("LIVE")) {

           } else {

           }
       } catch (Exception e) {
           debug("parseMatch: ", e.getMessage());
       }
    }
    private static void getTeamComp(LiveScoreObject liveScoreObject, String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements teams = document.select("table[class='boxscore-tab']");
            teams.forEach(team -> {
                String teamName = team.select("div[class='boxscore-team-name'] > a").text();
                Elements players = team.select("table[class='boxscore-tab'] tr[class='main-background-color'], [class='second-background-color']");
                players.forEach(player -> {
                    String firstName = player.select("span[class='first-name']").text();
                    String lastName = player.select("span[class='last-name']").text();
                    LiveEvent event = new LiveEvent()
                            .setEventType(EventType.LINEUP)
                            .setParam1(firstName + " " + lastName)
                            .setParam2(teamName);
                    liveScoreObject.addLiveEvent(event);
                });
            });
        } catch (Exception e) {
            debug("getTeamComp: ", e.getMessage());
        }
    }

    private static Document getDocument(String url) {
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("action", "get_calendar_html")
                    .add("season", "2024")
                    .add("team", "all")
                    .add("date", "all")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            Response response = CLIENT.newCall(request).execute();
            String json = response.body().string();
           //String json = Jsoup.connect(url).data("action", "get_calendar_html", "season", "2024", "team", "all", "date", "all").post().body().text();
           JsonNode jsonNode = MAPPER.readTree(json);
           String html = jsonNode.get("html").asText();
           return Jsoup.parse(html);
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
