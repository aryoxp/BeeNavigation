package ap.mobile.beenavigation.lib;

import java.util.ArrayList;
import java.util.List;

public class DouglasPeucker {

  private static double toleranceDistance = 200; // meter
  private static final double oneDegreeInMeter = 111319.9; // 1 degree in meter

  public static <T extends LatLng> List<T> simplify(List<T> points, int toleranceDistance) {
    if (points.size() <= 1) return points;

    // if(toleranceDistance == 0) DouglasPeucker.toleranceDistance = 200;
    // else DouglasPeucker.toleranceDistance = toleranceDistance;
    DouglasPeucker.toleranceDistance = toleranceDistance;

    DouglasPeucker.douglasPeucker(points);

    List<T> simplifiedPoints = new ArrayList<>();
    simplifiedPoints.add(points.get(0));
    for (T point : points) {
      if (point.isKeep()) simplifiedPoints.add(point);
    }
    simplifiedPoints.add(points.get(points.size() - 1));
    return simplifiedPoints;
  }


  private static <T extends LatLng> void douglasPeucker(List<T> points) {

    int length = points.size();

    double maxDistance = 0;
    int maxIndex = 0;
    double distanceDeg = DouglasPeucker.toleranceDistance / DouglasPeucker.oneDegreeInMeter;

    for (int i = 1; i < length - 2; i++) {

      T origin = points.get(0);
      T destination = points.get(length - 1);

      double pDistance = DouglasPeucker.perpendicularDistance(points.get(i), origin, destination);
      if (pDistance > maxDistance) {
        maxDistance = pDistance;
        maxIndex = i;
      }

    }

    if (maxDistance > distanceDeg) {
      points.get(maxIndex).keep();
      douglasPeucker(new ArrayList<>(points.subList(0, maxIndex)));
      douglasPeucker(new ArrayList<>(points.subList(maxIndex, length - 1)));
    }

  }

  private static <T extends LatLng> double perpendicularDistance(T point, T origin, T destination) {

    // (py – qy)x + (qx – px)y + (pxqy – qxpy) = 0

    double a = origin.latitude - destination.latitude;
    double b = destination.longitude - origin.longitude;
    double c = (origin.longitude * destination.latitude)
        - (destination.longitude * origin.latitude);

    // d = |Am + Bn + C| / sqrt (A^2 + B^2);

    return Math.abs(a * point.longitude + b * point.latitude + c) /
        (Math.sqrt(a * a + b * b));

  }


  public static class LatLng extends Point {

    private boolean keep = false;

    public LatLng(String id, double latitude, double longitude) {
      super(id, latitude, longitude);
    }

    public void keep() {
      this.keep = true;
    }
    public boolean isKeep() {
      return this.keep;
    }

  }

  private static class Point {
    public double latitude, longitude;
    public String id;

    public Point(String id, double latitude, double longitude) {
      this.id = id;
      this.latitude = latitude;
      this.longitude = longitude;
    }
  }
}