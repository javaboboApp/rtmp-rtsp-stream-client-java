package com.river.apollo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.river.apollo.backgroundexample.BackgroundActivity;
import com.river.apollo.customexample.RtmpActivity;
import com.river.apollo.customexample.RtspActivity;
import com.river.apollo.defaultexample.ExampleRtmpActivity;
import com.river.apollo.defaultexample.ExampleRtspActivity;
import com.river.apollo.displayexample.DisplayActivity;
import com.river.apollo.filestreamexample.RtmpFromFileActivity;
import com.river.apollo.filestreamexample.RtspFromFileActivity;
import com.river.apollo.openglexample.OpenGlRtmpActivity;
import com.river.apollo.openglexample.OpenGlRtspActivity;
import com.river.apollo.rotation.RotationExampleActivity;
import com.river.apollo.surfacemodeexample.SurfaceModeRtmpActivity;
import com.river.apollo.surfacemodeexample.SurfaceModeRtspActivity;
import com.river.apollo.texturemodeexample.TextureModeRtmpActivity;
import com.river.apollo.texturemodeexample.TextureModeRtspActivity;
import com.river.apollo.utils.ActivityLink;
import com.river.apollo.utils.ImageAdapter;
import com.river.apollo.webserver.WebServerViewActivity;

import java.util.ArrayList;
import java.util.List;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

  private GridView list;
  private List<ActivityLink> activities;

  private final String[] PERMISSIONS = {
      Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

  @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
  private final String[] PERMISSIONS_A_13 = {
          Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
          Manifest.permission.POST_NOTIFICATIONS
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    overridePendingTransition(R.transition.slide_in, R.transition.slide_out);
    TextView tvVersion = findViewById(R.id.tv_version);
    tvVersion.setText(getString(R.string.version, BuildConfig.VERSION_NAME));

    list = findViewById(R.id.list);
    createList();
    setListAdapter(activities);
    requestPermissions();
  }



  private void requestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (!hasPermissions(this)) {
        ActivityCompat.requestPermissions(this, PERMISSIONS_A_13, 1);
      }
    } else {
      if (!hasPermissions(this)) {
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
      }
    }
  }

  @SuppressLint("NewApi")
  private void createList() {
    activities = new ArrayList<>();
    activities.add(new ActivityLink(new Intent(this, WebServerViewActivity.class),
            getString(R.string.start_webserver), JELLY_BEAN));
    activities.add(new ActivityLink(new Intent(this, RtmpActivity.class),
        getString(R.string.rtmp_streamer), JELLY_BEAN));
    activities.add(new ActivityLink(new Intent(this, RtspActivity.class),
        getString(R.string.rtsp_streamer), JELLY_BEAN));
    activities.add(new ActivityLink(new Intent(this, ExampleRtmpActivity.class),
        getString(R.string.default_rtmp), JELLY_BEAN));
    activities.add(new ActivityLink(new Intent(this, ExampleRtspActivity.class),
        getString(R.string.default_rtsp), JELLY_BEAN));
    activities.add(new ActivityLink(new Intent(this, RtmpFromFileActivity.class),
        getString(R.string.from_file_rtmp), JELLY_BEAN_MR2));
    activities.add(new ActivityLink(new Intent(this, RtspFromFileActivity.class),
        getString(R.string.from_file_rtsp), JELLY_BEAN_MR2));
    activities.add(new ActivityLink(new Intent(this, SurfaceModeRtmpActivity.class),
        getString(R.string.surface_mode_rtmp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, SurfaceModeRtspActivity.class),
        getString(R.string.surface_mode_rtsp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, TextureModeRtmpActivity.class),
        getString(R.string.texture_mode_rtmp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, TextureModeRtspActivity.class),
        getString(R.string.texture_mode_rtsp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, OpenGlRtmpActivity.class),
        getString(R.string.opengl_rtmp), JELLY_BEAN_MR2));
    activities.add(new ActivityLink(new Intent(this, OpenGlRtspActivity.class),
        getString(R.string.opengl_rtsp), JELLY_BEAN_MR2));
    activities.add(new ActivityLink(new Intent(this, DisplayActivity.class),
        getString(R.string.display_rtmp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, BackgroundActivity.class),
        getString(R.string.service_rtp), LOLLIPOP));
    activities.add(new ActivityLink(new Intent(this, RotationExampleActivity.class),
        getString(R.string.rotation_rtmp), LOLLIPOP));
  }

  private void setListAdapter(List<ActivityLink> activities) {
    list.setAdapter(new ImageAdapter(activities));
    list.setOnItemClickListener(this);
  }

  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    if (hasPermissions(this)) {
      ActivityLink link = activities.get(i);
      int minSdk = link.getMinSdk();
      if (Build.VERSION.SDK_INT >= minSdk) {
        startActivity(link.getIntent());
        overridePendingTransition(R.transition.slide_in, R.transition.slide_out);
      } else {
        showMinSdkError(minSdk);
      }
    } else {
      showPermissionsErrorAndRequest();
    }
  }

  private void showMinSdkError(int minSdk) {
    String named;
    switch (minSdk) {
      case JELLY_BEAN_MR2:
        named = "JELLY_BEAN_MR2";
        break;
      case LOLLIPOP:
        named = "LOLLIPOP";
        break;
      default:
        named = "JELLY_BEAN";
        break;
    }
    Toast.makeText(this, "You need min Android " + named + " (API " + minSdk + " )",
        Toast.LENGTH_SHORT).show();
  }

  private void showPermissionsErrorAndRequest() {
    Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
    requestPermissions();
  }

  private boolean hasPermissions(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return hasPermissions(context, PERMISSIONS_A_13);
    } else {
      return hasPermissions(context, PERMISSIONS);
    }
  }

  private boolean hasPermissions(Context context, String... permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
      for (String permission : permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }
}