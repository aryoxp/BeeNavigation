package ap.mobile.beenavigation.base;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Interchange extends Point {

  protected String idInterchange;
  protected String name;
  protected HashMap<String, Point> points;
  protected HashMap<String, Graph.GraphPoint> hub;

  public Interchange(String idInterchange, String name) {
    this.idInterchange = idInterchange;
    this.name = name;
    this.points = new HashMap<>();
    this.hub = new HashMap<>();
  }

  public String getId() { return this.idInterchange; }
  public String getIdInterchange() { return this.idInterchange; }
  public String getName() { return this.name; }
  public List<Point> getPoints() {
    return new ArrayList<>(this.points.values());
  }
  public boolean hasPointId(String id) {
    return this.points.containsKey(id);
  }
  public void addPoint(Point point) {
    this.points.put(point.getId(), point);
  }
  public void addHubPoint(Graph.GraphPoint point) { this.hub.put(point.getId(), point); }
  public boolean hasHubPoint(String id) { return this.hub.containsKey(id); }

  @Override
  public LatLng getLatLng() {
    double lat = 0, lng = 0;
    for(Point p: this.points.values()) {
      lat += p.getLat();
      lng += p.getLng();
    }
    return new LatLng(lat/this.getPoints().size(), lng/this.getPoints().size());
  }
}
