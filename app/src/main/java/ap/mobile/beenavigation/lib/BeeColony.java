package ap.mobile.beenavigation.lib;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ap.mobile.beenavigation.MapCDM;
import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;

public class BeeColony {

  private Graph g;
  private GraphPoint start;
  private GraphPoint end;
  private Bee bee;


  public BeeColony(Graph g, GraphPoint start, GraphPoint end) {
    this.g = g;
    this.start = g.getPoint(start.getId());
    this.end = g.getPoint(end.getId());
  }

  public void init(int numBee) {
    Bee bee = new Bee(Bee.Type.SCOUT);
    bee.scout(this.start, this.end);
    this.bee = bee;
  }

  public List<LatLng> getPath() {
    List<LatLng> path = new ArrayList<>();
    for(Graph.GraphPoint point: this.bee.food.path) {
      path.add(new LatLng(point.getLat(), point.getLng()));
    }
    return path;
  }

  private static class Bee {
    enum Type {
      EMPLOYED,
      ONLOOKER,
      SCOUT
    }
    Type type = Type.SCOUT;
    Food food = new Food();

    public Bee(Type type) {
      this.type = type;
    }

    public void scout(GraphPoint start, GraphPoint end) {
      this.food = new Food().build(start, end);
    }
  }

  private static class Food {
    List<GraphPoint> path = new ArrayList<>();
    Set<GraphPoint> taboo = new HashSet<>();
    public Food() {

    }
    private boolean avoid(GraphPoint np) {
      return this.path.contains(np) || this.taboo.contains(np);
    }
    public Food build(GraphPoint start, GraphPoint end) {
      Set<GraphPoint> visited = new HashSet<>();
      this.path.add(start);
      GraphPoint current = start;
      while(current != null && current != end && this.hasNext(current)) {
        GraphPoint nextPoint = this.getNext(current);
        if (nextPoint == null) {
          this.taboo.add(this.removeLast());
          current = this.getLast();
        } else {
          this.path.add(nextPoint);
          current = nextPoint;
        }
      }
      if (current == end)
        Log.e("END", "END");
      return this;
    }
    private List<GraphPoint> getNextAvailablePoints(GraphPoint point) {
      List<GraphPoint> nextPoints = new ArrayList<>(point.getNextPoints());
      nextPoints.removeIf(this::avoid);
      return nextPoints;
    }
    private boolean hasNext(GraphPoint point) {
      return this.getNextAvailablePoints(point).size() > 0;
    }
    public GraphPoint getNext(GraphPoint point) {
      if (this.hasNext(point)) {
        List<GraphPoint> nextPoints = this.getNextAvailablePoints(point);
        int index = new Random().nextInt(nextPoints.size());
        return nextPoints.get(index);
      }
      return null;
    }
    public GraphPoint getLast() {
      if (this.path.size() == 0) return null;
      return this.path.get(this.path.size()-1);
    }
    public GraphPoint removeLast() {
      if (this.path.size() == 0) return null;
      return this.path.remove(this.path.size()-1);
    }
  }

}
