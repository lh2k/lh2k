package kr.ac.hansung.lh2k_project;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by Musong on 2017-05-27.
 */

public class SetupActivity extends Activity{

    private EditText et_setup_gps_gap;
    private Button btn_setup_gps_ok, btn_setup_gps_home;
    private TextView tv_setup_gsp_gap;

    // 연결된 파이어베이스
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;

    String email;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // 연결된 파이어베이스 가져옴
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        myRef = database.getReference();

        // 구글계정에서 @gmail.com을 자르고 저장하기 위함
        int a = mAuth.getCurrentUser().getEmail().indexOf('@');
        email = mAuth.getCurrentUser().getEmail().substring(0, a);

        et_setup_gps_gap = (EditText) findViewById(R.id.et_setup_gps_gap);
        btn_setup_gps_ok = (Button) findViewById(R.id.btn_setup_gps_ok);
        tv_setup_gsp_gap = (TextView) findViewById(R.id.tv_setup_gsp_gap);
        btn_setup_gps_home = (Button) findViewById(R.id.btn_setup_gps_home);

        Intent intent = getIntent();
        int i = intent.getIntExtra("Gap", 1);

        tv_setup_gsp_gap.setText("원하시는 위치정보 갱신 시간을 설정해주세요.\n(현재설정: "+i+"초)");

        btn_setup_gps_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int a = Integer.valueOf(et_setup_gps_gap.getText().toString());
                    tv_setup_gsp_gap.setText("원하시는 위치정보 갱신 시간을 설정해주세요.\n(현재설정: "+a+"초)");

                    myRef.child(email).child("UpdateGap").child(email).setValue(a);
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "숫자만 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_setup_gps_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(SetupActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
