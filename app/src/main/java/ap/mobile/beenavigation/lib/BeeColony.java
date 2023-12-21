package ap.mobile.beenavigation.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;

public class BeeColony {

  private final Graph g;

  public BeeColony(Graph g) {
    this.g = g;
  }

  public void init(int numBee) {

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
