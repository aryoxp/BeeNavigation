package ap.mobile.beenavigation.lib;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;
import ap.mobile.beenavigation.base.Point;
import ap.mobile.beenavigation.util.Helper;

public class AntColony {
  private final Graph.GraphPoint start;
  private final Graph.GraphPoint end;
  private final List<Segment> segments = new ArrayList<>();
  private final List<Ant> colony = new ArrayList<>();

  // Parameters

  private static final int numCycle = 100;
  private static final float rho = .5f; // evaporation rate
  private final AntColonyEvent listener;

  public AntColony(Graph g, GraphPoint start, GraphPoint end) {
    this(g, start, end, null);
  }

  public AntColony(Graph g, GraphPoint start, GraphPoint end, AntColonyEvent listener) {
    this.start = g.getPoint(start.getId());
    this.end = g.getPoint(end.getId());
    this.listener = listener;

    // Initialize segments and its pheromone level of a Graph
    for(Graph.GraphSegment s: g.getSegments())
      this.segments.add(new Segment(s.start().getId(), s.end().getId(), s.start(), s.end()));
  }

  public List<LatLng> run(int numAnts) {
    this.resetPheromones();
    for (int i = 0; i < numAnts; i++)
      this.colony.add(new Ant());
    int cycle = 1;
    List<GraphPoint> bestPath = null;
    double bestFitness = Double.MAX_VALUE;
    int bestCycle = 0;
    while (cycle <= AntColony.numCycle) {
      double min = Double.MAX_VALUE;
      double max = 0;
      double gap = 0;
      double sumFitness = 0;
      Ant bestAnt = null;
      for(Ant ant: this.colony) {
        ant.search();
        Log.d("ANT", "F" + ant.fitness + " P " + ant.path.size());
        sumFitness += ant.fitness;
        if (ant.fitness > max) {
          max = ant.fitness;
        }
        if (ant.fitness < min) {
          min = ant.fitness;
          bestAnt = ant;
        }
        if (ant.fitness < bestFitness) {
          bestPath = new ArrayList<>(ant.path);
          bestFitness = ant.fitness;
          bestCycle = cycle;
          Log.e("BEST-ANT", bestFitness + "/" + bestPath.size() + "@" + bestCycle);
        }
      }
      // fitness /= this.colony.size();
      gap = max-min;
      this.updatePheromones(bestAnt); // Update pheromones (+trace -evaporation)
      Log.d("ANT-CYCLE", cycle + "/" + AntColony.numCycle + ":" + sumFitness/this.colony.size() + "/" + gap);
      // if (gap == 0) break;
      cycle++;
    }
    if (this.listener != null) this.listener.onCycle(cycle, this.segments);
    // return getPath();
    Log.d("BEST-ANT", bestFitness + "/" + bestPath.size() + "@" + bestCycle);
    List<LatLng> result = new ArrayList<>();
    for(GraphPoint p: bestPath) result.add(new LatLng(p.getLat(), p.getLng()));
    return result;
  }

  private void resetPheromones() {
    for(Segment s: this.segments) s.resetPheromone();
  }

  private List<LatLng> getPath() {
    List<LatLng> path = new ArrayList<>();
    GraphPoint current = this.start;
    while(current != this.end && current != null) {
      path.add(new LatLng(current.getLat(), current.getLng()));
      Set<GraphPoint> nextPoints = current.getNextPoints();
      double highestPheromones = 0;
      GraphPoint nextPoint = null;
      for(GraphPoint next: nextPoints) {
        if (next == null) continue;
        Segment s = this.getSegment(current.getId(), next.getId());
        if (s == null) continue;
        double pheromone = s.getPheromone();
        if (pheromone > highestPheromones) {
          highestPheromones = pheromone;
          nextPoint = next;
        }
      }
      current = nextPoint;
    }
    if (current != null)
      path.add(new LatLng(current.getLat(), current.getLng()));
    return path;
  }

  private void updatePheromones(Ant bestAnt) {
    for(Segment s: this.segments) {
      // double sumAntPheromone = 0;
      // for(Ant ant: this.colony)
      // sumAntPheromone += bestAnt.passSegment(s);
      s.updatePheromone(bestAnt.passSegment(s));
      // if (sumAntPheromone != 0)
      //   Log.d("PHEROMONE", sumAntPheromone + "/" + s.getPheromone());
    }
  }

  private Segment getSegment(String source, String destination) {
    for(Segment s: this.segments)
      if (Objects.equals(s.source, source) && Objects.equals(s.destination, destination)) return s;
    return null;
  }

