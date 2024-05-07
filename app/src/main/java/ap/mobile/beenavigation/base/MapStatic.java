package ap.mobile.beenavigation.base;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapStatic {

    public static Marker endMarker;
    public static Marker startMarker;
    public static Marker startPointMarker;
    public static Marker endPointMarker;

    public static Polyline cPolyline; // bee colony result
    public static Polyline aPolyline; // ant colony result
    public static Polyline dPolyline; // dijkstra result

    public static List<Polyline> eLinePolylines = new ArrayList<>();
    public static List<Polyline> sLinePolylines = new ArrayList<>();

    public static List<Polyline> phPolylines = new ArrayList<>();
    public static List<Polyline> solutionPolylines = new ArrayList<>();
    public static ArrayList<Polyline> linePolylines = new ArrayList<>();


    public static Marker startTerminalMarker;
    public static Marker endTerminalMarker;
}
