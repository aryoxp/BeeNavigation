package ap.mobile.beenavigation.base;

import com.google.android.gms.maps.model.LatLng;

public class Point {

  protected String id;
  protected int idLine;
  protected int sequence;
  protected boolean stop;
  protected double lat, lng;
  protected String idInterchange;

  public Point() {}
  public Point(String id, int idLine, int sequence, boolean stop, double lat, double lng, String idInterchange) {
    this.id = id;
    this.idLine = idLine;
    this.sequence = sequence;
    this.stop = stop;
    this.lat = lat;
    this.lng = lng;
    this.idInterchange = idInterchange;
  }

  public String getId() { return this.id; }

  public int getIdLine() {
    return this.idLine;
  }

  public int getSequence() {
    return this.sequence;
  }

  public boolean isStop() {
    return this.stop;
  }

  public boolean isInterchange() { return this.idInterchange != null; }

  public String getIdInterchange() {
    return this.idInterchange;
  }

  public double getLat() {
    return this.lat;
  }

  public double getLng() {
    return this.lng;
  }

  public LatLng getLatLng() {
    return new LatLng(this.lat, this.lng);
  }

  public Point copy() {
    return new Point(
        this.id, this.idLine, this.sequence, this.stop, this.lat, this.lng, this.idInterchange
    );
  }

}
