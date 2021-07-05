package jlab.floatingfolder.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
//TODO: Add ads in Version 2.0
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import java.util.ArrayList;
import jlab.floatingfolder.R;
import jlab.floatingfolder.Utils;
import jlab.floatingfolder.view.AppListFragment;
import jlab.floatingfolder.view.FloatingFolderView;
import jlab.floatingfolder.view.FolderListFragment;
import jlab.floatingfolder.view.OnRunOnUiThread;
import static jlab.floatingfolder.Utils.rateApp;
import static jlab.floatingfolder.Utils.showAboutDialog;

public class MainActivity extends AppCompatActivity implements OnRunOnUiThread {

    public static final int CAN_DRAW_OVERLAY = 9101, ALL_PERMISSION_REQUEST_CODE = 9100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.activity = this;
        AppListFragment.setOnRunOnUiThread(this);
        FloatingFolderView.setOnRunOnUiThread(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.llMain, new FolderListFragment()).commit();
        requestPermission();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //TODO: Add ads in Version 2.0
//        MobileAds.initialize(this, new OnInitializationCompleteListener() {
//            @Override
//            public void onInitializationComplete(InitializationStatus initializationStatus) {
//                AdView adView = findViewById(R.id.adView);
//                adView.loadAd(new AdRequest.Builder().build());
//            }
//        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public boolean requestPermission() {
        boolean request = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> requestPermissions = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
                request = true;
            }
            if (request)
                requestAllPermission(requestPermissions);
        }
        return request;
    }

    private void requestAllPermission(ArrayList<String> requestPermissions) {
        String[] permission = new String[requestPermissions.size()];
        ActivityCompat.requestPermissions(this, requestPermissions.toArray(permission),
                ALL_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mnRateApp)
            //rate
            rateApp(this);
        else if (id == R.id.mnAbout)
            //about
            showAboutDialog(this, findViewById(R.id.llMain));
        else if (id == R.id.mnClose)
            //close
            finish();
        return super.onOptionsItemSelected(item);
    }
}