package ap.mobile.beenavigation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Interchange;
import ap.mobile.beenavigation.base.Line;
import ap.mobile.beenavigation.base.Point;
import ap.mobile.beenavigation.lib.BeeColony;
import ap.mobile.beenavigation.util.Helper;

public class MainActivity extends AppCompatActivity
    implements MapCDM.Callback, MapCDM.IServiceInterface {

  private GoogleMap gMap;
  private MapCDM mapCDM;

  private Marker endMarker;
  private Marker startMarker;
  private Marker startPointMarker;
  private Marker endPointMarker;

  private Graph g;
  private Graph.GraphPoint startPoint, endPoint;
  private Polyline cPolyline;
  private List<Polyline> eLinePolylines = new ArrayList<>();
  private List<Polyline> sLinePolylines = new ArrayList<>();

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
    if (item.getItemId() == R.id.action_test) {
      if (this.g == null) return false;
      if (this.startPoint != null && this.endPoint != null) {
        BeeColony beeColony = new BeeColony(this.g, this.startPoint, this.endPoint);
        Handler h = new Handler(Looper.getMainLooper());
        Thread t = new Thread(() -> {
          List<LatLng> path = beeColony.run(10);
          h.post(() -> {
            if (this.cPolyline != null) this.cPolyline.remove();
            this.cPolyline = MapCDM.drawPolyline(this.gMap, path, Color.RED);
          });
        });
        t.start();
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

    this.gMap.setOnMapClickListener(latLng -> {
      if (this.startMarker != null) this.startMarker.remove();
      if (this.endMarker != null) this.endMarker.remove();
      this.startMarker = MapCDM.drawMarker(this.gMap, MapCDM.getCurrentLocation(), BitmapDescriptorFactory.HUE_AZURE, "Start", "Starting Point");
      this.endMarker = MapCDM.drawMarker(this.gMap, latLng, BitmapDescriptorFactory.HUE_ORANGE, "End", "Ending Point");
      Graph.GraphPoint startPoint = null, endPoint = null;
      Line startingLine = null, endingLine = null;
      double startPointDistance = Double.MAX_VALUE;
      double endPointDistance = Double.MAX_VALUE;
      for(Line line: lines.values()) {
        Point checkStart = line.getNearestPoint(MapCDM.getCurrentLocation());
        Point checkEnd = line.getNearestPoint(latLng);

        if (checkEnd == null || checkStart == null) continue;

        double checkStartDistance = Helper.calculateDistance(checkStart.getLatLng(), MapCDM.getCurrentLocation());
        double checkEndDistance = Helper.calculateDistance(checkEnd.getLatLng(), latLng);

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
        this.sLinePolylines = startingLine.drawSegments(this.gMap);
        this.eLinePolylines = endingLine.drawSegments(this.gMap);
        // Recreate the graph segments, including the starting and ending points
        this.g = new Graph(new ArrayList<>(lines.values()), interchanges, this.gMap);
      }

    });
  }

}