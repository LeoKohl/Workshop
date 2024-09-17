package framework;

public abstract class CrawlerInterface {

    public abstract void downLoad();

    protected static void debug(String method, String message) {
        debug(method + " : " + message);
    }

    protected static void debug(String method, int message) {
        debug(method, message + "");
    }

    protected static void debug(String message) {
        System.out.println(message);
    }

}
