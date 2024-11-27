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

import java.text.DateFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BetclicCrawler extends CrawlerInterface {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String CATEGORY = "Betclic.Élite";

    static final List<LiveScoreObject> FIXTURE_LIST = new ArrayList<>();

    static final DateFormat DF = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);

    static final String API_URL = "https://www.lnb.fr/elite/wp-admin/admin-ajax.php";

    static final OkHttpClient CLIENT = new OkHttpClient();

    @Override
    public void downLoad() {
        try {
            Document document = getDocument(API_URL);
            if (document != null) {
                parseMatches(document);
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
           if (match.select("button").text().equalsIgnoreCase("Game Center")) {
               String url = match.select("div[class*='ticketing'] > a").attr("href");
               String time = LocalTime.now(Clock.systemUTC()).toString();
               Date dateTime = DF.parse(date + " " + time);
               liveScoreObject.setDate(dateTime);
               String scoreHome = match.select("div[class*='hour-score'] div:eq(0) > a").text();
               String scoreAway = match.select("div[class*='hour-score'] div:eq(1) > a").text();
               LiveEvent event = new LiveEvent()
                       .setEventType(EventType.FULLTIME_SCORE)
                       .setParam1(scoreHome)
                       .setParam2(scoreAway);
               liveScoreObject.addLiveEvent(event);
               getTeamComp(liveScoreObject, url);
               FIXTURE_LIST.add(liveScoreObject);
           //} else if (match.select("button").text().equalsIgnoreCase("LIVE")) {

           } else {
               String time = match.select("div[class*='hour-score']").text();
               Date dateTime = DF.parse(date + " " + time);
               liveScoreObject.setDate(dateTime);
               FIXTURE_LIST.add(liveScoreObject);
           }
       } catch (Exception e) {
           debug("parseMatch: ", e.getMessage());
       }
    }
    private static void getTeamComp(LiveScoreObject liveScoreObject, String url) {
        try {
            Document document = getDocument(url);
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
            if (url.equalsIgnoreCase("https://www.lnb.fr/elite/wp-admin/admin-ajax.php")) {
                RequestBody formBody = new FormBody.Builder()
                        .add("action", "get_calendar_html")
                        .add("season", "2024")
                        .add("team", "all")
                        .add("date", "all")
                        .add("screen_size", "1376")
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();
                Response response = CLIENT.newCall(request).execute();
                String json = response.body().string();
                JsonNode jsonNode = MAPPER.readTree(json);
                String html = jsonNode.get("html").asText();
                return Jsoup.parse(html);
            } else {
                return Jsoup.connect(url).get();
            }
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }
}
