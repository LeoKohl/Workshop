package crawlers;

import framework.CrawlerInterface;
import framework.FixtureObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class IIHFCrawler extends CrawlerInterface {

    private static final String CATEGORY = "Ice.Hockey.World.Championchip.Czechia";

    private static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    private static final String timeZone = "Etc/UTC";

    private static final String dateFormat = "yyyy-MM-dd HH:mm";

    @Override
    public void downLoad() {
        try {
            String url = "https://www.iihf.com/en/events/2024/wm/schedule";
            Document document = getDocument(url);
            parseDocument(document);
            CrawlerInterface.debug("found Fixtures", FIXTURE_LIST.size());
            FIXTURE_LIST.forEach(fixture -> CrawlerInterface.debug(fixture.toString()));

        } catch (Exception e) {
            CrawlerInterface.debug("main", e.getMessage());
        }

    }

    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("div[class='s-content-main'] div[class*='b-card-schedule']");
            matches.forEach(IIHFCrawler::parseMatch);
        } catch (Exception e) {
            CrawlerInterface.debug("parseDocument", e.getMessage());
        }

    }

    private static void parseMatch(Element match) {
        try {
            String home = match.attr("data-hometeam");
            String away = match.attr("data-guestteam");
            String date = match.attr("data-date-utc");
            String time = match.attr("data-time-utc");

            FixtureObject fixture = new FixtureObject(home, away, CATEGORY, date + " " + time, dateFormat, timeZone);
            FIXTURE_LIST.add(fixture);
        } catch (Exception e) {
            CrawlerInterface.debug("parseMatch", e.getMessage());
        }
    }

    private static Document getDocument(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (Exception e) {
            CrawlerInterface.debug("download", e.getMessage());
        }
        return null;
    }
}