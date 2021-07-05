package jlab.floatingfolder.activity;

import jlab.floatingfolder.R;
import jlab.floatingfolder.Utils;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.os.Looper;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;


/**
 * Created by Javier on 7/4/2020.
 */

public class SplashActivity extends AppCompatActivity {
    private ImageView ivIcon;
    private boolean finish = false;
    private final long timeSleep = 250;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!finish) {
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        ivIcon = (ImageView) findViewById(R.id.ivIconInSplash);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.beat);
        ivIcon.startAnimation(animation);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Utils.getAllApplications(SplashActivity.this, "");
            }
        }).start();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }).start();
            }
        }, 2000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish = true;
        finish();
    }
}