package ap.mobile.beenavigation.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Interchange {

  protected int idInterchange;
  protected String name;
  protected HashMap<Integer, Point> points;

  public Interchange(int idInterchange, String name) {
    this.idInterchange = idInterchange;
    this.name = name;
    this.points = new HashMap<>();
  }

  public int getId() { return this.idInterchange; }
  public String getName() { return this.name; }

  public List<Point> getPoints() {
    return new ArrayList<>(this.points.values());
  }

  public boolean hasPointId(int id) {
    return this.points.containsKey(id);
  }

  public void addPoint(Point point) {
    this.points.put(point.id, point);
  }

}
