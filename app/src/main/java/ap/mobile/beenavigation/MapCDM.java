package ap.mobile.beenavigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Interchange;
import ap.mobile.beenavigation.base.Line;
import ap.mobile.beenavigation.base.MapStatic;
import ap.mobile.beenavigation.base.Point;

public class MapCDM implements OnMapReadyCallback {

  public static final int LOCATION_REQUEST_CODE = 99;
  private static Callback callback;
  private static GoogleMap gMap;
  private final Context context;

  private static boolean showCurrentLocation = false;
  private static LatLng currentLocation;

  // Map Utilities

  private static final int PATTERN_DASH_LENGTH_PX = 20;
  private static final int PATTERN_GAP_LENGTH_PX = 10;
  private static final PatternItem DOT = new Dot();
  private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
  private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
  private static final List<PatternItem> PATTERN_POLYGON_WALKING = Arrays.asList(DASH, GAP);
  private static final int colorWalking = Color.parseColor("#e53935");
  private static final int colorTransfer = Color.parseColor("#ff9800");

  public MapCDM(Context context) {
    this.context = context;
  }

  public static MapCDM init(Context context, Callback callback) {
    return MapCDM.init(context, callback, true);
  }

  public static MapCDM init(Context context, Callback callback, boolean showCurrentLocation) {
    MapCDM mapCDM = new MapCDM(context);
    MapCDM.callback = callback;
    MapCDM.showCurrentLocation = showCurrentLocation;
    ((SupportMapFragment) Objects.requireNonNull(((AppCompatActivity) context)
        .getSupportFragmentManager()
        .findFragmentById(R.id.mapView))).getMapAsync(mapCDM);
    return mapCDM;
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    try {
      MapCDM.gMap = googleMap;
      MapCDM.gMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.context, R.raw.map_clean_style));
      if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MapCDM.LOCATION_REQUEST_CODE);
        return;
      }
      if (MapCDM.showCurrentLocation) this.showLastLocation();
    } catch (Exception ignored) {}
    if (MapCDM.callback != null)
      MapCDM.callback.onMapReady(gMap);
  }

  public void showLastLocation() {
    FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    LocationRequest locationRequest = new LocationRequest.Builder(1000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();
    LocationListener locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(@NonNull Location location) {
        fusedLocationClient.removeLocationUpdates(this);
        if(MapCDM.gMap == null) return;
        MapCDM.gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));
        MapCDM.currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
      }
    };
    fusedLocationClient.requestLocationUpdates(locationRequest,
      locationListener, Looper.getMainLooper());
  }

  public interface Callback {
    void onMapReady(GoogleMap gMap);
  }

  public interface IServiceInterface {
    void onDataLoaded(HashMap<Integer, Line> lines, ArrayList<Interchange> interchanges);
    // void onDataLoadError(String error);
  }

  public static void loadPointData(Context context, final IServiceInterface callback) {

    Handler h = new Handler(Looper.getMainLooper());
    Thread t = new Thread(() -> {

      boolean useOfflineData = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_offline", true);
      HashMap<Integer, Line> lines = new HashMap<>();
      ArrayList<Interchange> interchanges = new ArrayList<>();

      if (!useOfflineData) {
        Log.w("VOLLEY", "ONLINE");
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        String url = "https://mgm.ub.ac.id/index.php/admin/m/x/mta/dataApi/getAllData";
        StringRequest stringRequest = new StringRequest(url, response -> {
          try {
            parseJSONString(lines, interchanges, response);
            h.post(() -> {
              if (callback != null) callback.onDataLoaded(lines, interchanges);
            });
          } catch (JSONException e) {
            throw new RuntimeException(e);
          }
        }, error -> {
          Log.e("VOLLEY", error.toString());
        });
        requestQueue.add(stringRequest);
      } else {
        Log.w("VOLLEY", "OFFLINE");
        try {
          String rawManagedPointsJson = readRawString(context, R.raw.mta_network);
          parseJSONString(lines, interchanges, rawManagedPointsJson);
          h.post(() -> {
            if (callback != null) callback.onDataLoaded(lines, interchanges);
          });
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
    t.start();



  }

  private static void parseJSONString(HashMap<Integer, Line> lines, ArrayList<Interchange> interchanges, String rawManagedPointsJson) throws JSONException {
    JSONObject response = new JSONObject(rawManagedPointsJson);
    JSONArray linesJson = response.getJSONArray("lines");
    for(int i=0; i<linesJson.length();i++) {
      JSONObject lineJson = linesJson.getJSONObject(i);
      /* "idline": "1", "name": "AL", "direction": "O", "color": "#FF0000", "path": [] */
      Line line = new Line(
          Integer.parseInt(lineJson.getString("idline")),
          lineJson.getString("name"),
          lineJson.getString("direction"),
          Color.parseColor(lineJson.getString("color"))
      );

      JSONArray pathJson = lineJson.getJSONArray("path");

      for(int j = 0; j<pathJson.length(); j++) {
        JSONObject pointJson = pathJson.getJSONObject(j);
        /*
          {"idline":"1","idpoint":"637","sequence":"0","stop":"0","idinterchange":null,"lat":"-7.9346600068216","lng":"112.65868753195"}
          String id, String idInterchange, int idLine, int sequence, boolean stop, double lat, double lng
        */
        int sequence = Integer.parseInt(pointJson.getString("sequence"));
        Point point = new Point(
            pointJson.getString("idpoint"),
            line.getId(),
            Integer.parseInt(pointJson.getString("sequence")),
            (j == 0 || j == pathJson.length() - 1 || pointJson.getString("stop").equals("1")),
            Double.parseDouble(pointJson.getString("lat")),
            Double.parseDouble(pointJson.getString("lng")),
            pointJson.getString("idinterchange").equals("null") ?  null : pointJson.getString("idinterchange")
        );
        line.addPoint(sequence, point);
      }
      // if (line.getDirection() == Line.Direction.OUTBOUND)
      line.buildSegments();
      lines.put(line.getId(), line);

    }
    JSONArray interchangesJson = response.getJSONArray("interchanges");
    for(int i = 0; i<interchangesJson.length(); i++) {

      JSONObject interchangeJson = interchangesJson.getJSONObject(i);
      Interchange interchange = new Interchange(
          "i" + interchangeJson.getString("idinterchange"),
          interchangeJson.getString("name")
      );
      JSONArray pointsJson = interchangeJson.getJSONArray("points");
      for(int j = 0; j < pointsJson.length(); j++) {
        JSONObject pointJson = pointsJson.getJSONObject(j);
        Line line = lines.get(Integer.parseInt(pointJson.getString("idline")));
        if (line != null) {
          Point p = line.getPoints().get(pointJson.getString("idpoint"));
          if (p != null) interchange.addPoint(p);
        }
      }
      if(interchange.getPoints().size() <= 1) {
        Log.e("BEE", "Invalid Interchange");
      } else interchanges.add(interchange);

    }
  }

  private static String readRawString(Context context, int rawResourceId) {
    InputStream inputStream = context.getResources().openRawResource(rawResourceId);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      int i = inputStream.read();
      while (i != -1) {
        byteArrayOutputStream.write(i);
        i = inputStream.read();
      }
      inputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return byteArrayOutputStream.toString();
  }

  // Utilities

  public static PolylineOptions getWalkingPolylineOptions() {
    return new PolylineOptions()
        .color(colorWalking)
        .pattern(PATTERN_POLYGON_WALKING)
        .width(10f);
  }

  public static List<Polyline> drawSolution(GoogleMap gMap, List<Graph.GraphPoint> solution, Graph g) {
    List<Polyline> polylines = new ArrayList<>();
    Graph.GraphPoint prevPoint = null;
    List<LatLng> segment = new ArrayList<>();
    for(Marker m: MapStatic.markedMarkers) m.remove();
    boolean skip = false;
    for(Graph.GraphPoint p: solution) {
      if (p.marked) {
        Marker m = MapCDM.drawInterchangeMarker(gMap, p.getLatLng(), R.drawable.ic_circle_red);
        MapStatic.markedMarkers.add(m);
        skip = !skip;
      }
      if (prevPoint == null) {
        prevPoint = p;
        segment = new ArrayList<>();
        segment.add(p.getLatLng());
        continue;
      }
      if (p.getIdLine() != prevPoint.getIdLine()) {
        segment.add(p.getLatLng());
        int idLine = prevPoint.getIdLine();
        Line line = g.getLine(idLine);
        int color = line.getColor();
        // if (skip) color = Color.parseColor("#000000");
        polylines.add(MapCDM.drawPolyline(gMap, segment, color));
        segment = new ArrayList<>();
      }
      segment.add(p.getLatLng());
      prevPoint = p;
    }
    if (segment.size() > 1)  {
      int idLine = prevPoint.getIdLine();
      Line line = g.getLine(idLine);
      int color = line.getColor();
      polylines.add(MapCDM.drawPolyline(gMap, segment, color));
    }
    if (MapStatic.startTerminalMarker != null) MapStatic.startTerminalMarker.remove();
    if (MapStatic.endTerminalMarker != null) MapStatic.endTerminalMarker.remove();
    if (solution.size() > 0) {
      MapStatic.startTerminalMarker = MapCDM.drawInterchangeMarker(gMap, solution.get(0).getLatLng(), R.drawable.ic_circle);
      MapStatic.endTerminalMarker = MapCDM.drawInterchangeMarker(gMap, solution.get(solution.size() - 1).getLatLng(), R.drawable.ic_circle);
    }
    return polylines;
  }

  public static Polyline drawPolyline(GoogleMap map, List<LatLng> line, int color) {
    PolylineOptions polylineOptions = new PolylineOptions()
        .color(color)
        .width(7f);
    for (LatLng point : line) {
      polylineOptions.add(point);
    }
    return map.addPolyline(polylineOptions);
  }

  public static Marker drawInterchangeMarker(GoogleMap map, LatLng position, int markerResource) {
    MarkerOptions markerOptions = new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromResource(markerResource))
        .position(position)
        .anchor(0.5f, 0.5f);
    return map.addMarker(markerOptions);
  }

  public static Marker drawMarker(GoogleMap map, LatLng position, float color, String label, String description, String tag) {
    MarkerOptions markerOptions = new MarkerOptions()
        .icon(BitmapDescriptorFactory.defaultMarker(color))
        .title(label)
        .snippet(description)
        .position(position)
        .draggable(true);
    Marker m = map.addMarker(markerOptions);
    m.setTag(tag);
    return m;
  }

  public static LatLng getCurrentLocation() {
    return MapCDM.currentLocation;
  }


}
