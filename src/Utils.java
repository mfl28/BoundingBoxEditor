public class Utils {
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static String filenameFromUrl(String url){
        return url.substring(Math.max(url.lastIndexOf("/"), url.lastIndexOf("\\")) + 1, url.lastIndexOf("."));
    }
}