  public static class Segment {
    private final Point sourcePoint;
    private final Point endPoint;
    String source, destination;
    private double pheromone;

    public Segment(String source, String destination, Point sourcePoint, Point endPoint) {
      this.source = source;
      this.destination = destination;
      this.pheromone = 1.0d;
      this.sourcePoint = sourcePoint;
      this.endPoint = endPoint;
    }
    public void updatePheromone(double sumAntPheromone) {
      this.pheromone = this.pheromone * (1-AntColony.rho) + sumAntPheromone;
    }
    public double getPheromone() {
      return this.pheromone;
    }
    public LatLng getSourceLatLng() {
      return this.sourcePoint.getLatLng();
    }
    public LatLng getEndLatLng() {
      return this.endPoint.getLatLng();
    }
    public void resetPheromone() { this.pheromone = 1.0d; }
  }

  private class Ant {
    private final List<GraphPoint> path = new ArrayList<>();
    private double fitness;
    private List<GraphPoint> taboo;
    private double L = 0;

    public double getFitness() { return this.fitness; }

    private void search() {
      this.path.clear();
      this.path.add(start);
      this.taboo = new ArrayList<>();
      this.fitness = 0;
      this.L = 0;
      GraphPoint current = start;
      while(current != null && current != end) {
        GraphPoint nextPoint = this.getNext(current);
        if (nextPoint == null) {
          this.taboo.add(this.path.remove(this.path.size()-1));
          if (this.path.size() == 0) current = null;
          else current = this.path.get(this.path.size()-1);
        } else {
          this.path.add(nextPoint);
          current = nextPoint;
        }
      }
      this.calculateFitness();
    }
    private boolean avoid(GraphPoint np) {
      return this.path.contains(np) || this.taboo.contains(np) || isTurnAround(np);
    }
    private boolean isTurnAround(GraphPoint np) {
      if (this.path.size() > 2) {
        Set<GraphPoint> pps = this.path.get(this.path.size() - 2).getNextPoints();
        return pps.contains(np);
      }
      return false;
    }
    private GraphPoint getNext(GraphPoint point) {
      List<GraphPoint> nextPoints = new ArrayList<>(point.getNextPoints());
      nextPoints.removeIf(this::avoid);
      if (nextPoints.size() == 0) return null;
      return rouletteWheel(point, nextPoints);
    }
    private GraphPoint rouletteWheel(GraphPoint point, List<GraphPoint> nextPoints) {
      if (nextPoints.size() == 1) return nextPoints.get(0);
      double tPh = 0;
      List<Segment> rouletteSegments = new ArrayList<>();
      for(GraphPoint p: nextPoints) {
        Segment s = getSegment(point.getId(), p.getId());
        tPh += (s != null) ? s.getPheromone() : 0;
        if (s != null) rouletteSegments.add(s);
      }
      double sum = 0;
      double rand = Math.random(); // [0,1]
      int selectedIndex = 0;
      for(int i = 0; i < rouletteSegments.size(); i++) {
        sum += rouletteSegments.get(0).pheromone / tPh;
        if(rand <= sum) {
          selectedIndex = i;
          break;
        }
      }
      // int index = new Random().nextInt(nextPoints.size());
      return nextPoints.get(selectedIndex);
    }
    private void calculateFitness() {
      this.fitness = this.calculateFitness(this.path) * 100;
    }
    private double calculateFitness(List<GraphPoint> path) {
      GraphPoint prev = null;
      // double cost = 0;
      for(GraphPoint p: path) {
        if (prev == null) {
          prev = p;
          // cost = CDM.cost;
          continue;
        }
        // if (p.getIdLine() != prev.getIdLine()) cost += CDM.cost;
        // cost += Helper.calculateDistance(p.getLatLng(), prev.getLatLng());
        this.L += Helper.calculateDistance(p.getLatLng(), prev.getLatLng());
        prev = p;
      }
      // return (double) 1 / (1 + cost);
      // return (double) 1/(1+this.L);
      return this.L;
    }
    public double passSegment(Segment s) {
      // if this ant passes the specified segment
      // it leaves (returns) its pheromone on that segment
      GraphPoint prev = null;
      for(GraphPoint p: path) {
        if (prev == null) {
          prev = p;
          continue;
        }
        if (Objects.equals(prev.getId(), s.source) && Objects.equals(p.getId(), s.destination)) return 1 / this.getFitness();
        prev = p;
      }
      return 0;
    }
  }

  public interface AntColonyEvent {
    void onCycle(int iteration, List<Segment> segments);
  }
}
