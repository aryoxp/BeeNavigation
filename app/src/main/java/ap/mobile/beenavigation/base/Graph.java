package ap.mobile.beenavigation.base;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ap.mobile.beenavigation.MapCDM;
import ap.mobile.beenavigation.R;
import ap.mobile.beenavigation.util.CDM;
import ap.mobile.beenavigation.util.Helper;

public class Graph {

  private GoogleMap gMap;
  private HashMap<Integer, Line> lines = new HashMap<>();
  private HashMap<String, GraphPoint> points = new HashMap<>();
  private List<GraphSegment> segments = new ArrayList<>();
  private List<Polyline> graphPolylines = new ArrayList<>();
  private GraphPoint startPoint;
  private GraphPoint endPoint;

  public Graph(@NonNull List<Line> lines, List<Interchange> interchanges) {
    for (Line line: lines) this.lines.put(line.getId(), line);
    // for (Interchange i: interchanges) {
    //   if (i.getPoints().size() == 0) continue;
    //   points.put(i.getIdInterchange(), new GraphPoint(i));
    // }
    // for (Line line: lines) {
    //   if (line.getSegments().size() == 0) continue;
    //   for(Line.Segment s: line.getSegments()) {
    //     GraphPoint ps = points.get(s.start().isInterchange() ? "i" + s.start().getIdInterchange() : s.start().getId());
    //     GraphPoint pe = points.get(s.end().isInterchange() ? "i" + s.end().getIdInterchange() : s.end().getId());
    //     if (ps == null) ps = new GraphPoint(s.start());
    //     if (pe == null) pe = new GraphPoint(s.end());
    //     ps.addNext(pe);
    //     points.put(ps.getId(), ps);
    //     points.put(pe.getId(), pe);
    //     GraphSegment segment = new GraphSegment(ps, pe, line);
    //     this.segments.add(segment);
    //   }
    // }


    for(Line l: lines) {
      // this.segments.addAll(l.getSegments());
      for(Line.Segment s: l.segments) {
        GraphPoint ps = points.get(s.start().id);
        GraphPoint pe = points.get(s.end().id);
        if (ps == null) ps = new GraphPoint(s.start());
        if (pe == null) pe = new GraphPoint(s.end());
        this.segments.add(new GraphSegment(ps, pe, l));
        // ps.setCost(Double.MAX_VALUE);
        // pe.setCost(Double.MAX_VALUE);
        ps.addNext(pe);
        points.put(ps.id, ps);
        points.put(pe.id, pe);
      }
    }
    for(Interchange interchange: interchanges) {
      for(Point spoint: interchange.getPoints()) {
        for(Point epoint: interchange.getPoints()) {
          if (spoint.id.equals(epoint.id)) continue;
          GraphPoint sp = points.get(spoint.id);
          GraphPoint ep = points.get(epoint.id);
          if (sp != null && ep != null) {
            sp.addNext(ep);
            Line.Segment s = new Line.Segment(0);
            s.points.put(sp.id, sp);
            s.points.put(ep.id, ep);
            s.path.put(1, sp);
            s.path.put(2, ep);
            this.segments.add(new GraphSegment(sp, ep, new Line(0, "Walk", "O", Color.GRAY)));
            // this.segments.add(s);
          }
          // else if (sp != null) MapCDM.drawInterchangeMarker(this.gMap, sp.getLatLng(), R.drawable.ic_directions_black_24dp);
          // else if (ep != null) MapCDM.drawInterchangeMarker(this.gMap, ep.getLatLng(), R.drawable.ic_directions_bus_black_24dp);
        }
      }
    }
  }

  public Graph(@NonNull List<Line> lines, List<Interchange> interchanges, GoogleMap gMap) {
    this(lines, interchanges);
    this.gMap = gMap;
  }

