package ap.mobile.beenavigation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.HashMap;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Interchange;
import ap.mobile.beenavigation.base.Line;
import ap.mobile.beenavigation.base.Point;
import ap.mobile.beenavigation.util.Helper;

public class MainActivity extends AppCompatActivity
    implements MapCDM.Callback, MapCDM.IServiceInterface {

  private GoogleMap gMap;
  private MapCDM mapCDM;

  private Marker startMarker;
  private Marker startingPointMarker;

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
    Graph g = new Graph(new ArrayList<>(lines.values()), interchanges, this.gMap);
    // g.draw(lines);
    for(Line line: lines.values()) {
      line.drawSegments(this.gMap);
      line.drawStops(this.gMap);
    }

    this.gMap.setOnMapClickListener(latLng -> {
      if (this.startMarker != null) this.startMarker.remove();
      this.startMarker = MapCDM.drawMarker(this.gMap, latLng, BitmapDescriptorFactory.HUE_ORANGE, "Start", "Starting Point");
      Point startingPoint = null;
      Line startingLine = null;
      double startingPointDistance = Double.MAX_VALUE;
      for(Line line: lines.values()) {
        Point check = line.getNearestPoint(latLng);
        if (check == null) continue;
        double checkDistance = Helper.calculateDistance(check.getLatLng(), latLng);
        if (startingPoint == null || checkDistance < startingPointDistance) {
          startingPoint = check;
          startingPointDistance = checkDistance;
          startingLine = line;
        }
        if (line.hasRestrictedPoints()) {
          line.clearRestrictedPoints();
          line.buildSegments();
          line.drawSegments(this.gMap);
        }
      }
      if (this.startingPointMarker != null) this.startingPointMarker.remove();
      assert startingPoint != null;
      this.startingPointMarker = MapCDM.drawInterchangeMarker(this.gMap, startingPoint.getLatLng(), R.drawable.ic_circle);
      startingLine.addRestrictedPoint(startingPoint);
      startingLine.buildSegments();
      startingLine.drawSegments(this.gMap);
    });
  }

}