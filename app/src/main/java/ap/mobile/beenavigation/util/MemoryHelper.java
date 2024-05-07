package ap.mobile.beenavigation.util;


import android.app.ActivityManager;
import android.content.Context;

public class MemoryHelper {
  public static ActivityManager.MemoryInfo getAvailableMemory(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    activityManager.getMemoryInfo(memoryInfo);
    return memoryInfo;
  }
}
