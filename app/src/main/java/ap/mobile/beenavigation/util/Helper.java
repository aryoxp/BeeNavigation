package ap.mobile.beenavigation.util;

import android.content.res.Resources;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import ap.mobile.beenavigation.base.Point;

public class Helper {

    public static double calculateDistance(Point a, Point b) {
        return Math.sqrt(Math.pow((a.getLat() - b.getLat()), 2) + Math.pow((a.getLng() - b.getLng()), 2));
    }

    public static double calculateDistance(Point a, Double lat, Double lng) {
        return Math.sqrt(Math.pow((a.getLat() - lat), 2) + Math.pow((a.getLng() - lng), 2));
    }

    public static double calculateDistance(LatLng source, LatLng destination) {
        return Math.sqrt(Math.pow((source.latitude - destination.latitude), 2)
                + Math.pow((source.longitude - destination.longitude), 2));
    }

    public static String humanReadableDistance(double distance) {
        int unit = 1000;
        if (distance < unit) return String.format(Locale.getDefault(), "%.1f", distance) + " m";
        int exp = (int) (Math.log(distance) / Math.log(unit));
        String pre = String.valueOf("kMGTPE".charAt(exp-1));
        return String.format(Locale.getDefault(),"%.1f %sm", distance / Math.pow(unit, exp), pre);
    }

    public static int toDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int toPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }


}
