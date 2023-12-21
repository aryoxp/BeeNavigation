package ap.mobile.beenavigation.base;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ap.mobile.beenavigation.MapCDM;
import ap.mobile.beenavigation.R;
import ap.mobile.beenavigation.util.Helper;

public class Line {

  protected int id, color;
  protected String name;
  protected Direction direction;
  protected SortedMap<Integer, Point> path = new TreeMap<>();
  protected HashMap<Integer, Point> points = new HashMap<>();
  protected List<Segment> segments = new ArrayList<>();
  protected Set<Point> stops = new HashSet<>();
  protected Set<Point> interchanges = new HashSet<>();
  protected Set<Point> restrictedPoints = new HashSet<>();

  private Polyline pathPolyline;
  private List<Polyline> segmentPolylines = new ArrayList<>();
  private List<Marker> stopMarkers = new ArrayList<>();

  public Line(int id, String name, String direction, int color) {
    this.id = id;
    this.name = name;
    this.direction = direction.equals("I") ? Direction.INBOUND : Direction.OUTBOUND;
    this.color = color;
  }

  public int getId() { return this.id; }
  public int getColor() { return this.color; }
  public String getName() { return this.name; }
  public Direction getDirection() { return this.direction; }
  public SortedMap<Integer, Point> getPath() { return this.path; }
  public HashMap<Integer, Point> getPoints() { return this.points; }
  public List<Point> getPathList() { return new ArrayList<>(path.values()); }
  public List<Segment> getSegments() { return this.segments; }
  public Set<Point> getStops() { return this.stops; }
  public Set<Point> getInterchanges() { return this.interchanges; }
  public Set<Point> getRestrictedPoints() { return this.restrictedPoints; }
  public boolean hasRestrictedPoints() { return this.restrictedPoints.size() > 0; }

  public void addPoint(int sequence, Point point) {
    this.path.put(sequence, point);
    this.points.put(point.id, point);
    if (point.isStop()) this.stops.add(point);
    if (point.isInterchange()) this.interchanges.add(point);
  }

  public void addRestrictedPoint(Point p) {
    if(this.points.get(p.id) != null)
      this.restrictedPoints.add(this.points.get(p.id));
  }

  public void removeRestrictedPoint(Point p) {
    Point restrictedPoint = this.points.get(p.id);
    if (restrictedPoint != null)
      this.restrictedPoints.remove(restrictedPoint);
  }

  public void clearRestrictedPoints() {
    this.restrictedPoints.clear();
  }

  public void buildSegments() {
    List<Segment> segments = new ArrayList<>();
    Segment s = new Segment();
    ArrayList<Point> points = new ArrayList<>(this.getPathList());
    for(int i = 0; i < this.path.size(); i++) {
      Point p = points.get(i);
      s.path.put(p.getSequence(), p);
      s.points.put(p.id, p);
      if (i != 0 && (p.isStop() || this.restrictedPoints.contains(p))) {
        segments.add(s);
        s = new Segment();
        s.path.put(p.getSequence(), p);
        s.points.put(p.id, p);
      }
    }
    if (s.path.size() > 1) segments.add(s);
    this.segments = segments;
  }

  public Point getNearestPoint(LatLng position) {
    double d = Double.MAX_VALUE;
    Point point = null;
    for(Point p: this.points.values()) {
      double check = Helper.calculateDistance(p.getLatLng(), position);
      if(point == null || check < d) {
        point = p;
        d = check;
      }
    }
    return point;
  }

  public static List<LatLng> toLatLngs(List<Point> points) {
    List<LatLng> latLngs = new ArrayList<>();
    for(Point p: points) latLngs.add(p.getLatLng());
    return latLngs;
  }

  public void drawPath(GoogleMap gMap) {
    this.pathPolyline = MapCDM.drawPolyline(gMap, Line.toLatLngs(this.getPathList()), this.getColor());
  }

  public void drawSegments(GoogleMap gMap) {
    for(Polyline p: this.segmentPolylines) p.remove();
    for(Segment s: this.getSegments()) {
      List<LatLng> edge = new ArrayList<>(Arrays.asList(s.start().getLatLng(), s.end().getLatLng()));
      this.segmentPolylines.add(MapCDM.drawPolyline(gMap, edge, this.getColor()));
    }
  }

  public void drawStops(GoogleMap gMap) {
    for(Point p: this.getStops())
      this.stopMarkers.add(MapCDM.drawInterchangeMarker(gMap, p.getLatLng(), R.drawable.ic_circle));
  }

  public enum Direction {
    INBOUND,
    OUTBOUND
  }

  public static class Segment {
    SortedMap<Integer, Point> path = new TreeMap<>();
    HashMap<Integer, Point> points = new HashMap<>();
    public Point start() {
      return this.path.get(this.path.firstKey());
    }
    public Point end() {
      return this.path.get(this.path.lastKey());
    }
  }
}
