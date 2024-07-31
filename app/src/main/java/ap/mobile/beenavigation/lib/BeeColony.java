package ap.mobile.beenavigation.lib;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;
import ap.mobile.beenavigation.util.CDM;

public class BeeColony {

  private final Point start;
  private final Point end;
  private final Graph g;
  private final Queue<Food> foods = new LinkedList<>();
  private List<Bee> colony = new LinkedList<>();
  private static int seed = 0;
  private int cycle = 0;

  private static final int cycleLimit = 50;
  private static final int forageLimit = 5;
  private static final int convergence = 25;
  private static final float percentForager = .5f;
  private static final float percentOnlooker = .5f;
  private Food bestFood;
  private static Map<Integer, Double> convergenceMetric = new HashMap<>();

  public BeeColony(Graph g, GraphPoint start, GraphPoint end) {
    this.g = g;
    // Clone Graph,
    HashMap<String, Point> points = new HashMap<>();
    for(GraphPoint p: g.getPoints().values()) {
      points.put(p.getId(), new Point(p));
    }
    for(Point p: points.values())
      for (GraphPoint gp: p.point.getNextPoints()) {
        Point np = points.get(gp.getId());
        p.addNext(np);
        if(gp.getIdLine() == p.getIdLine()) p.addNextOnLine(np);
      }
    this.start = points.get(start.getId());
    this.end = points.get(end.getId());
  }

  public List<LatLng> run(int numBee) {
    seed = 0;
    this.init(numBee);
    int cycle = 0;
    int convergence = 0;
    double bestCost = Double.MAX_VALUE;
    BeeColony.convergenceMetric.clear();
    while(cycle < BeeColony.cycleLimit && convergence < BeeColony.convergence) {

      // Employed Bee Phase
      Log.w("BEE", "EMPLOYED: Cycle: " + cycle + " Food: " + this.foods.size());
      for(Bee bee: colony)
      {
        if (bee.type == Bee.Type.EMPLOYED) {
          foods.add(bee.forage(foods.poll()));
          bee.transform(Bee.Type.SCOUT);
        }
      }

      // Waggle Dance + Onlooker Bee Phase
      Log.d("BEE", "ONLOOKER: Cycle: " + cycle);
      this.dance();
      for(Bee bee: colony) {
        if (bee.type == Bee.Type.ONLOOKER) {
          Food food = roulette((List<Food>) this.foods);
          if (food == null) continue;
          foods.add(bee.forage(food));
        }
      }
      Food cycleBestFood = this.dance();

      if (this.bestFood == null || Food.getCost(cycleBestFood.path) < Food.getCost(this.bestFood.path))
        this.bestFood = cycleBestFood;

      // Scout Bee Phase
      Log.d("BEE", "SCOUT: Cycle: " + cycle);
      this.foods.clear();
      for(Bee bee: colony) {
        if (bee.type == Bee.Type.SCOUT) {
          foods.add(bee.scout(this.start, this.end));
          bee.transform(Bee.Type.EMPLOYED);
        }
      }
      // Log.d("CYCLE", cycle + "/" + bestFitness);

      if (bestCost > Food.getCost(this.bestFood.path)) {
        bestCost = Food.getCost(this.bestFood.path);
        convergence = 0;
      }
      else convergence++;
      Log.d("BEE", "Convergence: " + convergence);
      cycle++;
      this.cycle = cycle;
      Log.e("BEE", cycle +"/"+bestCost);
      BeeColony.convergenceMetric.put(cycle, bestCost);
    }
    return null;
  }

  private Food roulette(List<Food> foods) {
    int total = 0;
    for(Food f: foods) total += f.getNectarSize();
    int random = new Random(seed).nextInt(total);
    int index = 0;
    int sum = 0;
    for(Food f: foods) {
      if (sum <= random) return foods.remove(index);
      sum += f.getNectarSize();
      index++;
    }
    return null;
  }

  private Food dance() {
    double cost = Double.MAX_VALUE;
    Food bestFood = null;
    for (Food food: this.foods) {
      if(Food.getCost(food.path) < cost) {
        cost = Food.getCost(food.path);
        bestFood = food;
      }
    }
    return bestFood;
  }

