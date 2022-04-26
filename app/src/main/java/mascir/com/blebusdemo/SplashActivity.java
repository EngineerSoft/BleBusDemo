package mascir.com.blebusdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends Activity {

    private ProgressBar progressBar;
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        hideSystemUI();

        ImageView img1 = (ImageView) findViewById(R.id.aptiv_logo);
        ImageView img2 = (ImageView) findViewById(R.id.beacon_logo);
        TextView txt = (TextView) findViewById(R.id.app_name);
        progressBar = findViewById(R.id.progress);

        //make logo, app name and progress bar animation
        Animation myanim = AnimationUtils.loadAnimation(this, R.anim.mytransition);
        txt.startAnimation(myanim);
        img1.startAnimation(myanim);
        img2.startAnimation(myanim);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.startAnimation(myanim);

        //function to load mainActivity
        loadMain();
    }

    private void loadMain() {

        final Intent i = new Intent(this, DeviceScanActivity.class);
        Thread timer = new Thread() {
            public void run() {
                try {
                    sleep(3000);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    startActivity(i);
                    finish();
                }
            }
        };
        timer.start();
    }

    private void hideSystemUI() {

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}