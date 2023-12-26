package ap.mobile.beenavigation.lib;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ap.mobile.beenavigation.base.Graph;
import ap.mobile.beenavigation.base.Graph.GraphPoint;
import ap.mobile.beenavigation.util.CDM;
import ap.mobile.beenavigation.util.Helper;

public class BeeColony {

  private final GraphPoint start;
  private final GraphPoint end;
  private Set<Bee> colony;

  // Parameter

  private static final int foragerLimit = 5;
  private static final int cycleLimit = 300;
  private static final float percentOnlooker = .5f;
  private static final float percentForager = .5f;
  private static final float percentScout = .2f;

  public BeeColony(Graph g, GraphPoint start, GraphPoint end) {
    this.start = g.getPoint(start.getId());
    this.end = g.getPoint(end.getId());
  }

  public List<LatLng> run(int numBee) {
    int cycle = 0;
    double bestFitness = 0;
    Food bestFood = null;

    this.init(numBee);
    while(cycle < BeeColony.cycleLimit) {
      // Employed bees forage and waggle dance
      List<Bee> hives = this.dance();
      bestFitness = hives.get(0).food.fitness;
      bestFood = hives.get(0).food;
      // Bad bees go scout
      int numScout = (int) Math.floor(BeeColony.percentScout * hives.size());
      for(int i = hives.size()-1; i > hives.size() - 1 - numScout; i--)
        hives.get(i).type = Bee.Type.SCOUT;
      // Onlooker bees look other bees with good dance
      this.fastLook(hives);
      // Onlooker bees check their dance
      for(Bee bee: this.colony) {
        if (bee.type == Bee.Type.ONLOOKER) {
          if (bee.food.fitness >= bestFitness) {
            bestFood = bee.food;
            bestFitness = bee.food.fitness;
          }
        }
      }
      // Log.d("CYCLE", cycle + "/" + bestFitness);
      cycle++;
    }

    // Yields the best food (path) in LatLng list
    List<LatLng> result = new ArrayList<>();
    if (bestFood == null) return result;
    for(GraphPoint p: bestFood.path)
      result.add(new LatLng(p.getLat(), p.getLng()));
    return result;
  }

  private void init(int numBee) {
    this.colony = new HashSet<>();
    int numForager = (int) Math.floor(BeeColony.percentForager * numBee);
    int numOnlooker = (int) Math.floor(BeeColony.percentOnlooker * numBee);
    for (int i = 0; i < numForager; i++) {
      Bee bee = new Bee(Bee.Type.EMPLOYED);
      bee.scout(this.start, this.end);
      colony.add(bee);
    }
    for (int i = 0; i < numOnlooker; i++) {
      Bee bee = new Bee(Bee.Type.ONLOOKER);
      colony.add(bee);
    }
  }

  private  List<Bee> dance() {
    ArrayList<Bee> foragers = new ArrayList<>();
    for(Bee bee: this.colony) {
      if (bee.type == Bee.Type.SCOUT) bee.scout(this.start, this.end);
      if (bee.type == Bee.Type.EMPLOYED) {
        bee.forage();
        foragers.add(bee);
      }
    }
    // Line up all foragers so that Best bee (have highest fitness) is always on top
    foragers.sort(Collections.reverseOrder());
    return foragers;
  }
  private  void look() {
    // Onlooker looks for bee with better fitness
    // Roulette wheel picks
    double tFitness = 0;
    for(Bee bee: this.colony) {
      if (bee.type == Bee.Type.EMPLOYED) {
        tFitness += bee.food.fitness;
      }
    }
    List<Bee> employedBees = new ArrayList<>();
    for(Bee bee: this.colony) {
      if (bee.type == Bee.Type.EMPLOYED) {
        bee.probability = bee.food.fitness / tFitness;
        employedBees.add(bee);
      }
    }
    for(Bee bee: this.colony) {
      if (bee.type == Bee.Type.ONLOOKER) {
        double r = Math.random();
        float sum = 0;
        int index = 0;
        for (Bee b: employedBees) {
          sum += b.probability;
          if (sum >= r) break;
          index++;
        }
        Bee selectedBee = employedBees.get(index);
        bee.food = new Food();
        bee.food.path = new ArrayList<>(selectedBee.food.path);
        bee.food.calculateFitness();
        bee.food.mutate();
      }
    }
  }
  private  void fastLook(List<Bee> hives) {
    for(Bee bee: this.colony) {
      if (bee.type == Bee.Type.ONLOOKER) {
        Bee bestBee = hives.get(0);
        if (bestBee != null) {
          bee.food = new Food();
          bee.food.path = new ArrayList<>(bestBee.food.path);
          bee.food.calculateFitness();
          bee.food.mutate();
        }
      }
    }
  }

