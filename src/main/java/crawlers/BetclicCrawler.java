package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.EventType;
import framework.LiveEvent;
import framework.LiveScoreObject;
import framework.Period;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.time.*;
import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BetclicCrawler extends CrawlerInterface {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String CATEGORY = "Betclic.Élite";

    static final List<LiveScoreObject> FIXTURE_LIST = new ArrayList<>();

    static final DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.FRANCE);

    static final String API_URL = "https://www.lnb.fr/elite/wp-admin/admin-ajax.php";

    static final String BASE_LIVE_URL = "https://eapi.web.prod.cloud.atriumsports.com/v1/embed/6/fixtures/";

    static final String REQUESTBODY = "action=get_calendar_html&season=2024&team=all&date=all&screen_size=1376";

    static final OkHttpClient CLIENT = new OkHttpClient();

    @Override
    public void downLoad() {
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("action", "get_calendar_html")
                    .add("season", "2024")
                    .add("team", "all")
                    .add("date", "all")
                    .add("screen_size", "1376")
                    .build();
            Document document2 = getDocument(formBody);
            String json = getDocument(API_URL, REQUESTBODY);
            String html = parseJson(json).get("html").asText();
            Document document = parseDocument(html);
            if (document2 != null) {
                parseMatches(document2);
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
            String localTime = LocalTime.now(ZoneId.of("Europe/Paris")).toString();
            LiveScoreObject liveScoreObject = (LiveScoreObject) new LiveScoreObject()
                    .setHomeTeam(homeTeam)
                    .setAwayTeam(awayTeam)
                    .setCategory(CATEGORY);
            if (match.select("button").text().equalsIgnoreCase("Game Center")) {
                Date dateTime = DF.parse(date + " " + localTime);
                liveScoreObject.setDate(dateTime);
                String scoreHome = match.select("div[class*='hour-score'] div:eq(0) > a").text();
                String scoreAway = match.select("div[class*='hour-score'] div:eq(1) > a").text();
                LiveEvent event = new LiveEvent()
                        .setEventType(EventType.FULLTIME_SCORE)
                        .setParam1(scoreHome)
                        .setParam2(scoreAway);
                liveScoreObject.addLiveEvent(event);
                FIXTURE_LIST.add(liveScoreObject);
            } else if (match.select("button").text().equalsIgnoreCase("LIVE")) {
                Date dateTime = DF.parse(date + " " + localTime);
                liveScoreObject.setDate(dateTime);
                String liveGameURL = match.select("div[class*='ticketing'] a").attr("href");
                Document document = parseDocument(getDocument(liveGameURL, null));
                String fixtureId = document.select("div[class='widget'] script").attr("data-fixture-id");
                String bs_url = BASE_LIVE_URL + fixtureId + "/statistics?sub=statistics";
                String pbp_url = BASE_LIVE_URL + fixtureId + "/statistics?sub=pbp";
                String bs_json = parseDocument(getDocument(bs_url, null)).body().text();
                var pbp_json = parseDocument(getDocument(pbp_url, null)).body().text();
                JsonNode bs = parseJson(bs_json);
                JsonNode pbp = parseJson(pbp_json);
                List<JsonNode> periods = pbp.at("/data/pbp").findParents("labels");
                String homeId = pbp.at("/data/fixture/competitors/0/entityId").asText();
                String awayId = pbp.at("/data/fixture/competitors/1/entityId").asText();
                parseScoreChange(liveScoreObject, periods, homeId, awayId);
                parsePeriod(pbp, liveScoreObject, periods, homeId, awayId);
                parseTeams(bs, liveScoreObject, homeTeam, awayTeam);
                FIXTURE_LIST.add(liveScoreObject);
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

    private static void parseScoreChange(LiveScoreObject liveScoreObject, List<JsonNode> periods, String homeId, String awayId) {
        try {
            JsonNode currentPeriod = periods.getLast();
            List<JsonNode> events = currentPeriod.get("events").findParents("success").reversed();
            Optional<JsonNode> sce = events.stream().filter(c -> c.get("success").asBoolean()).findFirst();
            String homeScore = sce.get().at("/scores/" + homeId).asText();
            String awayScore = sce.get().at("/scores/" + awayId).asText();
            String goalscorer = sce.get().get("name").asText();
            LiveEvent scoreChange = new LiveEvent()
                    .setEventType(EventType.SCORECHANGE)
                    .setParam1(homeScore)
                    .setParam2(awayScore)
                    .setParam3(goalscorer);
            liveScoreObject.addLiveEvent(scoreChange);
        } catch (Exception e) {
            debug("parseScoreChange: ", e.getMessage());
        }
    }

    private static void parsePeriod(JsonNode pbp, LiveScoreObject liveScoreObject, List<JsonNode> periods, String homeId, String awayId) {
        try {
            JsonNode currentPeriod = periods.getLast();
            String periodLabel = currentPeriod.at("/labels/shortLabel").asText();
            LiveEvent period = new LiveEvent().setEventType(EventType.CURRENT_PERIOD);
            switch (periodLabel) {
                case "Q4":
                    period.setParam1(Period.FOURTH_PERIOD.name());
                    break;
                case "Q3":
                    period.setParam1(Period.THIRD_PERIOD.name());
                    break;
                case "Q2":
                    period.setParam1(Period.SECOND_PERIOD.name());
                    break;
                case "Q1":
                    period.setParam1(Period.FIRST_PERIOD.name());
                    break;
                default:
                    break;
            }
            liveScoreObject.addLiveEvent(period);
            parsePeriodScore(pbp, liveScoreObject, periodLabel, homeId, awayId);
        } catch (Exception e) {
            debug("parsePeriod: ", e.getMessage());
        }
    }

    private static void parsePeriodScore(JsonNode pbp, LiveScoreObject liveScoreObject, String periodLabel, String homeId, String awayId) {
        try {
            switch (periodLabel) {
                case "Q4":
                    addPeriodScore(homeId, awayId, "3", Period.FOURTH_PERIOD, pbp, liveScoreObject);
                case "Q3":
                    addPeriodScore(homeId, awayId, "2", Period.THIRD_PERIOD, pbp, liveScoreObject);
                case "Q2":
                    addPeriodScore(homeId, awayId, "1", Period.SECOND_PERIOD, pbp, liveScoreObject);
                case "Q1":
                    addPeriodScore(homeId, awayId, "0", Period.FIRST_PERIOD, pbp, liveScoreObject);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            debug("parsePeriodScore: ", e.getMessage());
        }
    }

    private static void addPeriodScore(String homeId, String awayId, String periodPath, Period period, JsonNode pbp, LiveScoreObject liveScoreObject) {
        try {
            var homeScore = pbp.at("/data/periodData/teamScores/" + homeId + "/" + periodPath + "/score").asText();
            var awayScore = pbp.at("/data/periodData/teamScores/" + awayId + "/" + periodPath + "/score").asText();
            LiveEvent periodScore1 = new LiveEvent()
                    .setEventType(EventType.PERIOD_SCORE)
                    .setParam1(period.name())
                    .setParam2(homeScore)
                    .setParam3(awayScore);
            liveScoreObject.addLiveEvent(periodScore1);
        } catch (Exception e) {
            debug("addPeriodScore", e.getMessage());
        }
    }

    private static void parseTeams(JsonNode bs, LiveScoreObject liveScoreObject, String homeTeam, String awayTeam) {
        try {
            JsonNode homePlayers = bs.at("/data/statistics/home/persons/0").get("rows");
            JsonNode awayPlayers = bs.at("/data/statistics/away/persons/0").get("rows");
            homePlayers.forEach(player -> parseLineUp(player, liveScoreObject, homeTeam));
            awayPlayers.forEach(player -> parseLineUp(player, liveScoreObject, awayTeam));
        } catch (Exception e) {
            debug("getLineUp: ", e.getMessage());
        }
    }

    private static void parseLineUp(JsonNode player, LiveScoreObject liveScoreObject, String team) {
        try {
            if (player.get("active").asBoolean()) {
                String playerName = player.get("personName").asText();
                LiveEvent lineUp = new LiveEvent()
                        .setEventType(EventType.LINEUP)
                        .setParam1(playerName)
                        .setParam2(team);
                if (player.get("starter").asBoolean()) {
                    lineUp.setParam3("1");
                } else {
                    lineUp.setParam3("0");
                }
                liveScoreObject.addLiveEvent(lineUp);
            }
        } catch (Exception e) {
            debug("getLineUp: ", e.getMessage());
        }
    }

    private static Document getDocument(RequestBody formBody) {
        try {
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(formBody)
                    .build();
            Response response = CLIENT.newCall(request).execute();
            String json = response.body().string();
            JsonNode jsonNode = MAPPER.readTree(json);
            String html = jsonNode.get("html").asText();
            return Jsoup.parse(html);
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }

    private static String getDocument(String url, String requestBody) {
        try {
            if (requestBody != null) {
                return Jsoup.connect(url).requestBody(REQUESTBODY).ignoreContentType(true).post().body().text();
            }
            return Jsoup.connect(url).ignoreContentType(true).get().body().toString();
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        }
        return null;
    }

    private static Document parseDocument(String document) {
        try {
            return Jsoup.parse(document);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
        return null;
    }

    private static JsonNode parseJson(String document) {
        try {
            return MAPPER.readTree(document);
        } catch (Exception e) {
            debug("parseJson: ", e.getMessage());
        }
        return null;
    }
}

