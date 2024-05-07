package ap.mobile.beenavigation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Interchange;
import ap.mobile.beenavigation.base.Line;
import ap.mobile.beenavigation.base.MapStatic;
import ap.mobile.beenavigation.base.Pair;
import ap.mobile.beenavigation.base.Point;
import ap.mobile.beenavigation.lib.AntColony;
import ap.mobile.beenavigation.lib.BeeColony;
import ap.mobile.beenavigation.lib.Dijkstra;
import ap.mobile.beenavigation.util.Helper;
import ap.mobile.beenavigation.util.MemoryHelper;

public class MainActivity extends AppCompatActivity
  implements MapCDM.Callback, MapCDM.IServiceInterface, AntColony.AntColonyEvent, GoogleMap.OnMarkerDragListener {

  private GoogleMap gMap;
  private MapCDM mapCDM;

  private Graph g;
  private Graph.GraphPoint startPoint, endPoint;


  private Handler ha;


  private Queue<Pair> pairs = new LinkedList<>();
  private HashMap<Integer, Line> lines;
  private ArrayList<Interchange> interchanges;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Window window = this.getWindow();
    // clear FLAG_TRANSLUCENT_STATUS flag:
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    // finally change the color
    window.setStatusBarColor(ContextCompat.getColor(this,R.color.toolbar));
    setContentView(R.layout.activity_main);
    Toolbar toolbar = this.findViewById(R.id.app_toolbar);
    this.setSupportActionBar(toolbar);
    this.mapCDM = MapCDM.init(this, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
//    pairs.add(new Pair(-7.9389452744464,112.600382,-7.9719772167498,112.6133345043)); // medium no-transfer
//    pairs.add(new Pair(-7.9389452744464,112.600382,-7.9563803243522,112.61370982175)); // short no-transfer
//    pairs.add(new Pair(-7.9615146153135,112.613521,-7.9746534441299,112.64521747828)); // short multiple-transfer
//    pairs.add(new Pair(-8.0241245688691,112.64144344255,-7.9310603930299,112.652900666)); // long no-transfer
//    pairs.add(new Pair("SN", -7.9416423435152,112.60931477546,-7.9614039236743,112.62353091502)); // short no-transfer
//    pairs.add(new Pair("SM", -7.9656745099606,112.61358875215,-7.9523396667593,112.63175210491)); // short multiple-transfer
//    pairs.add(new Pair("LN", -8.0220704724961,112.62443072884,-7.9325072506729,112.65622511195)); // long no-transfer
//    pairs.add(new Pair("LM", -8.0162618062117,112.62850865721,-7.9319107836599,112.65483668221)); // long multiple-transfer

//    S: lat/lng: (-7.969336551501829,112.62958120554686) - E: lat/lng: (-7.9868385728634586,112.63291653245687);
//    SID6;-7.9692040682516;112.62898541987;EID6;-7.9867970698673;112.63272643089;4.2175;0.01798635230672798;5000.0
//    pairs.add(new Pair("SN",-7.969336551501829,112.62958120554686,-7.9868385728634586,112.63291653245687));
//    S: lat/lng: (-7.973062334815168,112.62492690235376) - E: lat/lng: (-7.980627685589258,112.63293631374836);
//    SID15;-7.9737259376614;112.62451090675;EID22;-7.9813667819369;112.63321431101;6.702188;0.011581526106487019;15000.0
//    pairs.add(new Pair("SM",-7.973062334815168,112.62492690235376,-7.980627685589258,112.63293631374836));

//    S: lat/lng: (-7.9338424589274545,112.60619301348925) - E: lat/lng: (-8.022524273730982,112.6317786052823);
//    SID6;-7.9350186382036;112.60473489762;EID6;-8.0237277533653;112.6313303411;3.870312;0.0926100681711724;5000.0;
//    pairs.add(new Pair("LN",-7.9338424589274545,112.60619301348925,-8.022524273730982,112.6317786052823));
//    S: lat/lng: (-7.936292109147728,112.64922067523003) - E: lat/lng: (-8.0236251672402,112.62026891112328);
//    SID1;-7.9364076677239;112.64973088168;EID22;-8.0211268855806;112.62159280671;3.763646;0.08926979969322421;15000.0;
    pairs.add(new Pair("LM",-7.9414785984133935,112.65198200941086,-8.023316412726935,112.62254241853952));


    askForPermissions();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (Environment.isExternalStorageManager()) {
        createDir();
      }
    }
  }

  public void askForPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivity(intent);
        return;
      }
      createDir();
    }
  }

  public void createDir(){
    File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv" );
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    this.getMenuInflater().inflate(R.menu.action_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      Intent i = new Intent(this, SettingsActivity.class);
      this.startActivity(i);
      return true;
    }
    if (item.getItemId() == R.id.action_bee) {
      if (this.g == null) return false;
      if (this.startPoint != null && this.endPoint != null) {

        // this.g.draw();

        // Bee Colony Algorithm
        BeeColony beeColony = new BeeColony(this.g, this.startPoint, this.endPoint);
        Handler h = new Handler(Looper.getMainLooper());
        Thread t = new Thread(() -> {
          double timea = System.nanoTime();
          List<LatLng> path = beeColony.run(10);
          double timeb = System.nanoTime() - timea;
          h.post(() -> {
            Toast.makeText(this, (int) (timeb/1000000) + "ms", Toast.LENGTH_SHORT).show();
            // this.cPolyline = MapCDM.drawPolyline(this.gMap, path, Color.RED);
            MapStatic.solutionPolylines = MapCDM.drawSolution(this.gMap, beeColony.getSolution(), this.g);
          });
        });
        for(Polyline polyline: MapStatic.solutionPolylines) polyline.remove();
        t.start();

        // Ant Colony Algorithm
        // AntColony antColony = new AntColony(this.g, this.startPoint, this.endPoint, this);
        // this.ha = new Handler(Looper.getMainLooper());
        // Thread ta = new Thread(() -> {
        //   List<LatLng> path = antColony.run(5);
        //   this.ha.post(() -> {
        //     if (this.aPolyline != null) this.aPolyline.remove();
        //     // this.aPolyline = MapCDM.drawPolyline(this.gMap, path, Color.YELLOW);
        //   });
        // });
        // ta.start();

        // Dijkstra
        // Dijkstra dijkstra = new Dijkstra(this.g, this.startPoint, this.endPoint);
        // Handler dh = new Handler(Looper.getMainLooper());
        // Thread dt = new Thread(() -> {
        //   List<LatLng> path = dijkstra.run(Dijkstra.Priority.COST);
        //   dh.post(() -> {
        //     this.solutionPolylines = MapCDM.drawSolution(this.gMap, dijkstra.getSolution(), this.g);
        //   });
        // });
        // for(Polyline polyline: this.solutionPolylines) polyline.remove();
        // dt.start();

      }
      return true;
    }
    if (item.getItemId() == R.id.action_dijkstra) {
//      if (this.g == null) return false;
//      if (this.startPoint != null && this.endPoint != null) {

        // this.g.draw();
        // Dijkstra
        try {
          Pair p = this.pairs.poll();
          while (p != null) {
//            MapStatic.startMarker.setPosition(p.getStartPosition());
//            MapStatic.endMarker.setPosition(p.getEndPosition());
          LatLng start = new LatLng(-7.9414785984133935,112.65198200941086);
          LatLng end = new LatLng(-8.023316412726935,112.62254241853952);
          List<Line> lines = new ArrayList<>(this.lines.values());
            for (int i = 0; i < 30; i++) {
//              Graph g = MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(), lines, this.interchanges);
              Graph g = MainActivity.buildGraph(start, end, lines, this.interchanges);

              Handler dh = new Handler(Looper.getMainLooper());
              String type = "X";
//              String type = p.type;
              Thread dt = new Thread(() -> {
                Graph.GraphPoint startPoint = g.getStartPoint();
                Graph.GraphPoint endPoint = g.getEndPoint();
                Dijkstra dijkstra = new Dijkstra(g, startPoint, endPoint);
                double timea = System.nanoTime();
                List<LatLng> path = dijkstra.run(Dijkstra.Priority.COST);
                double timeb = (System.nanoTime() - timea) / 1000000;
                try {
                  File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
                  if (file.exists()) {
                    FileOutputStream fos = new FileOutputStream(file, true);
                    String data = type + ";";
                    data += "SID" + startPoint.getIdLine() + ";" + startPoint.getLat() + ";" + startPoint.getLng() + ";";
                    data += "EID" + endPoint.getIdLine() + ";" + endPoint.getLat() + ";" + endPoint.getLng() + ";";
                    data += timeb + ";" + Helper.calculateDistance(startPoint, endPoint) + ";";
                    data += dijkstra.getCost() + ";\n";
                    fos.write(data.getBytes());
                    fos.close();
                  }
                } catch (Exception e) {}
                dh.post(() -> {
                  for (Polyline polyline : MapStatic.solutionPolylines) polyline.remove();
                  List<Graph.GraphPoint> solution = dijkstra.getSolution();
                  MapStatic.solutionPolylines = MapCDM.drawSolution(this.gMap, solution, g);
                });

              });

              dt.start();
              dt.join();
            }
            p = this.pairs.poll();
          }
        } catch (Exception e) {}
//      }
      return true;
    }

    return true;
  }

  @Override
  public void onMapReady(GoogleMap gMap) {
    this.gMap = gMap;
    this.gMap.getUiSettings().setZoomControlsEnabled(true);
    MapCDM.loadPointData(this, this);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == MapCDM.LOCATION_REQUEST_CODE
        && grantResults[0] == PackageManager.PERMISSION_GRANTED
        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
      this.mapCDM.showLastLocation();
    }
  }

  @Override
  public void onDataLoaded(HashMap<Integer, Line> lines, ArrayList<Interchange> interchanges) {
    
    // g.draw(lines);
    // for(Line line: lines.values()) {
    //  line.drawSegments(this.gMap);
    //  line.drawStops(this.gMap);
    // }
    this.lines = lines;
    this.interchanges = interchanges;

    // for(Polyline p: this.linePolylines) p.remove();
    // this.linePolylines = new ArrayList<>();
    // for(Line line: lines.values()) {
    //   Polyline p = line.drawPath(this.gMap);
    //   this.linePolylines.add(p);
    //   if (line.getPath().size() == 0) continue;
    //   LatLng start = new LatLng(p.getPoints().get(0).latitude, p.getPoints().get(0).longitude);
    //   LatLng end = new LatLng(p.getPoints().get(p.getPoints().size()-1).latitude, p.getPoints().get(p.getPoints().size()-1).longitude);
    //   MapCDM.drawInterchangeMarker(this.gMap, start, R.drawable.ic_circle);
    //   MapCDM.drawInterchangeMarker(this.gMap, end, R.drawable.ic_circle);
    // }
    // for(Interchange i: this.interchanges) {
    //   MapCDM.drawInterchangeMarker(this.gMap, i.getLatLng(), R.drawable.ic_circle);
    // }

    this.gMap.setOnMapClickListener(latLng -> {
      if (MapStatic.startMarker != null) MapStatic.startMarker.remove();
      if (MapStatic.endMarker != null) MapStatic.endMarker.remove();
      MapStatic.startMarker = MapCDM.drawMarker(this.gMap, MapCDM.getCurrentLocation(), BitmapDescriptorFactory.HUE_AZURE, "Start", "Starting Point", "START");
      MapStatic.endMarker = MapCDM.drawMarker(this.gMap, latLng, BitmapDescriptorFactory.HUE_ORANGE, "End", "Ending Point", "END");
      Graph g = MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(),
              new ArrayList<>(this.lines.values()), this.interchanges);
      g.drawTerminal(this.gMap);
      File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
      try {
        if (file.exists()) {
          FileOutputStream fos = new FileOutputStream(file, true);
          String data = "S: " + MapStatic.startMarker.getPosition().toString() + " - ";
          data += "E: " + MapStatic.endMarker.getPosition().toString() + ";\n";
          fos.write(data.getBytes());
          fos.close();
        }
      } catch (Exception e) {}

    });

    this.gMap.setOnMarkerDragListener(this);
  }

  private static Graph buildGraph(LatLng startPos, LatLng endPos, List<Line> lines, List<Interchange> interchanges) {
    Graph.GraphPoint startPoint = null, endPoint = null;
    Line startingLine = null, endingLine = null;
    double startPointDistance = Double.MAX_VALUE;
    double endPointDistance = Double.MAX_VALUE;
    for(Line line: lines) {
      Point checkStart = line.getNearestPoint(startPos);
      Point checkEnd = line.getNearestPoint(endPos);

      if (checkEnd == null || checkStart == null) continue;

      double checkStartDistance = Helper.calculateDistance(checkStart.getLatLng(), startPos);
      double checkEndDistance = Helper.calculateDistance(checkEnd.getLatLng(), endPos);

      if (endPoint == null || checkEndDistance < endPointDistance) {
        endPoint = new Graph.GraphPoint(checkEnd);
        endPointDistance = checkEndDistance;
        endingLine = line;
      }
      if (startPoint == null || checkStartDistance < startPointDistance) {
        startPoint = new Graph.GraphPoint(checkStart);
        startPointDistance = checkStartDistance;
        startingLine = line;
      }
      if (line.hasRestrictedPoints()) {
        line.clearRestrictedPoints();
        line.buildSegments();
      }
    }
    if (endPoint != null) {
//      MapStatic.endPointMarker = MapCDM.drawInterchangeMarker(this.gMap, endPoint.getLatLng(), R.drawable.ic_circle);
//      MapStatic.startPointMarker = MapCDM.drawInterchangeMarker(this.gMap, startPoint.getLatLng(), R.drawable.ic_circle);
//      this.startPoint = startPoint;
//      this.endPoint = endPoint;
      endingLine.addRestrictedPoint(endPoint);
      startingLine.addRestrictedPoint(startPoint);
      endingLine.buildSegments();
      if (endingLine != startingLine)
        startingLine.buildSegments();
      for (Polyline p: MapStatic.sLinePolylines) p.remove();
      for (Polyline p: MapStatic.eLinePolylines) p.remove();
      // this.sLinePolylines = startingLine.drawSegments(this.gMap);
      // this.eLinePolylines = endingLine.drawSegments(this.gMap);
      // Recreate the graph segments, including the starting and ending points
//      if (this.g != null) this.g.clearDrawing();
      Graph g = new Graph(lines, interchanges);
      g.setStartEnd(startPoint, endPoint);
      // this.g.draw();
      return g;
    }
    return null;
  }

  @Override
  public void onCycle(int iteration, List<AntColony.Segment> segments) {
    this.ha.post(() -> {
      double min = Double.MAX_VALUE;
      double max = 0;
      double avg = 0;
      double sum = 0;
      for(AntColony.Segment s: segments) {
        double ph = s.getPheromone();
        if (ph > max) max = ph;
        if (ph < min) min = ph;
      }
      List<Double> data = new ArrayList<>();
      for(AntColony.Segment s: segments) {
        if (s.getPheromone() == min) continue;
        sum += s.getPheromone();
        data.add(s.getPheromone());
      }
      avg = sum/data.size();
      double sxmxb = 0;
      for (int i = 0; i < data.size(); i++) {
        sxmxb += Math.pow(data.get(i) - avg, 2);
      }
      double sd = Math.sqrt(sxmxb / data.size());
      // double onethird = (max - min) * 2 / 3;
      // double twothird = (max - min) * 5 / 6;
      for(Polyline p: MapStatic.phPolylines) p.remove();
      for(AntColony.Segment s: segments) {
        if (s.getPheromone() == min) continue;
        int color = Color.YELLOW;
        if (s.getPheromone() > (avg + sd) ) color = Color.RED;
        else if (s.getPheromone() > (avg - sd)) color = Color.rgb(255,165,0);
        MapStatic.phPolylines.add(MapCDM.drawPolyline(this.gMap, new ArrayList<>(Arrays.asList(s.getSourceLatLng(), s.getEndLatLng())), color));
      }
    });
  }

  private static boolean isExternalStorageReadOnly() {
    String extStorageState = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
  }

  private static boolean isExternalStorageAvailable() {
    String extStorageState = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(extStorageState);
  }

  @Override
  public void onMarkerDrag(@NonNull Marker marker) {}

  @Override
  public void onMarkerDragEnd(@NonNull Marker marker) {
    if (marker.getTag() == "END") Toast.makeText(MainActivity.this, "End", Toast.LENGTH_SHORT).show();
    if (marker.getTag() == "START") Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
    MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(), new ArrayList<>(this.lines.values()), this.interchanges);
    for(Polyline polyline: MapStatic.solutionPolylines) polyline.remove();

    File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
    try {
      if (file.exists()) {
        FileOutputStream fos = new FileOutputStream(file, true);
        String data = "S: " + MapStatic.startMarker.getPosition().toString() + " - ";
        data += "E: " + MapStatic.endMarker.getPosition().toString() + ";\n";
        fos.write(data.getBytes());
        fos.close();
      }
    } catch (Exception e) {}
  }

  @Override
  public void onMarkerDragStart(@NonNull Marker marker) {}
}