  public List<GraphPoint> getSolution() {
    // Revert back BeeColony Point to GraphPoint
    List<GraphPoint> solution = new ArrayList<>();
    for(Point p: this.bestFood.path)
      solution.add(this.g.getPoints().get(p.getId()));
    return solution;
  }
  public double getCost() {
    return Food.getCost(this.bestFood.path);
  }
  public int getCycle() {
    return this.cycle;
  }
  public Map<Integer, Double> getConvergenceMetric() { return BeeColony.convergenceMetric; }

  private void init(int numBee) {
    this.colony = new LinkedList<>();
    int numForager = (int) Math.floor(BeeColony.percentForager * numBee);
    int numOnlooker = (int) Math.floor(BeeColony.percentOnlooker * numBee);
    for (int i = 0; i < numForager; i++) {
      Bee bee = new Bee(Bee.Type.EMPLOYED);
      Food food = bee.scout(this.start, this.end);
      this.foods.add(food);
      colony.add(bee);
    }
    for (Food f: this.foods) Log.d("BEE", "INIT: Food Size: " + f.path.size());
    for (int i = 0; i < numOnlooker; i++) {
      Bee bee = new Bee(Bee.Type.ONLOOKER);
      colony.add(bee);
    }
  }

  private static class Point {
    private final GraphPoint point;
    private final Set<Point> nextPoints = new HashSet<>();
    private Point nextPointOnLine = null;

    Point(GraphPoint p) {
      this.point = p;
    }
    private void addNext(Point p) {
      this.nextPoints.add(p);
    }
    private Set<Point> getNextPoints() {
      return this.nextPoints;
    }
    private Point getNextPointOnLine() {
      return this.nextPointOnLine;
    }
    private String getId() {
      return this.point.getId();
    }
    private int getIdLine() {
      return this.point.getIdLine();
    }
    public void addNextOnLine(Point np) {
      this.nextPointOnLine = np;
    }
  }

  private static class Bee {
    enum Type {
      EMPLOYED,
      ONLOOKER,
      SCOUT;
    }
    private Type type;
    private Bee(Type type) {
      this.type = type;
    }
    public Food forage(Food food) {
      for (int i = 0; i < BeeColony.forageLimit;) {
        if(food.optimize()) i = 0;
        else i++;
      }
      return food;
    }
    public void transform(Type type) {
      this.type = type;
    }
    private Food scout(Point start, Point end) {
      return Food.build(start, end);
    }
  }

  private static class Food implements Comparable<Food> {
    private final Set<Point> taboo = new HashSet<>();
    private List<Point> path = new ArrayList<>();

    public Food(Point start, Point end) {
      this.path.add(start);
      Point current = start;
      do {
        Point np = getNext(current);
        if (np != null) {
          this.path.add(np);
          current = np;
        } else current = this.backtrack();
      } while (current != null && current != end);
      this.taboo.clear();
    }

