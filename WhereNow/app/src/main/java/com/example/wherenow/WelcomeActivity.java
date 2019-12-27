package com.example.wherenow;

import android.os.*;
import android.content.*;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        new Handler().postDelayed(new Runnable() {
                                      @Override
                                      public void run() {
                                          Intent intent = new Intent(WelcomeActivity.this, MapsActivity.class);
                                          startActivity(intent);
                                          finish();
                                      }
                                  },
                5000);     }
}
