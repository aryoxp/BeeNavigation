package ap.mobile.beenavigation.lib;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;
import ap.mobile.beenavigation.util.CDM;
import ap.mobile.beenavigation.util.Helper;

public class Dijkstra {

  private Point start, end;
  private Graph g;
  private List<Point> solution;
  private HashMap<String, Point> points;

  public enum Priority {
    COST,
    DISTANCE
  }

  public Dijkstra(Graph g, GraphPoint start, GraphPoint end) {
    this.g = g;

    // Clone Graph,
    // Dijkstra adds cost and remembers the shortest prior point
    this.points = new HashMap<>();
    for(GraphPoint p: g.getPoints().values()) {
      this.points.put(p.getId(), new Point(p));
    }
    for(Point p: this.points.values())
      for (GraphPoint gp: p.point.getNextPoints()) {
        Point np = this.points.get(gp.getId());
        p.addNext(np);
      }

    this.start = this.points.get(start.getId());
    this.end = this.points.get(end.getId());
  }

  public List<LatLng> run(Priority priority) {
    this.start.setCost(0D);
    Set<Point> settled = new HashSet<>();
    Set<Point> unsettled = new HashSet<>();
    unsettled.add(this.start);
    Point current = null;
    while (!unsettled.isEmpty()) {
      current = null;
      double minCost = Double.MAX_VALUE;
      for (Point un : unsettled) {
        if (un.getCost() < minCost) {
          current = un;
          minCost = un.getCost();
        }
      }

      unsettled.remove(current);
      assert current != null;
      Set<Point> nexts = current.getNextPoints();
      for (Point next : nexts) {

        double edgeCost = (priority == Priority.COST) ?
          ((current.getIdLine() == next.getIdLine()) ? 0 : CDM.cost) :
          (Helper.calculateDistance(current.getLatLng(), next.getLatLng()));

        if (!settled.contains(next)) {
          if (current.getCost() + edgeCost < next.getCost()) {
            next.setCost(current.getCost() + edgeCost);
            next.cheapestSource = current;
          }
          unsettled.add(next);
        }
      }
      settled.add(current);
    }
    Point backtrack = this.end;
    List<Point> solution = new ArrayList<>();
    while (backtrack.cheapestSource != null) {
      solution.add(backtrack);
      backtrack = backtrack.cheapestSource;
    }
    if (backtrack.getIdLine() == this.start.getIdLine())
      solution.add(this.start);
    Collections.reverse(solution);
    this.solution = solution;
    List<LatLng> latLngs = new ArrayList<>();
    for(Point p: solution) latLngs.add(p.getLatLng());
    return latLngs;
  }

  public List<GraphPoint> getSolution() {
    // Revert back Dijkstra Point to GraphPoint
    List<GraphPoint> solution = new ArrayList<>();
    for(Point p: this.solution)
      solution.add(this.g.getPoints().get(p.getId()));
    return solution;
  }

  public double getCost() {
    double cost = 0;
    Point prev = null;
    for(Point p: this.solution) {
      if (prev == null) {
        cost += CDM.cost;
        prev = p;
        continue;
      }
      if (prev.getIdLine() != p.getIdLine()) cost += CDM.cost;
      prev = p;
    }
    return cost;
  }

  private class Point {
    private final GraphPoint point;
    private final Set<Point> nextPoints = new HashSet<>();
    private final Set<Point> prevPoints = new HashSet<>();

    private double cost = Double.MAX_VALUE;
    private Point cheapestSource;

    Point(GraphPoint p) {
      this.point = p;
    }
    private void addNext(Point p) {
      this.nextPoints.add(p);
      p.prevPoints.add(this);
    }
    private Set<Point> getNextPoints() {
      return this.nextPoints;
    }
    private Set<Point> getPrevPoints() { return this.prevPoints; }
    private double getCost() {
      return this.cost;
    }
    private void setCost(double cost) {
      this.cost = cost;
    }
    private Point getCheapestSource() {
      return this.cheapestSource;
    }
    private void setCheapestSource(Point p) {
      this.cheapestSource = p;
    }

    private String getId() {
      return this.point.getId();
    }
    private LatLng getLatLng() {
      return this.point.getLatLng();
    }
    private int getIdLine() {
      return this.point.getIdLine();
    }
  }

}
