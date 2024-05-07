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
import java.util.List;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Interchange;
import ap.mobile.beenavigation.base.Line;
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

  private Marker endMarker;
  private Marker startMarker;
  private Marker startPointMarker;
  private Marker endPointMarker;

  private Graph g;
  private Graph.GraphPoint startPoint, endPoint;
  private Polyline cPolyline; // bee colony result
  private Polyline aPolyline; // ant colony result
  private Polyline dPolyline; // dijkstra result
  private List<Polyline> eLinePolylines = new ArrayList<>();
  private List<Polyline> sLinePolylines = new ArrayList<>();
  private Handler ha;
  private List<Polyline> phPolylines = new ArrayList<>();
  private List<Polyline> solutionPolylines = new ArrayList<>();

  private List<Pair> pairs = new ArrayList<>();
  private HashMap<Integer, Line> lines;
  private ArrayList<Interchange> interchanges;
  private ArrayList<Polyline> linePolylines = new ArrayList<>();

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
    pairs.add(new Pair(-7.9389452744464,112.600382,-7.9719772167498,112.6133345043)); // medium no-transfer
    pairs.add(new Pair(-7.9389452744464,112.600382,-7.9563803243522,112.61370982175)); // short no-transfer
    // askForPermissions();
    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    //   if (Environment.isExternalStorageManager()) {
    //     createDir();
    //   }
    // }
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
    File file = new File(Environment.getExternalStorageDirectory(), "dijkstra.csv" );
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
            this.solutionPolylines = MapCDM.drawSolution(this.gMap, beeColony.getSolution(), this.g);
          });
        });
        for(Polyline polyline: this.solutionPolylines) polyline.remove();
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
      if (this.g == null) return false;
      if (this.startPoint != null && this.endPoint != null) {

        // this.g.draw();
        // Dijkstra
        Dijkstra dijkstra = new Dijkstra(this.g, this.startPoint, this.endPoint);
        Handler dh = new Handler(Looper.getMainLooper());
        Thread dt = new Thread(() -> {
          double timea = System.nanoTime();
          List<LatLng> path = dijkstra.run(Dijkstra.Priority.COST);
          double timeb = System.nanoTime() - timea;
          try {
            File file = new File(Environment.getExternalStorageDirectory(), "dijkstra.csv" );
            if (file.exists()) {
              FileOutputStream fos = new FileOutputStream(file, true);
              String data = this.startPoint.getLat() + ";" + this.startPoint.getLng() + ";";
              data += this.endPoint.getLat() + ";" + this.endPoint.getLng() + ";";
              data += timea + ";" + timeb + ";" + Helper.calculateDistance(startPoint, endPoint) + ";\n";
              fos.write(data.getBytes());
              fos.close();
            } else file.createNewFile();
          } catch (Exception e) {
            e.printStackTrace();
          }
          dh.post(() -> {
            this.solutionPolylines = MapCDM.drawSolution(this.gMap, dijkstra.getSolution(), this.g);
          });
        });
        for(Polyline polyline: this.solutionPolylines) polyline.remove();
        dt.start();

      }
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
      if (this.startMarker != null) this.startMarker.remove();
      if (this.endMarker != null) this.endMarker.remove();
      this.startMarker = MapCDM.drawMarker(this.gMap, MapCDM.getCurrentLocation(), BitmapDescriptorFactory.HUE_AZURE, "Start", "Starting Point", "START");
      this.endMarker = MapCDM.drawMarker(this.gMap, latLng, BitmapDescriptorFactory.HUE_ORANGE, "End", "Ending Point", "END");
      this.buildGraph();
    });

    this.gMap.setOnMarkerDragListener(this);
  }

  private void buildGraph() {
    Graph.GraphPoint startPoint = null, endPoint = null;
    Line startingLine = null, endingLine = null;
    double startPointDistance = Double.MAX_VALUE;
    double endPointDistance = Double.MAX_VALUE;
    for(Line line: this.lines.values()) {
      Point checkStart = line.getNearestPoint(this.startMarker.getPosition());
      Point checkEnd = line.getNearestPoint(this.endMarker.getPosition());

      if (checkEnd == null || checkStart == null) continue;

      double checkStartDistance = Helper.calculateDistance(checkStart.getLatLng(), this.startMarker.getPosition());
      double checkEndDistance = Helper.calculateDistance(checkEnd.getLatLng(), this.endMarker.getPosition());

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
    if (this.endPointMarker != null) this.endPointMarker.remove();
    if (this.startPointMarker != null) this.startPointMarker.remove();
    if (endPoint != null) {
      this.endPointMarker = MapCDM.drawInterchangeMarker(this.gMap, endPoint.getLatLng(), R.drawable.ic_circle);
      this.startPointMarker = MapCDM.drawInterchangeMarker(this.gMap, startPoint.getLatLng(), R.drawable.ic_circle);
      this.startPoint = startPoint;
      this.endPoint = endPoint;
      endingLine.addRestrictedPoint(endPoint);
      startingLine.addRestrictedPoint(startPoint);
      endingLine.buildSegments();
      if (endingLine != startingLine)
        startingLine.buildSegments();
      for (Polyline p: this.sLinePolylines) p.remove();
      for (Polyline p: this.eLinePolylines) p.remove();
      // this.sLinePolylines = startingLine.drawSegments(this.gMap);
      // this.eLinePolylines = endingLine.drawSegments(this.gMap);
      // Recreate the graph segments, including the starting and ending points
      if (this.g != null) this.g.clearDrawing();
      this.g = new Graph(new ArrayList<>(lines.values()), this.interchanges, this.gMap);
      // this.g.draw();
    }
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
      for(Polyline p: this.phPolylines) p.remove();
      for(AntColony.Segment s: segments) {
        if (s.getPheromone() == min) continue;
        int color = Color.YELLOW;
        if (s.getPheromone() > (avg + sd) ) color = Color.RED;
        else if (s.getPheromone() > (avg - sd)) color = Color.rgb(255,165,0);
        this.phPolylines.add(MapCDM.drawPolyline(this.gMap, new ArrayList<>(Arrays.asList(s.getSourceLatLng(), s.getEndLatLng())), color));
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
    this.buildGraph();
    for(Polyline polyline: this.solutionPolylines) polyline.remove();
  }

  @Override
  public void onMarkerDragStart(@NonNull Marker marker) {}
}