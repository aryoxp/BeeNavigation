package ap.mobile.beenavigation.base;

import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ap.mobile.beenavigation.MapCDM;
import ap.mobile.beenavigation.R;
import ap.mobile.beenavigation.util.Helper;

public class Graph {

  private final GoogleMap gMap;
  private HashMap<Integer, GraphPoint> points = new HashMap<>();

  public Graph(List<Line> lines, List<Interchange> interchanges, GoogleMap gMap) {
    this.gMap = gMap;
    for(Line l: lines) {
      for(Line.Segment s: l.segments) {
        GraphPoint ps = points.get(s.start().id);
        GraphPoint pe = points.get(s.end().id);
        if (ps == null) ps = new GraphPoint(s.start());
        if (pe == null) pe = new GraphPoint(s.end());
        ps.addNext(pe);
        points.put(ps.id, ps);
        points.put(pe.id, pe);
      }
    }
    for(Interchange interchange: interchanges) {
      for(Point spoint: interchange.getPoints()) {
        for(Point epoint: interchange.getPoints()) {
          if (spoint.id == epoint.id) continue;
          GraphPoint sp = points.get(spoint.id);
          GraphPoint ep = points.get(epoint.id);
          if (sp != null && ep != null)
            sp.addNext(ep);
          // else if (sp != null) MapCDM.drawInterchangeMarker(this.gMap, sp.getLatLng(), R.drawable.ic_directions_black_24dp);
          // else if (ep != null) MapCDM.drawInterchangeMarker(this.gMap, ep.getLatLng(), R.drawable.ic_directions_bus_black_24dp);
        }
      }
    }
  }

  public void draw(HashMap<Integer, Line> lines) {
    for(GraphPoint p: this.points.values()) {
      for(GraphPoint np: p.nextPoints) {
        int color = Color.BLACK;
        if (p.idLine == np.idLine) color = lines.get(p.idLine).getColor();
        MapCDM.drawPolyline(this.gMap, Arrays.asList(p.getLatLng(), np.getLatLng()), color);
      }
    }
  }

  public GraphPoint getPoint(int id) {
    return this.points.get(id);
  }

  public static class GraphPoint extends Point {

    Set<GraphPoint> nextPoints = new HashSet<>();
    Set<GraphPoint> prevPoints = new HashSet<>();

    public GraphPoint(Point p) {
      this.id = p.id;
      this.idLine = p.idLine;
      this.sequence = p.sequence;
      this.stop = p.stop;
      this.lat = p.lat;
      this.lng = p.lng;
      this.idInterchange = p.idInterchange;
    }

    public void addNext(GraphPoint p) {
      this.nextPoints.add(p);
      p.prevPoints.add(this);
    }

    public boolean hasNext() {
      return nextPoints.size() > 0;
    }

    public boolean hasPrev() {
      return prevPoints.size() > 0;
    }

    public Set<GraphPoint> getNextPoints() {
      return this.nextPoints;
    }

    public Set<GraphPoint> getPrevPoints() {
      return this.prevPoints;
    }
  }
}
