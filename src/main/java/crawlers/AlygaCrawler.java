package crawlers;

import framework.CrawlerInterface;
import framework.FixtureObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class AlygaCrawler extends CrawlerInterface {

    private static final String CATEGORY = "Soccer.Lithuania.1st";

    private static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    private static final String dateFormat = "yyyy-MM-dd, HH:mm";

    private static final String timeZone = "Europe/Vilnius";

    @Override
    public void downLoad() {
        try {
            String url = "https://alyga.lt/tvarkarastis/1";
            Document document = getDocument(url);
            if (document != null) {
                parseDocument(document);
                CrawlerInterface.debug("Found Fixtures", FIXTURE_LIST.size());
                FIXTURE_LIST.forEach( fixture -> CrawlerInterface.debug(fixture.toString()));
            }
        } catch (Exception e) {
            CrawlerInterface.debug("main", e.getMessage());
        }
    }

    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("table[class='table01 schedule_table'] tr[class]");
            matches.forEach(AlygaCrawler::parseMatch);
        }catch (Exception e) {
            CrawlerInterface.debug("parseDocument", e.getMessage());
        }
    }

    private static void parseMatch(Element match) {
        try {
            String home = match.select("td:eq(1) > a").text();
            String away = match.select("td:eq(3) > a").text();
            String date = match.select("td:eq(0)").text();

            FixtureObject fixture = new FixtureObject()
                    .setHomeTeam(home)
                    .setAwayTeam(away)
                    .setCategory(CATEGORY)
                    .setDate(date, dateFormat, timeZone);
            FIXTURE_LIST.add(fixture);
        } catch (Exception e) {
            CrawlerInterface.debug("parseMatch", e.getMessage());
        }
    }

    private static Document getDocument(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (Exception e) {
            CrawlerInterface.debug("downLoad", e.getMessage());
        }
        return null;
    }
}