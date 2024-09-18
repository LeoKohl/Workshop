package execution;

import crawlers.AlygaCrawler;
import crawlers.EHWCrawler;
import crawlers.IIHFCrawler;
import crawlers.NSFBIHCrawler;
import framework.CrawlerInterface;

public class Executor {

    public static void main(String[] args) {
        CrawlerInterface crawlerToExecute = new NSFBIHCrawler();
        crawlerToExecute.downLoad();
    }
}