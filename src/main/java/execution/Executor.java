package execution;

import crawlers.*;
import framework.CrawlerInterface;

public class Executor {

    public static void main(String[] args) {
        CrawlerInterface crawlerToExecute = new SoccerwayCrawler();
        crawlerToExecute.downLoad();
    }
}