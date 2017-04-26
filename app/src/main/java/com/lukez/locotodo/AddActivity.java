package com.lukez.locotodo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Activity;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;

public class AddActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add);

    Intent intent = getIntent();

    Bitmap bitmap = intent.getParcelableExtra("BitmapImage");
    LatLng latLng = intent.getParcelableExtra("LatLng");

    ImageView imgView = (ImageView)findViewById(R.id.imgSnap);

    imgView.setImageBitmap(bitmap);
  }

}
