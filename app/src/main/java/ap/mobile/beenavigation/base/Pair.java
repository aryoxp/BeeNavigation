package ap.mobile.beenavigation.base;

import com.google.android.gms.maps.model.LatLng;

public class Pair {
  public String type;
  public double lat;
  public double lng;
  public double lt;
  public double lg;
  public Pair(String type, double lat, double lng, double lt, double lg) {
    this.type = type;
    this.lat = lat;
    this.lng = lng;
    this.lt = lt;
    this.lg = lg;
  }
  public Pair(String type, LatLng start, LatLng end) {
    this(type, start.latitude, start.longitude, end.latitude, end.longitude);
  }
  public LatLng getStartPosition() {
    return new LatLng(this.lat, this.lng);
  }
  public LatLng getEndPosition() {
    return new LatLng(this.lt, this.lg);
  }
}
