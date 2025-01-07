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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BetclicCrawler extends CrawlerInterface {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static final String CATEGORY = "Betclic.Élite";

    static final List<LiveScoreObject> FIXTURE_LIST = new ArrayList<>();

    static final DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.FRANCE);

    static final String API_URL = "https://www.lnb.fr/elite/wp-admin/admin-ajax.php";

    static final String BASE_LIVE_URL = "https://eapi.web.prod.cloud.atriumsports.com/v1/embed/6/fixture_detail?state=";

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
            Document document = getDocument(formBody);
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
                /*String url = match.select("div[class*='ticketing'] > a").attr("href");
                String gameId = getGameId(url);
                RequestBody formBody = new FormBody.Builder()
                        .add("action", "get_slider_section_games_home_page")
                        .add("game_id", gameId)
                        .build();
                Document document = getDocument(formBody);
                if (document != null) {*/
//                    String bs_url = BASE_LIVE_URL + document.select("div[class='sw-sub-tabs'] a:eq(0)").attr("href").replace("\\?%7Ew=f%7E", "")
//                    String pbp_url = BASE_LIVE_URL +  document.select("div[class='sw-sub-tabs'] a:eq(1)").attr("href").replace("\\?%7Ew=f%7E", "");
                    String bs_url = "https://eapi.web.prod.cloud.atriumsports.com/v1/embed/6/fixture_detail?state=eJwljLENwzAMBFcxWIeFKNIOM0AGyAakbAIGXFmqEmT3mEj3dzj8Bzo8JmjmFEozUphjKVvg3VVxE6mNV5mdGG4THBnHic9X0jupDxt7H3vrqSJVJQsSLsi12v_Nmgcu11KWhVdW-P4Az54gVA";
                    String pbp_url = "https://eapi.web.prod.cloud.atriumsports.com/v1/embed/6/fixture_detail?state=eJwlzLENg0AMheFVTq7j4nw2xAyQAdjAPnBFcSJdInZPLLrvl57eF96wFOjmFEoTUphjrXvg01VxF2mdN5mcGB4FjhzHia8165M1fKQj3ciChCtya3bfWPfA-S9lmXljhesHuOodSw";
                    JsonNode bs = getJson(bs_url);
                    JsonNode pbp = getJson(pbp_url);
                    List<JsonNode> periods = pbp.at("/data/pbp").findParents("labels");
                    String homeId = pbp.at("/data/fixture/competitors/0/entityId").asText();
                    String awayId = pbp.at("/data/fixture/competitors/1/entityId").asText();
                    parseScoreChange(liveScoreObject, periods, homeId, awayId);
                    parsePeriod(pbp, liveScoreObject, periods, homeId,awayId);
                    parseTeams(bs, liveScoreObject, homeTeam, awayTeam);
                    FIXTURE_LIST.add(liveScoreObject);
                //}
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
            LiveEvent period = new LiveEvent();
            switch (periodLabel) {
                case "Q4":
                    period
                            .setEventType(EventType.CURRENT_PERIOD)
                            .setParam1(Period.FOURTH_PERIOD.name());
                    liveScoreObject.addLiveEvent(period);
                    parsePeriodScore(pbp, liveScoreObject, periodLabel, homeId, awayId);
                    break;
                case "Q3":
                    period
                            .setEventType(EventType.CURRENT_PERIOD)
                            .setParam1(Period.THIRD_PERIOD.name());
                    liveScoreObject.addLiveEvent(period);
                    parsePeriodScore(pbp, liveScoreObject, periodLabel, homeId, awayId);
                    break;
                case "Q2":
                    period
                            .setEventType(EventType.CURRENT_PERIOD)
                            .setParam1(Period.SECOND_PERIOD.name());
                    liveScoreObject.addLiveEvent(period);
                    parsePeriodScore(pbp, liveScoreObject, periodLabel, homeId, awayId);
                    break;
                case "Q1":
                    period
                            .setEventType(EventType.CURRENT_PERIOD)
                            .setParam1(Period.FIRST_PERIOD.name());
                    liveScoreObject.addLiveEvent(period);
                    parsePeriodScore(pbp, liveScoreObject, periodLabel, homeId, awayId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            debug("parsePeriod: ", e.getMessage());
        }
    }

    private static void parsePeriodScore(JsonNode pbp, LiveScoreObject liveScoreObject, String periodLabel, String homeId, String awayId) {
        try {
            String homeScore;
            String awayScore;
            switch (periodLabel) {
                case "Q4":
                    homeScore = pbp.at("/data/periodData/teamScores/" + homeId +"/3/score").asText();
                    awayScore = pbp.at("/data/periodData/teamScores/" + awayId +"/3/score").asText();
                    LiveEvent periodScore4 = new LiveEvent()
                            .setEventType(EventType.PERIOD_SCORE)
                            .setParam1(Period.FOURTH_PERIOD.name())
                            .setParam2(homeScore)
                            .setParam3(awayScore);
                    liveScoreObject.addLiveEvent(periodScore4);
                case "Q3":
                    homeScore = pbp.at("/data/periodData/teamScores/" + homeId +"/2/score").asText();
                    awayScore = pbp.at("/data/periodData/teamScores/" + awayId +"/2/score").asText();
                    LiveEvent periodScore3 = new LiveEvent()
                            .setEventType(EventType.PERIOD_SCORE)
                            .setParam1(Period.THIRD_PERIOD.name())
                            .setParam2(homeScore)
                            .setParam3(awayScore);
                    liveScoreObject.addLiveEvent(periodScore3);
                case "Q2":
                    homeScore = pbp.at("/data/periodData/teamScores/" + homeId +"/1/score").asText();
                    awayScore = pbp.at("/data/periodData/teamScores/" + awayId +"/1/score").asText();
                    LiveEvent periodScore2 = new LiveEvent()
                            .setEventType(EventType.PERIOD_SCORE)
                            .setParam1(Period.SECOND_PERIOD.name())
                            .setParam2(homeScore)
                            .setParam3(awayScore);
                    liveScoreObject.addLiveEvent(periodScore2);
                case "Q1":
                    homeScore = pbp.at("/data/periodData/teamScores/" + homeId +"/0/score").asText();
                    awayScore = pbp.at("/data/periodData/teamScores/" + awayId +"/0/score").asText();
                    LiveEvent periodScore1 = new LiveEvent()
                            .setEventType(EventType.PERIOD_SCORE)
                            .setParam1(Period.FIRST_PERIOD.name())
                            .setParam2(homeScore)
                            .setParam3(awayScore);
                    liveScoreObject.addLiveEvent(periodScore1);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            debug("parsePeriodScore: ", e.getMessage());
        }
    }

    private static String getGameId (String url) {
        try {
            Pattern pattern = Pattern.compile("id=(\\d+)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            debug("getGameId: ", e.getMessage());
        } return null;
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
            if(player.get("active").asBoolean()) {
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

    private static JsonNode getJson(String url) {
        try {
            String json = Jsoup.connect(url).ignoreContentType(true).get().body().text();
            return MAPPER.readTree(json);
        } catch (Exception e) {
            debug("getJsonNode: ", e.getMessage());
        }
        return null;
    }
}

