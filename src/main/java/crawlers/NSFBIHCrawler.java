package crawlers;

import framework.CrawlerInterface;
import framework.FixtureObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NSFBIHCrawler extends CrawlerInterface {

    private static final String timeZone = "Europe/Sarajevo";

    private static final List<FixtureObject> FIXTURE_LIST = new ArrayList<>();

    private static final String dateFormat = "dd.MM.yyyy. HH:mm";

    protected static String CATEGORY;
    
    private static final String BASE_URL = "https://www.nsfbih.ba/takmicenja/";

    private static final Map<String, String> league_Map = Map.ofEntries(
            Map.entry("WWIN.Prva.Liga.FBiH", "wwin-prva-liga-fbih"),
            Map.entry("WWIN.Prva.Liga.FBiH-Sjever", "wwin-druga-liga-fbih-sjever"),
            Map.entry("WWIN.Prva.Liga.FBiH-Centar", "wwin-druga-liga-fbih-centar"),
            Map.entry("WWIN.Prva.Liga.FBiH-Zapad", "wwin-druga-liga-fbih-zapad"),
            Map.entry("WWIN.Prva.Liga.FBiH-Jug", "wwin-druga-liga-fbih-jug"),
            Map.entry("Prva.ženska.Liga.FBiH","prva-zenska-liga-fbih"),
            Map.entry("Omladinska.Liga.BiH.Kadeti-Centar.1","omladinska-liga-bih-kadeti-centar-1"),
            Map.entry("Omladinska.Liga.BiH.Juniori-Centar.1", "omladinska-liga-bih-juniori-centar-1"),
            Map.entry("Omladinska.Liga.BiH.Kadeti-Centar.2","omladinska-liga-bih-kadeti-centar-2"),
            Map.entry("Omladinska.Liga.BiH.Juniori-Centar.2", "omladinska-liga-bih-juniori-centar-2"),
            Map.entry("Omladinska.Liga.BiH.Kadeti-Jug","omladinska-liga-bih-kadeti-jug"),
            Map.entry("Omladinska.Liga.BiH.Juniori-Jug", "omladinska-liga-bih-juniori-jug"),
            Map.entry("Prva.Futsal.liga.FBiH-Sjever","prva-futsal-liga-fbih-sjever"),
            Map.entry("Prva.Futsal.liga.FBiH-Centar","prva-futsal-liga-fbih-centar"),
            Map.entry("Prva.Futsal.liga.FBiH-Zapad","prva-futsal-liga-fbih-zapad"),
            Map.entry("Prva.Futsal.liga.FBiH-Jug","prva-futsal-liga-fbih-jug")
    );

    @Override
    public void downLoad() {
        league_Map.forEach((category, url) -> {
            CATEGORY = category;
            try {
                Document document = getDocument(BASE_URL+url);
                if (document != null) {
                    parseDocument(document);
                }
            } catch (Exception e) {
                CrawlerInterface.debug("downLoad", e.getMessage());
            }
        });

        CrawlerInterface.debug("Found Fixtures", FIXTURE_LIST.size());
        FIXTURE_LIST.forEach(fixture -> CrawlerInterface.debug(fixture.toString()));
    }

    private static void parseDocument(Document document) {
        try {
            Elements matches = document.select("div#t3 tbody tr");
            matches.forEach(NSFBIHCrawler::parseMatch);
        } catch (Exception e) {
            CrawlerInterface.debug("parseDocument", e.getMessage());
        }
    }

    private static void parseMatch(Element match) {
        try {
            String home = match.select("td:eq(1) > b").text();
            String away = match.select("td:eq(5) > b").text();
            String date = match.select("td:eq(0)").text();
            String score = match.select("td:eq(3) > b").text();

            FixtureObject fixture = new FixtureObject()
                    .setHomeTeam(home)
                    .setAwayTeam(away)
                    .setCategory(CATEGORY)
                    .setDate(date, dateFormat, timeZone);
//                    .setScore(score);
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