  private static class Bee implements Comparable<Bee> {
    enum Type {
      EMPLOYED,
      ONLOOKER,
      SCOUT
    }
    private Type type;
    private Food food = new Food();
    private double probability;

    private Bee(Type type) {
      this.type = type;
    }
    private void scout(GraphPoint start, GraphPoint end) {
      this.food = new Food().build(start, end);
      this.type = Type.EMPLOYED;
    }
    private void forage() {
      int cycle = 1;
      while (cycle < BeeColony.foragerLimit) {
        // will return true if mutation yields better fitness
        // gets better food, resets foraging cycle
        if(this.food.mutate()) cycle = 1;
        cycle++;
      }
    }
    @Override
    public int compareTo(Bee bee) {
      return Double.compare(this.food.fitness, bee.food.fitness);
    }
  }

  private static class Food {
    private List<GraphPoint> path = new ArrayList<>();
    private List<GraphPoint> taboo = new ArrayList<>();
    private double fitness = 0;
    private GraphPoint end;

    private boolean avoid(GraphPoint np) {
      return this.path.contains(np) || this.taboo.contains(np);
    }
    public Food build(GraphPoint start, GraphPoint end) {
      this.end = end;
      this.path.add(start);
      this.taboo = new ArrayList<>();
      GraphPoint current = start;
      while(current != null && current != end) {
        GraphPoint nextPoint = this.getNext(current);
        if (nextPoint == null) {
          this.taboo.add(this.removeLast());
          current = this.getLast();
        } else {
          this.path.add(nextPoint);
          current = nextPoint;
        }
      }
      this.calculateFitness();
      return this;
    }
    private GraphPoint getNext(GraphPoint point) {
      List<GraphPoint> nextPoints = new ArrayList<>(point.getNextPoints());
      nextPoints.removeIf(this::avoid);
      if (nextPoints.size() == 0) return null;
      int index = new Random().nextInt(nextPoints.size());
      return nextPoints.get(index);
    }
    private GraphPoint getLast() {
      if (this.path.size() == 0) return null;
      return this.path.get(this.path.size()-1);
    }
    private GraphPoint removeLast() {
      if (this.path.size() == 0) return null;
      return this.path.remove(this.path.size()-1);
    }
    private void calculateFitness() {
      this.fitness = this.calculateFitness(this.path);
    }
    private double calculateFitness(List<GraphPoint> path) {
      GraphPoint prev = null;
      int cost = 0;
      for(GraphPoint p: path) {
        if (prev == null) {
          prev = p;
          cost = CDM.cost;
          continue;
        }
        if (p.getIdLine() != prev.getIdLine()) cost += CDM.cost;
        cost += Helper.calculateDistance(p.getLatLng(), prev.getLatLng());
        prev = p;
      }
      return (double) 1 / (1 + cost);
    }
    public boolean mutate() {
      List<GraphPoint> candidates = new ArrayList<>();
      List<GraphPoint> mutatedPath = new ArrayList<>();
      List<GraphPoint> mTaboo = new ArrayList<>();

      for(GraphPoint p: this.path) {
        List<GraphPoint> nextPoints = new ArrayList<>(p.getNextPoints());
        if (nextPoints.size() > 0) candidates.add(p);
      }
      if(candidates.size() == 0) return false; //cannot mutate

      // Picks random mutation point as a starting point
      GraphPoint mutatePoint = candidates.get(new Random().nextInt(candidates.size()));
      // Copy previous path up to mutation point
      for(GraphPoint p: this.path) {
        mutatedPath.add(p);
        if (p == mutatePoint) break;
      }

      GraphPoint current = mutatePoint; // new GraphPoint(mutatePoint);
      while(current != null && current != end) {
        GraphPoint nextPoint = null;
        List<GraphPoint> nextPoints = new ArrayList<>(current.getNextPoints());
        nextPoints.removeIf(np -> mutatedPath.contains(np) || mTaboo.contains(np));
        if (nextPoints.size() > 0) {
          int index = new Random().nextInt(nextPoints.size());
          nextPoint = nextPoints.get(index);
        }
        if (nextPoint == null) { // dead-end
          // go one step back, do backtrack
          GraphPoint tp = mutatedPath.remove(mutatedPath.size()-1);
          if (!mutatedPath.contains(mutatePoint)) break;
          mTaboo.add(tp);
          current = mutatedPath.get(mutatedPath.size()-1);
          if (current == mutatePoint) break; // cannot mutate
        } else {
          mutatedPath.add(nextPoint);
          current = nextPoint;
        }
      }
      double mutatedFitness = 0;
      if (current == end)
        mutatedFitness = calculateFitness(mutatedPath);
      if (mutatedFitness > this.fitness) {
        // if the new mutated path has better fitness, update new food path as path
        this.path = mutatedPath;
        this.fitness = mutatedFitness;
        return true;
      } else return false;
    }
  }

}
