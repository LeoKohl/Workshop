package crawlers;

import framework.CrawlerInterface;
import framework.FixtureObject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EliteserienCrawler extends CrawlerInterface {

    static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    static final String CATEGORY = "Eliteserien";

    static final String TIMEZONE = "Europe/Oslo";

    static final String DATEFORMAT = "dd.MM.yyyy HH:mm";

    static final String API_URL = "https://www.eliteserien.no/terminliste";

    @Override
    public void downLoad() {
        try {
            Document document = getDocument();
            if (document != null) {
            parseDocument(document);
            debug("Found Fixtures: ", FIXTURE_LIST.size());
            FIXTURE_LIST.forEach(fixture -> debug(fixture.toString()));
            }
        } catch (Exception e) {
            debug("downLoad: ", e.getMessage());
        }
    }
    private static void parseDocument(Document document) {
        try {
            Element match = document.selectFirst("tr[class*='schedule__match schedule__match']");
            Elements matches = document.select("tr[class*='future__match__terminlist']");
            parseMatch(match);
            matches.forEach(EliteserienCrawler::parseMatch);
        } catch (Exception e) {
            debug("parseDocument: ", e.getMessage());
        }
    }
    private static void parseMatch(Element match) {
        try {
            String[] Teams = match.select("td[class*='item--teams']").text().split("-");
            String homeTeam = Teams[0].trim();
            String awayTeam = Teams[1].trim();
            String date = match.select("td[class*='item--date'] > span:eq(0)").text();
            String time = match.select("td[class*='item--date'] > span:eq(1)").text();
            FixtureObject fixture = new FixtureObject()
                    .setHomeTeam(homeTeam)
                    .setAwayTeam(awayTeam)
                    .setCategory(CATEGORY)
                    .setDate(date + " " + time, DATEFORMAT, TIMEZONE);
            FIXTURE_LIST.add(fixture);
        } catch (Exception e) {
            debug("parseMatch: ", e.getMessage());
        }
    }

    private static Document getDocument() {
        try {
            return Jsoup.connect(API_URL).get();
        } catch (Exception e) {
            debug("getDocument: ", e.getMessage());
        } return null;
    }
}
