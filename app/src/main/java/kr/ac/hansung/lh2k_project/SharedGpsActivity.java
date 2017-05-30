package kr.ac.hansung.lh2k_project;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Musong on 2017-05-09.
 */

public class SharedGpsActivity extends Activity {

    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;

    private Button shared_gps_btn_ok, btn_shared_user_delete, btn_goHome;
    private EditText shared_gps_id;
    private TextView tv_errorMsg;

    private String email;

    // 앱 사용자 유무 체크
    private List<String> checkSharedUsers = new ArrayList<String>();
    private List<String> checkSharedUsers2 = new ArrayList<String>();
    // 위치공유목록 가져오기
    private List<String> sharedUserList = new ArrayList<String>();

    private List<String> list = new ArrayList<String>();;
    ArrayAdapter<String> adapter;
    ListView lv;
    InputMethodManager imm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gps);

        btn_goHome = (Button) findViewById(R.id.btn_goHome);
        btn_shared_user_delete = (Button) findViewById(R.id.btn_shared_user_delete);
        shared_gps_btn_ok = (Button) findViewById(R.id.shared_gps_btn_ok);
        shared_gps_id = (EditText) findViewById(R.id.shared_gps_id);
        tv_errorMsg = (TextView) findViewById(R.id.tv_errorMsg);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, list);

        // Write a message to the database
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        myRef = database.getReference();

        int a = mAuth.getCurrentUser().getEmail().indexOf('@');
        email = mAuth.getCurrentUser().getEmail().substring(0, a);

        lv = (ListView) findViewById(R.id.shared_user_listview);

        // 유효 사용자 목록
        init();

        Intent intent = getIntent();
        int j = intent.getIntExtra("var", 0);
        for (int i=0; i<j; i++){
            sharedUserList.add(intent.getStringExtra("name"+i));
        }

        if(sharedUserList.size()==0){
            initSharedUser(email);
        }

        for (int i=0; i<sharedUserList.size(); i++){
            list.add(sharedUserList.get(i));
        }
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        lv.setAdapter(adapter);
    }

    public void mOnClick(View v) {
        init();
        switch (v.getId()) {
            case R.id.shared_gps_btn_ok:
                try {
                    int a = shared_gps_id.getText().toString().indexOf('@');
                    String s = shared_gps_id.getText().toString().substring(0, a);
                    String check = shared_gps_id.getText().toString().substring(a);

                    boolean cc = checkUsers(s);
                    boolean cc2 = checkUsers2(s);

                    if (check.equals("@gmail.com")) {
                        if (cc == true) {
                            if (shared_gps_id.getText().toString().equals(mAuth.getCurrentUser().getEmail())) {
                                tv_errorMsg.setText("자신의 이메일 주소입니다.");
                            } else if (!shared_gps_id.getText().toString().equals(mAuth.getCurrentUser().getEmail())) {
                                if (cc2 == false) {
                                    Intent intent = new Intent(SharedGpsActivity.this, MainActivity.class);
                                    intent.putExtra("id", s);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                } else if (cc2 = true) {
                                    tv_errorMsg.setText("이미 등록된 사용자입니다.");
                                }
                            }
                        } else if (cc == false) {
                            tv_errorMsg.setText("존재하지 않는 사용자입니다.");
                        }
                    } else if (!check.equals("@gmail.com")) {
                        tv_errorMsg.setText("@gmail.com(으)로 끝나는 이메일 주소를 입력해주세요.");
                    }
                } catch (Exception e) {
                    tv_errorMsg.setText("@gmail.com(으)로 끝나는 이메일 주소를 입력해주세요.");
                }
                break;
            case R.id.btn_shared_user_delete:
                try {
                    // 삭제 버튼
                    final int pos = lv.getCheckedItemPosition();
                    final String str = list.get(pos);

                    if (pos != ListView.INVALID_POSITION) {
                        // 삭제 버튼
                        AlertDialog.Builder builder = new AlertDialog.Builder(SharedGpsActivity.this);

                        builder.setTitle("위치공유삭제")
                                .setMessage(str + "님을\n삭제하시겠습니까?")
                                .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                    // 확인 버튼 클릭시 설정
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        myRef.child(email).child("SharedUser").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                                                while (it.hasNext()) {
                                                    if (str.equals(it.next().getValue().toString())) {

                                                        // 파이어베이스 에서 삭제
                                                        myRef.child(email).child("SharedUser").child(str).removeValue();
                                                        myRef.child(str).child("SharedUser").child(email).removeValue();

                                                        // 삭제 메시지 상대방에게
                                                        myRef.child(str).child("deleteUser").child(email).child(email).setValue(email);

                                                        // 리스트뷰 에서 삭제
                                                        list.remove(pos);
                                                        lv.clearChoices();
                                                        adapter.notifyDataSetChanged();

                                                        break;
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                })
                                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    // 취소 버튼 클릭시 설정
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
                        dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
                        dialog.show();
                    }
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "사용자를 선택해주세요.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_goHome:
                finish();
                break;
        }
    }

    public void init() {
        if (checkSharedUsers.size() > 0) {
            checkSharedUsers.clear();
        }

        if (checkSharedUsers2.size() > 0) {
            checkSharedUsers2.clear();
        }

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int i = 0;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while (it.hasNext()) {
                    SharedPreferences pref = getSharedPreferences("sharedCheck", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("checkUser" + i, it.next().getKey().toString());
                    editor.commit();
                    i++;
                }
                SharedPreferences pref = getSharedPreferences("sharedCheck", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt("check", i);
                editor.commit();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        myRef.child(email).child("SharedUser").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int i = 0;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while (it.hasNext()) {
                    SharedPreferences pref2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref2.edit();
                    editor.putString("checkUser2" + i, it.next().getValue().toString());
                    editor.commit();
                    i++;
                }

                SharedPreferences pref2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref2.edit();
                editor.putInt("check2", i);
                editor.commit();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        SharedPreferences pref = getSharedPreferences("sharedCheck", Activity.MODE_PRIVATE);
        for (int i = 0; i < pref.getInt("check", 0); i++) {
            SharedPreferences pref_shared = getSharedPreferences("sharedCheck", Activity.MODE_PRIVATE);
            String s = pref_shared.getString("checkUser" + i, "0");
            checkSharedUsers.add(s);
        }

        SharedPreferences pref2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
        for (int i = 0; i < pref2.getInt("check2", 0); i++) {
            SharedPreferences pref_shared2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
            String s = pref_shared2.getString("checkUser2" + i, "0");
            checkSharedUsers2.add(s);
        }
    }

    public boolean checkUsers(String s) {
        for (int i = 0; i < checkSharedUsers.size(); i++) {
            if (s.equals(checkSharedUsers.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkUsers2(String s) {
        for (int i = 0; i < checkSharedUsers2.size(); i++) {
            if (s.equals(checkSharedUsers2.get(i))) {
                return true;
            }
        }
        return false;
    }

    // 위치공유 유저목록 가져오기
    public void initSharedUser(String email) {
        if (sharedUserList.size() > 0) {
            sharedUserList.clear();
        }

        myRef.child(email).child("SharedUser").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int i = 0;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while (it.hasNext()) {
                    SharedPreferences pref2 = getSharedPreferences("sharedCheck3", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref2.edit();
                    editor.putString("checkUser3" + i, it.next().getValue().toString());
                    editor.commit();
                    i++;
                }
                SharedPreferences pref2 = getSharedPreferences("sharedCheck3", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref2.edit();
                editor.putInt("check3", i);
                editor.commit();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        SharedPreferences pref2 = getSharedPreferences("sharedCheck3", Activity.MODE_PRIVATE);
        for (int i = 0; i < pref2.getInt("check3", 0); i++) {
            SharedPreferences pref_shared2 = getSharedPreferences("sharedCheck3", Activity.MODE_PRIVATE);
            String s = pref_shared2.getString("checkUser3" + i, "0");
            sharedUserList.add(s);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Intent intent = new Intent(SharedGpsActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        initSharedUser(email);
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }
}