package crawlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.CrawlerInterface;
import framework.FixtureObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EHWCrawler extends CrawlerInterface {

    public static final String CATEGORY = "Women's.EHF.Euro.2024";

    public static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String timeZone = "Etc/UTC";

    private static final String dateFormat = "yyyy-MM-dd HH:mm";

    private static final String API_URL = "https://ehfeuro.eurohandball.com/umbraco/Api/CompetitionMatchesApi/GetInitMatchesAsync?contentId=81611&competitionId=nn0PiBg48QhWGo65vBOCfg";

    @Override
    public void downLoad() {
        try {
            JsonNode document = getDocument();
            if (document != null) {
                parseDocument(document);
                CrawlerInterface.debug("found Fixtures", FIXTURE_LIST.size());
                FIXTURE_LIST.forEach(fixture -> CrawlerInterface.debug(fixture.toString()));
            }

        } catch (Exception e) {
            CrawlerInterface.debug("main", e.getMessage());
        }
    }
    private static void parseDocument(JsonNode document) {
        try {
            JsonNode matches = document.get("matches");
            matches.forEach(EHWCrawler::parseMatch);

        } catch (Exception e) {
            CrawlerInterface.debug("parseDocument", e.getMessage());
        }

    }
    private static void parseMatch(JsonNode match) {
        try {
            String home = match.at("/homeTeam/team/fullName").asText();
            String away = match.at("/guestTeam/team/fullName").asText();
            String date = match.at("/venue/date/utc").asText().replace("T"," ");

            FixtureObject fixture = new FixtureObject(home, away, CATEGORY, date, dateFormat, timeZone);
            FIXTURE_LIST.add(fixture);
        } catch (Exception e) {
            CrawlerInterface.debug("parseMatch", e.getMessage());
        }
    }

    private static JsonNode getDocument() {
        try {
            return mapper.readTree(URI.create(API_URL).toURL());
        } catch (Exception e) {
            CrawlerInterface.debug("download", e.getMessage());
        }
        return null;
    }
}