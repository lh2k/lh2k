package kr.ac.hansung.lh2k_project;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class FakeCallActivity extends AppCompatActivity {

    Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    private NotificationManager notificationManager;
    Notification.Builder nBuilder;
    Ringtone ringtone;
    RingtoneManager ringtoneManager;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.fake_call);
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nBuilder = new Notification.Builder(this);
        ringtoneManager = new RingtoneManager(context);
        ringtone= ringtoneManager.getRingtone(this, alert);
        Ring();
        Notify();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAnyPlayingRingtone();
    }

    private void stopAnyPlayingRingtone() {
        if(ringtone != null || ringtone.isPlaying()) ringtone.stop();
        if(ringtoneManager != null) ringtoneManager.stopPreviousRingtone();
    }

    public void Ring() {
        try {
            ringtone.play();

            Toast.makeText(getApplicationContext(), "Fake Call이 울립니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void Notify() {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), FakeCallActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.setSmallIcon(R.drawable.fakecall_icon);
        nBuilder.setTicker("Ring~~~~");
        nBuilder.setWhen(System.currentTimeMillis());
        nBuilder.setContentTitle("Fake Call");
        nBuilder.setContentText("가짜 전화가 옵니다.");
        nBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        nBuilder.setContentIntent(pi);
        nBuilder.setAutoCancel(true);

        notificationManager.notify(1234, nBuilder.build());
    }

    public void onCallClicked(View v) {
        onStop();

        Intent intent = new Intent(this, CallingActivity.class);
        startActivity(intent);
        finish();
    }

    public void onRefuseClicked(View v) {
        onStop();
        notificationManager.cancel(1234);
        finish();
    }
}
