package kr.ac.hansung.lh2k_project;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class CallingActivity extends AppCompatActivity {

    private NotificationManager notificationManager;
    Notification.Builder nBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.calling);

        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nBuilder = new Notification.Builder(this);
    }

    public void onCancelClicked(View v) {
        notificationManager.cancel(1234);
        finish();
    }
}
