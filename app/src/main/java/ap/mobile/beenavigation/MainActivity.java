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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

public class MainActivity extends AppCompatActivity
  implements MapCDM.Callback, MapCDM.IServiceInterface, AntColony.AntColonyEvent, GoogleMap.OnMarkerDragListener {

  private GoogleMap gMap;
  private MapCDM mapCDM;

  private final Queue<Pair> pairs = new LinkedList<>();
  private HashMap<Integer, Line> lines;
  private ArrayList<Interchange> interchanges;
  private Handler ha;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Window window = this.getWindow();
    // clear FLAG_TRANSLUCENT_STATUS flag:
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    // finally change the color
    window.setStatusBarColor(ContextCompat.getColor(this, R.color.toolbar));
    setContentView(R.layout.activity_main);
    Toolbar toolbar = this.findViewById(R.id.app_toolbar);
    this.setSupportActionBar(toolbar);
    this.mapCDM = MapCDM.init(this, this);
  }

  @Override
  protected void onResume() {
    super.onResume();

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

  public void createDir() {
    File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
    try {
      file.createNewFile();
      file = new File(Environment.getExternalStorageDirectory(), "bee-test.csv");
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
      pairs.add(new Pair("SN", -7.9407324572775915, 112.6097097247839, -7.955395359710581, 112.61473517864943));
      pairs.add(new Pair("SM", -7.973144015452409, 112.62706462293863, -7.974988466149719, 112.63451311737299));
      pairs.add(new Pair("LN", -7.931323391709715, 112.6036999002099, -8.020481172146864, 112.62394990772009));
      pairs.add(new Pair("LM", -7.9414785984133935, 112.65198200941086, -8.023316412726935, 112.62254241853952));
      // Bee Colony Algorithm
      try {
        Pair p = this.pairs.poll();
        while (p != null) {
          LatLng start = p.getStartPosition();
          LatLng end = p.getEndPosition();
          if (MapStatic.startPointMarker != null) MapStatic.startPointMarker.remove();
          if (MapStatic.endPointMarker != null) MapStatic.endPointMarker.remove();
          MapStatic.startPointMarker = MapCDM.drawInterchangeMarker(this.gMap, start, R.drawable.ic_circle);
          MapStatic.endPointMarker = MapCDM.drawInterchangeMarker(this.gMap, end, R.drawable.ic_circle);
          List<Line> lines = new ArrayList<>(this.lines.values());
          for (Polyline polyline : MapStatic.solutionPolylines) polyline.remove();
          for (int i = 0; i < 100; i++) {
            String type = p.type;
            // Thread t = new Thread(() -> {
            Graph g = MainActivity.buildGraph(start, end, lines, this.interchanges);
            Graph.GraphPoint startPoint = g.getStartPoint();
            Graph.GraphPoint endPoint = g.getEndPoint();
            BeeColony beeColony = new BeeColony(g, startPoint, endPoint);
            double timea = System.nanoTime();
            List<LatLng> path = beeColony.run(10);
            double timeb = (System.nanoTime() - timea) / 1000000;
            try {
              File file = new File(Environment.getExternalStorageDirectory(), "bee-test.csv");
              if (file.exists()) {
                FileOutputStream fos = new FileOutputStream(file, true);
                String data = type + ";";
                data += "SID" + startPoint.getIdLine() + ";" + startPoint.getLat() + ";" + startPoint.getLng() + ";";
                data += "EID" + endPoint.getIdLine() + ";" + endPoint.getLat() + ";" + endPoint.getLng() + ";";
                data += timeb + ";" + Helper.calculateDistance(startPoint, endPoint) + ";";
                data += beeColony.getCost() + ";\n";
                fos.write(data.getBytes());
                fos.close();
              }
            } catch (Exception e) {
            }
            // h.post(() -> {
            for (Polyline polyline : MapStatic.solutionPolylines) polyline.remove();
            List<Graph.GraphPoint> solution = beeColony.getSolution();
            if (solution != null) {
              MapStatic.solutionPolylines = MapCDM.drawSolution(this.gMap, solution, g);
            }
            Log.d("BEE", "Thread Finish. Cost: " + beeColony.getCost());
            // });
            // });
            // t.start();
            // t.join();
            System.gc();
            Runtime.getRuntime().gc();
          } // for
          p = this.pairs.poll();
        } // while
      } catch (Exception e) {
      } // try-catch
      return true;
    }
    if (item.getItemId() == R.id.action_dijkstra) {
      // this.g.draw();
      // Dijkstra
      try {
        Pair p = this.pairs.poll();
        while (p != null) {
          LatLng start = p.getStartPosition();
          LatLng end = p.getEndPosition();
          List<Line> lines = new ArrayList<>(this.lines.values());
          for (int i = 0; i < 100; i++) {
            // Graph g = MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(), lines, this.interchanges);
            Graph g = MainActivity.buildGraph(start, end, lines, this.interchanges);

            Handler dh = new Handler(Looper.getMainLooper());
            // String type = "X";
            String type = p.type;
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
              } catch (Exception e) {
              }
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
      } catch (Exception e) {
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
      if (MapStatic.startMarker != null) MapStatic.startMarker.remove();
      if (MapStatic.endMarker != null) MapStatic.endMarker.remove();
      MapStatic.startMarker = MapCDM.drawMarker(this.gMap, MapCDM.getCurrentLocation(), BitmapDescriptorFactory.HUE_AZURE, "Start", "Starting Point", "START");
      MapStatic.endMarker = MapCDM.drawMarker(this.gMap, latLng, BitmapDescriptorFactory.HUE_ORANGE, "End", "Ending Point", "END");
      Graph g = MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(),
        new ArrayList<>(this.lines.values()), this.interchanges);
      g.drawTerminal(this.gMap);
      File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
      // pairs.clear();
      // pairs.add(new Pair("X", MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition()));
      try {
        if (file.exists()) {
          FileOutputStream fos = new FileOutputStream(file, true);
          String data = "S: " + MapStatic.startMarker.getPosition().toString() + " - ";
          data += "E: " + MapStatic.endMarker.getPosition().toString() + ";\n";
          fos.write(data.getBytes());
          fos.close();
        }
      } catch (Exception e) {
      }

    });

    this.gMap.setOnMarkerDragListener(this);
  }

  private static Graph buildGraph(LatLng startPos, LatLng endPos, List<Line> lines, List<Interchange> interchanges) {
    Graph.GraphPoint startPoint = null, endPoint = null;
    Line startingLine = null, endingLine = null;
    double startPointDistance = Double.MAX_VALUE;
    double endPointDistance = Double.MAX_VALUE;
    for (Line line : lines) {
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
      endingLine.addRestrictedPoint(endPoint);
      startingLine.addRestrictedPoint(startPoint);
      endingLine.buildSegments();
      if (endingLine != startingLine)
        startingLine.buildSegments();
      for (Polyline p : MapStatic.sLinePolylines) p.remove();
      for (Polyline p : MapStatic.eLinePolylines) p.remove();
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
      for (AntColony.Segment s : segments) {
        double ph = s.getPheromone();
        if (ph > max) max = ph;
        if (ph < min) min = ph;
      }
      List<Double> data = new ArrayList<>();
      for (AntColony.Segment s : segments) {
        if (s.getPheromone() == min) continue;
        sum += s.getPheromone();
        data.add(s.getPheromone());
      }
      avg = sum / data.size();
      double sxmxb = 0;
      for (int i = 0; i < data.size(); i++) {
        sxmxb += Math.pow(data.get(i) - avg, 2);
      }
      double sd = Math.sqrt(sxmxb / data.size());
      // double onethird = (max - min) * 2 / 3;
      // double twothird = (max - min) * 5 / 6;
      for (Polyline p : MapStatic.phPolylines) p.remove();
      for (AntColony.Segment s : segments) {
        if (s.getPheromone() == min) continue;
        int color = Color.YELLOW;
        if (s.getPheromone() > (avg + sd)) color = Color.RED;
        else if (s.getPheromone() > (avg - sd)) color = Color.rgb(255, 165, 0);
        MapStatic.phPolylines.add(MapCDM.drawPolyline(this.gMap, new ArrayList<>(Arrays.asList(s.getSourceLatLng(), s.getEndLatLng())), color));
      }
    });
  }

  @Override
  public void onMarkerDrag(@NonNull Marker marker) {
  }

  @Override
  public void onMarkerDragEnd(@NonNull Marker marker) {
    // if (marker.getTag() == "END") Toast.makeText(MainActivity.this, "End", Toast.LENGTH_SHORT).show();
    // if (marker.getTag() == "START") Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
    Graph g = MainActivity.buildGraph(MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition(), new ArrayList<>(this.lines.values()), this.interchanges);
    for (Polyline polyline : MapStatic.solutionPolylines) polyline.remove();

    pairs.clear();
    pairs.add(new Pair("X", MapStatic.startMarker.getPosition(), MapStatic.endMarker.getPosition()));
    String startLine = this.lines.get(g.getStartPoint().getIdLine()).getName();
    String endLine = this.lines.get(g.getEndPoint().getIdLine()).getName();
    Toast.makeText(this, startLine + "/" + endLine, Toast.LENGTH_SHORT).show();
    File file = new File(Environment.getExternalStorageDirectory(), "dijkstra-test.csv");
    try {
      if (file.exists()) {
        FileOutputStream fos = new FileOutputStream(file, true);
        String data = "S: " + MapStatic.startMarker.getPosition().toString() + " - ";
        data += "E: " + MapStatic.endMarker.getPosition().toString() + ";\n";
        fos.write(data.getBytes());
        fos.close();
      }
    } catch (Exception e) {
    }
  }

  @Override
  public void onMarkerDragStart(@NonNull Marker marker) {
  }
}