  public List<Polyline> draw(HashMap<Integer, Line> lines) {
    for(Polyline p: this.graphPolylines) p.remove();
    for(GraphPoint p: this.points.values()) {
      for(GraphPoint np: p.nextPoints) {
        int color = Color.BLACK;
        if (p.idLine == np.idLine) color = lines.get(p.idLine).getColor();
        this.graphPolylines.add(MapCDM.drawPolyline(this.gMap, Arrays.asList(p.getLatLng(), np.getLatLng()), color));
      }
    }
    return this.graphPolylines;
  }
  public List<Polyline> draw() {
    for(Polyline p: this.graphPolylines) p.remove();
    for(GraphSegment s: this.segments) {
      this.graphPolylines.add(MapCDM.drawPolyline(this.gMap, Arrays.asList(s.start().getLatLng(), s.end().getLatLng()), s.getLine().getColor()));
    }
    return this.graphPolylines;
  }

  public void clearDrawing() {
    for(Polyline p: this.graphPolylines) p.remove();
  }

  public List<GraphSegment> getSegments() {
    return this.segments;
  }
  public GraphPoint getPoint(String id) {
    return this.points.get(id);
  }
  public HashMap<String, GraphPoint> getPoints() {
    return this.points;
  }
  public Line getLine(int lineId) {
    return this.lines.get(lineId);
  }

  public void setStartEnd(GraphPoint startPoint, GraphPoint endPoint) {
    this.startPoint = startPoint;
    this.endPoint = endPoint;
  }

  public void drawTerminal(GoogleMap gMap) {
    if (MapStatic.startPointMarker != null) MapStatic.startPointMarker.remove();
    if (MapStatic.endPointMarker != null) MapStatic.endPointMarker.remove();
    MapStatic.startPointMarker = MapCDM.drawInterchangeMarker(gMap, this.startPoint.getLatLng(), R.drawable.ic_circle);
    MapStatic.endPointMarker = MapCDM.drawInterchangeMarker(gMap, this.endPoint.getLatLng(), R.drawable.ic_circle);
  }

  public GraphPoint getStartPoint() {
    return this.startPoint;
  }

  public GraphPoint getEndPoint() {
    return this.endPoint;
  }


  public static class GraphPoint extends Point {

    Set<GraphPoint> nextPoints = new HashSet<>();
    Set<GraphPoint> prevPoints = new HashSet<>();

    public boolean marked = false;

    public GraphPoint() {}

    public GraphPoint(Point p) {
      this.id = p.id;
      this.idLine = p.idLine;
      this.sequence = p.sequence;
      this.stop = p.stop;
      this.lat = p.lat;
      this.lng = p.lng;
      this.idInterchange = p.idInterchange;
    }

    public GraphPoint(Interchange i) {
      this.id = i.getIdInterchange();
      this.stop = true;
      for(Point p: i.getPoints()) {
        this.lat += p.getLat();
        this.lng += p.getLng();
      }
      this.lat = this.lat / i.getPoints().size();
      this.lng = this.lng / i.getPoints().size();
      this.idInterchange = i.getIdInterchange();
    }

    public void addNext(GraphPoint p) {
      this.nextPoints.add(p);
      p.prevPoints.add(this);
    }

    public boolean hasNext() {
      return !nextPoints.isEmpty();
    }

    public boolean hasPrev() {
      return !prevPoints.isEmpty();
    }

    public Set<GraphPoint> getNextPoints() {
      return this.nextPoints;
    }

    public Set<GraphPoint> getPrevPoints() {
      return this.prevPoints;
    }

  }
  public static class GraphSegment {
    private final Line line;
    private final GraphPoint start;
    private final GraphPoint end;
    private final double distance;

    public GraphSegment(GraphPoint start, GraphPoint end, Line line) {
      this.start = start;
      this.end = end;
      this.distance = Helper.calculateDistance(start.getLatLng(), end.getLatLng());
      this.line = line;
    }

    public GraphPoint start() { return this.start; }
    public GraphPoint end() { return this.end; }
    public double getDistance() { return this.distance; }
    public Line getLine() { return this.line; }
  }
}