    private Point getNext(Point point) {
      List<Point> nextPoints = new ArrayList<>(point.getNextPoints());
      nextPoints.removeIf(np -> this.path.contains(np) || this.taboo.contains(np));
      if (nextPoints.isEmpty()) return null;
      ++seed;
//      Log.d("BEE", String.valueOf(seed));
      int index = new Random(seed).nextInt(nextPoints.size());
      return nextPoints.get(index);
    }
    private static Point getNextScout(List<Point> path, List<Point> taboo, Point point) {
      List<Point> nextPoints = new ArrayList<>(point.getNextPoints());
      nextPoints.removeIf(np -> path.contains(np) || taboo.contains(np));
      if (nextPoints.isEmpty()) return null;
      ++seed;
//      Log.d("BEE", String.valueOf(seed));
      int index = new Random(seed).nextInt(nextPoints.size());
      return nextPoints.get(index);
    }
    private Point backtrack() {
      if (this.path.isEmpty()) return null;
      this.taboo.add(this.path.remove(this.path.size()-1));
      if (this.path.isEmpty()) return null;
      return this.path.get(this.path.size()-1);
    }
    private static Food build(Point start, Point end) {
      return new Food(start, end);
    }
    private static double getCost(List<Point> path) {
      double cost = 0;
      Point prev = null;
      for(Point p: path) {
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
    private static Point getNextOnLine(Point point) {
//      for(Point p: point.getNextPoints())
//        if (p.getIdLine() == point.getIdLine())
//          return p;
//      return null;
      return point.getNextPointOnLine();
    }
    private static List<Point> scout(Point start, Point end, int limit) {
      List<Point> taboo = new ArrayList<>();
      List<Point> path = new ArrayList<>();
      path.add(start);
      Point current = start;
      int length = 0;
      do {
        if (length >= limit) return null;
        Point np = getNextScout(path, taboo, current);
        if (np != null) {
          path.add(np);
          current = np;
        } else { // backtrack
          if (path.isEmpty()) return null;
          taboo.add(path.remove(path.size()-1));
          if (path.isEmpty()) return null;
          current = path.get(path.size()-1);
        }
        length++;
      } while (current != null && current != end);
      return path;
    }

    public boolean optimize() {
      // determine start point to optimize
      ++seed;
      // Log.d("BEE", String.valueOf(seed));
      int startIndex = new Random(seed).nextInt(this.path.size()/3);
      int endIndex = 0;
      Point start = this.path.get(startIndex);
      for (int i = startIndex + 1; i < this.path.size(); i++) {
        if (this.path.get(i).getIdLine() != start.getIdLine()) {
          startIndex = i-1;
          start = this.path.get(startIndex);
          break;
        }
      }
      if (start == null) return false;

      // determine optimization end point
      // Point end = null;
      // for(int i = this.path.size()-1; i >= startIndex; i--) {
      //   if (this.path.get(i).getIdLine() == start.getIdLine()) {
      //     end = this.path.get(i);
      //     endIndex = i;
      //     break;
      //   }
      // }

      // start.marked = true;
      // end.marked = true;
      //
      // // can optimize?
      // if (endIndex == 0 || end == null || startIndex == endIndex) return false;

      // copy pre-chain
      List<Point> newPath = new ArrayList<>();
      for(int i = 0; i < startIndex; i++)
        newPath.add(this.path.get(i));


      if (true) {
        // build optimized chain
        Point next = Food.getNextOnLine(start);
        // Log.d("BEE", "Opt: Line ID: " + start.getIdLine());
        while (next != null) {
          // Log.d("BEE", "Following: Line ID: " + next.getIdLine());
          if (this.path.contains(next)) break;
          if (newPath.contains(next)) {
            Log.d("BEE", "Opt loop detected.");
            return false; // contains loop
          }
          newPath.add(next);
          next = Food.getNextOnLine(next);
        }

        for(int i = 0; i < this.path.size(); i++) {
          if (this.path.get(i) == next) {
            endIndex = i;
          }
        }
        if (endIndex == 0) {
          newPath.clear();
          return false;
        }

        // copy post-chain
        for(int i = endIndex; i < this.path.size(); i++)
          newPath.add(this.path.get(i));
      } else {
        int limit = this.path.size() - startIndex;
        List<Point> subPath = Food.scout(start, this.path.get(this.path.size()-1), limit);
        if (subPath != null) {
          Log.d("BEE", "New Optimized Path Found!" + subPath.size() + "/" + limit);
          for(int i = 1; i < subPath.size(); i++)
            newPath.add(subPath.get(i));
        } else {
          Log.e("BEE", "No optimized path!");
          return false;
        }
      }


      // better cost?
      if (Food.getCost(newPath) < Food.getCost(this.path)) {
        this.path = newPath;
        return true;
      }

      return false;
    }
    public int getNectarSize() {
      if (this.path.isEmpty()) return 1;
      List<Integer> lines = new ArrayList<>();
      int lastLineId = 0;
      for(int i = 0; i < this.path.size(); i++) {
        int lineId = this.path.get(i).getIdLine();
        if (lineId != lastLineId) {
          lastLineId = lineId;
          lines.add(lineId);
        }
      }
      return lines.size();
    }
    @Override
    public int compareTo(Food food) {
      return Double.compare(Food.getCost(this.path), Food.getCost(food.path));
    }
  }

}
