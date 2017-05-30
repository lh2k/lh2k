package kr.ac.hansung.lh2k_project;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.navdrawer.SimpleSideDrawer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    // 연결된 파이어베이스
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;

    // 파이어베이스 리스너들
    private ChildEventListener sharedGpsListener, sharedFailedListener, sharedDeleteListener, sharedSuccessListener;
    private ValueEventListener sharedUserListener;

    // 파이어베이스에 계정 저장 위한 변수
    private String email;

    // 흔들기 감지 관련 변수들
    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;
    private int mCount;
    private static final int SHAKE_THRESHOLD = 11000;
    private static final int DATA_X = SensorManager.DATA_X;
    private static final int DATA_Y = SensorManager.DATA_Y;
    private static final int DATA_Z = SensorManager.DATA_Z;

    private SensorManager sensorManager;
    private Sensor accelerormeterSensor;

    // Gps, 구글맵 관련 변수들
    LocationListener locationListener;
    LocationManager locationManager;
    private MarkerOptions marker = new MarkerOptions();
    private GoogleMap mMap;
    private double latitude = 0.0, longitude = 0.0;
    private Marker[] m1, m2;
    private List<Double> police_latitude = new ArrayList<Double>();
    private List<Double> police_longitude = new ArrayList<Double>();
    private List<LatLng> police_LatLng = new ArrayList<LatLng>();
    private List<Double> cctv_latitude = new ArrayList<Double>();
    private List<Double> cctv_longitude = new ArrayList<Double>();
    private List<LatLng> cctv_LatLng = new ArrayList<LatLng>();
    private List<String> sharedUserList = new ArrayList<String>();
    private List<String> sharedUserMarkerList = new ArrayList<String>();
    private List<LatLng> sharedUserLatLngList = new ArrayList<LatLng>();

    // 위치정보 업데이트 될 때마다 줌/카메라이동 되는걸 방지하기 위한 변수, true, false 이용
    private boolean checkGpsFirst;

    private int checkKeyCount;

    private AlarmManager alarmManager;

    // 미디어 플레이어 관련 변수들
    MediaPlayer mPlayer;
    private boolean tg_cctv_check = false, tg_police_check = false;

    // 슬라이드 메뉴 // .jar 추가
    private SimpleSideDrawer slide_menu;

    // UI 버튼 변수들
    private Button  btn_112;
    private Button btn_setUsers, btn_Home, btn_help, btn_setUp, btn_sysExit;
    private ImageButton btn_slide_menu;

    // 업데이트 간격
    int setUpGapValue;

    private AudioManager audio;

    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 상단바를 없앰(전체화면)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        view = (View) getLayoutInflater().
                inflate(R.layout.activity_main, null);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        setContentView(view);


        // 연결된 파이어베이스 가져옴
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        myRef = database.getReference();

        // 구글계정에서 @gmail.com을 자르고 저장하기 위함
        int a = mAuth.getCurrentUser().getEmail().indexOf('@');
        email = mAuth.getCurrentUser().getEmail().substring(0, a);

        // User의 Token값 저장
        myRef.child(email).child("Token").setValue(FirebaseInstanceId.getInstance().getToken());

        // 구글맵을 가져오고 Gps 최초 업데이트 시에만 줌,카메라이동 되도록 false
        checkGpsFirst = false;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 위도, 경도값을 FRD(Firebase Realtime Database)에서 불러오는 함수, 밑에 정의
        LatLngInit();

        // cctv, 경찰서 위치 표시 on/off 하기 위한 토글버튼
        ToggleButton tg_cctv = (ToggleButton) findViewById(R.id.tg_show_cctv);
        ToggleButton tg_police = (ToggleButton) findViewById(R.id.tg_show_police);

        // UI 버튼들
        slide_menu = new SimpleSideDrawer(this);
        //slide_menu.setLeftBehindContentView(R.layout.activity_setting);   => 왼쪽에서 화면이 나오게 함
        slide_menu.setRightBehindContentView(R.layout.activity_setting);
        btn_slide_menu = (ImageButton) findViewById(R.id.btn_slide_menu);
        btn_Home = (Button) findViewById(R.id.btn_Home);
        btn_setUsers = (Button) findViewById(R.id.btn_setUsers);
        btn_sysExit = (Button) findViewById(R.id.btn_sysExit);
        btn_112 = (Button) findViewById(R.id.btn_112);
        btn_help = (Button) findViewById(R.id.btn_help);
        btn_setUp = (Button) findViewById(R.id.btn_setUp);

        // 미디어 플레이어 작동시 소리 최대로
//        setVolumeControlStream (AudioManager.STREAM_MUSIC);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        audio.setStreamVolume(AudioManager.STREAM_MUSIC,
//                audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_PLAY_SOUND);
        audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audio.setSpeakerphoneOn(true);

        // 흔들기 감지, 흔들때마다 mCount값이 증가
        mCount = 0;

        checkKeyCount = 0;

        // 미디어 플레이어 설정
        mPlayer = MediaPlayer.create(this, R.raw.swp);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // cctv 토글버튼 이벤트
        tg_cctv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tg_cctv_check = isChecked;
                if (isChecked == true) {
                    for (int i = 0; i < cctv_LatLng.size(); i++) {
                        m1[i] = mMap.addMarker(new MarkerOptions().position(cctv_LatLng.get(i)).
                                icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cctv_image))));
                    }
                } else {
                    for (int i = 0; i < cctv_LatLng.size(); i++) {
                        m1[i].remove();
                    }
                }
            }
        });

        // 경찰서 토글버튼 이벤트
        tg_police.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tg_police_check = isChecked;

                if (isChecked == true) {
                    for (int i = 0; i < police_LatLng.size(); i++) {
                        m2[i] = mMap.addMarker(new MarkerOptions().position(police_LatLng.get(i)).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.police_image))));
                    }
                } else {
                    for (int i = 0; i < police_LatLng.size(); i++) {
                        m2[i].remove();
                    }
                }
            }
        });

        // 슬라이드 메뉴로 세팅 화면 보여줌
        btn_slide_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //slide_menu.toggleLeftDrawer();    => 왼쪽에서 화면이 나오게 함
                slide_menu.toggleRightDrawer();
            }
        });

        btn_Home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent home = new Intent(MainActivity.this, MainActivity.class);
                startActivity(home);
                finish();
            }
        });

        btn_setUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // sharedUserList(위치공유목록)의 내용을 전달
                // 몇 번 반복했는지 알기위해 i값 전달
                Intent intent = new Intent(MainActivity.this, SharedGpsActivity.class);
                int i = 0;
                for (i = 0; i < sharedUserList.size(); i++) {
                    intent.putExtra("name" + i, sharedUserList.get(i));
                }
                intent.putExtra("var", i);
                startActivityForResult(intent, 0);
            }
        });

        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_setUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SetupActivity.class);
                intent.putExtra("Gap", setUpGapValue);
                startActivity(intent);
                finish();
            }
        });

        // 종료버튼
        btn_sysExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        // 112버튼
        btn_112.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child(email).child("SharedUser").addValueEventListener(sharedUserListener);
                emergencyNoti();
                Intent intent_112 = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
                try {
                    startActivity(intent_112);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerormeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 위치공유 리스트 clear ( User 리스트가 계속 쌓이는걸 방지 )
        sharedUserList.clear();

        // 파이어베이스 리스너 등록 함수, 밑에 정의
        addMyListener();

        // 업데이트 간격 설정값 가져오는 함수
        loadGpsGap();
    }

    // Map이 준비되면
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // ↑매개변수로 GoogleMap 객체가 넘어옵니다.

        // 넘어온 GoogleMap객체를 mMap이라는 변수에 저장
        mMap = googleMap;

        // Gps 받아오기 위해 locationManager 사용
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) { // 위치정보 수집이 가능한 환경인지 검사.
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isGPSEnabled || isNetworkEnabled) {
                Log.e("GPS Enable", "true");

                final List<String> m_lstProviders = locationManager.getProviders(false);
                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.e("onLocationChanged", "onLocationChanged");
                        Log.e("location", "[" + location.getProvider() + "] (" + location.getLatitude() + "," + location.getLongitude() + ")");
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        mMap.clear();

                        // 현재 User의 위도, 경도값 받아옴
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        // Gps위치 변화가 있으면 FRD에 위,경도 값 저장
                        myRef.child("UserLatLng").child(email).setValue(email+"@"+latitude+","+longitude);

                        // 구글맵에 마커 ( 위치공유자들 )
                        addSharedMarker();

                        // cctv 토글버튼이 on이면
                        if (tg_cctv_check == true) {
                            for (int i = 0; i < cctv_LatLng.size(); i++) {
                                // cctv 위치들을 따로 관리할 배열
                                m1[i] = mMap.addMarker(new MarkerOptions().position(cctv_LatLng.get(i)).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cctv_image))));
                            }
                        }

                        // 경찰서 토글버튼이 on이면
                        if (tg_police_check == true) {
                            for (int i = 0; i < police_LatLng.size(); i++) {
                                // 경찰서 위치들을 따로 관리할 배열
                                m2[i] = mMap.addMarker(new MarkerOptions().position(police_LatLng.get(i)).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.police_image))));
                            }
                        }

                        // Gps 업데이트가 최초이면
                        if( checkGpsFirst == false) {
                            // camera 좌쵸를 현재위치로...
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(
                                    new LatLng(latitude, longitude)   // 위도, 경도
                            ));

                            // 구글지도(지구) 에서의 zoom 레벨은 1~23 까지 가능합니다.
                            // 여러가지 zoom 레벨은 직접 테스트해보세요
                            CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
                            mMap.animateCamera(zoom);   // moveCamera 는 바로 변경하지만,
                            // animateCamera() 는 근거리에선 부드럽게 변경합니다

                            // true 바꿔주어서 다시 업데이트 되었을 때 줌,카메라이동 방지
                            checkGpsFirst = true;
                        }

                        // marker 표시
                        // market 의 위치, 타이틀, 짧은설명 추가 가능.
                        marker.position(new LatLng(latitude, longitude))
                                .title("현재위치")
                                .snippet("나");
                        mMap.addMarker(marker).showInfoWindow(); // 마커추가,화면에출력

                        // 마커클릭 이벤트 처리
                        // GoogleMap 에 마커클릭 이벤트 설정 가능.
                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                // 마커 클릭시 호출되는 콜백 메서드
                                Toast.makeText(getApplicationContext(),
                                        marker.getSnippet() + " 의 위치"
                                        , Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        });
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        Log.e("onStatusChanged", "onStatusChanged");
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                        Log.e("onProviderEnabled", "onProviderEnabled");
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        Log.e("onProviderDisabled", "onProviderDisabled");
                    }
                };

                // QQQ: 시간, 거리를 0 으로 설정하면 가급적 자주 위치 정보가 갱신되지만 베터리 소모가 많을 수 있다.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (String name : m_lstProviders) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            locationManager.requestLocationUpdates(name, setUpGapValue*1000, 20, locationListener);
                        }

                    }
                });

            } else {
                Log.e("GPS Enable", "false");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 0: // 위치공유등록결과
                if (resultCode == RESULT_OK) {

                    // s = Edit Text에 입력한 email
                    String s = data.getStringExtra("id");
                    // email은 현재 자신의 email
                    myRef.child(s).child("sharedGps").child(email).child(email).setValue(email);

                } else if (resultCode == RESULT_CANCELED) {

                }
                break;
        }
    }

    //Fake Call 이벤트
    public void onFakeCallClicked(View v) {
        showMessage();
    }

    private void showMessage() {
        final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Fake Call");
        builder.setMessage("Fake Call을 실행 하시겠습니까?");
        builder.setIcon(R.drawable.main_btn_fakecall);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                emergencyNoti();
                setAlarm();
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
        alertDialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
        alertDialog.show();
    }

    private void setAlarm() {
        Date t = new Date();
        t.setTime(java.lang.System.currentTimeMillis());
        alarmManager.set(AlarmManager.RTC_WAKEUP, t.getTime(), pendingIntent());
    }

    private PendingIntent pendingIntent() {
        Intent i = new Intent(getApplicationContext(), FakeCallActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        return pi;
    }

    // FRD에 저장되어있는 위도 경도값 가져오기
    public void LatLngInit() {
        if (cctv_latitude.size()>0){
            cctv_latitude.clear();
            cctv_longitude.clear();
            cctv_LatLng.clear();
            police_latitude.clear();
            police_longitude.clear();
            police_LatLng.clear();
        }
            myRef.child("Data").child("LatLng").child("police").child("latitude").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int i = 0;
                    Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                    // 빠져나오면 null값으로 나와서 SharedPreferences 사용
                    while (it.hasNext()) {
                        SharedPreferences pref = getSharedPreferences("police", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("latitude" + i, it.next().getValue().toString());
                        editor.commit();
                        i++;
                    }

                    SharedPreferences pref = getSharedPreferences("police", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putInt("checkLatitude", i);
                    editor.commit();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            myRef.child("Data").child("LatLng").child("police").child("longitude").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    int i = 0;
                    Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                    while (it.hasNext()) {
                        SharedPreferences pref = getSharedPreferences("police", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("longitude" + i, it.next().getValue().toString());
                        editor.commit();
                        i++;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            myRef.child("Data").child("LatLng").child("cctv").child("latitude").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int i = 0;
                    Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                    while (it.hasNext()) {
                        SharedPreferences pref = getSharedPreferences("cctv", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("latitude" + i, it.next().getValue().toString());
                        editor.commit();
                        i++;
                    }
                    SharedPreferences pref = getSharedPreferences("cctv", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putInt("checkLatitude", i);
                    editor.commit();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            myRef.child("Data").child("LatLng").child("cctv").child("longitude").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int i = 0;
                    Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                    while (it.hasNext()) {
                        SharedPreferences pref = getSharedPreferences("cctv", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("longitude" + i, it.next().getValue().toString());
                        editor.commit();
                        i++;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            SharedPreferences pref_cctv = getSharedPreferences("cctv", Activity.MODE_PRIVATE);

            for (int i = 0; i < pref_cctv.getInt("checkLatitude", 0); i++) {
                SharedPreferences pref = getSharedPreferences("cctv", Activity.MODE_PRIVATE);

                // cctv 파일의 latitude+i에 해당하는 값을 가져오는데, 값이 없으면 default값 0을 집어넣음
                String latitude = pref.getString("latitude" + i, "0");
                String longitude = pref.getString("longitude" + i, "0");
                cctv_latitude.add(Double.valueOf(latitude));
                cctv_longitude.add(Double.valueOf(longitude));
                cctv_LatLng.add(new LatLng(cctv_latitude.get(i), cctv_longitude.get(i)));
            }

            SharedPreferences pref_poolice = getSharedPreferences("police", Activity.MODE_PRIVATE);

            for (int i = 0; i < pref_poolice.getInt("checkLatitude", 0); i++) {
                SharedPreferences pref = getSharedPreferences("police", Activity.MODE_PRIVATE);
                String latitude = pref.getString("latitude" + i, "0");
                String longitude = pref.getString("longitude" + i, "0");
                police_latitude.add(Double.valueOf(latitude));
                police_longitude.add(Double.valueOf(longitude));
                police_LatLng.add(new LatLng(police_latitude.get(i), police_longitude.get(i)));
            }

            // 마커객체 배열로 관리하여 remove 통해서 토글버튼 off 시 해당 객체(배열)만 지울 수 있도록
            m1 = new Marker[cctv_LatLng.size()];
            m2 = new Marker[police_LatLng.size()];
    }

    // 위치공유한 유저들 위치 마커
    public void addSharedMarker() {
        if(sharedUserMarkerList.size()>0){
            sharedUserMarkerList.clear();
            sharedUserLatLngList.clear();
        }
        myRef.child("UserLatLng").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int i = 0;
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while (it.hasNext()) {
                    SharedPreferences pref = getSharedPreferences("sharedList", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("sharedList" + i, it.next().getValue().toString());
                    editor.commit();
                    i++;
                }

                SharedPreferences pref = getSharedPreferences("checkSharedList", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt("checkSharedList", i);
                editor.commit();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        SharedPreferences pref = getSharedPreferences("checkSharedList", Activity.MODE_PRIVATE);
        int aa = pref.getInt("checkSharedList", 0);

        for(int i=0; i<aa; i++) {
            SharedPreferences preff = getSharedPreferences("sharedList", Activity.MODE_PRIVATE);
            String strr = preff.getString("sharedList" + i, "-1");
            sharedUserMarkerList.add(strr);
        }

        for(int i=0; i<sharedUserList.size(); i++){
            for(int j=0; j<sharedUserMarkerList.size(); j++){
                int q = sharedUserMarkerList.get(j).indexOf('@');
                String s = sharedUserMarkerList.get(j).substring(0,q);
                String ss = sharedUserMarkerList.get(j).substring(q+1);
                int qq = ss.indexOf(',');
                String latitude = ss.substring(0, qq);
                String longitude = ss.substring(qq+1);

                if(sharedUserList.get(i).equals(s) && sharedUserList.get(i).length() == q) {
                    sharedUserLatLngList.add(new LatLng(Double.valueOf(latitude), Double.valueOf(longitude)));
                    break;
                }
            }
        }

        for(int i=0; i<sharedUserLatLngList.size(); i++){
            mMap.addMarker(new MarkerOptions().position(sharedUserLatLngList.get(i)).title("공유자 마지막 위치").snippet(sharedUserList.get(i))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))).showInfoWindow();
        }
    }

    // 위험상황 알리기
    public void emergencyNoti(){
        // 현재시간을 msec 으로 구한다.
        long now = System.currentTimeMillis();
        // 현재시간을 date 변수에 저장한다.
        Date date = new Date(now);
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss");
        // nowDate 변수에 값을 저장한다.
        String formatDate = sdfNow.format(date);

        for (int i=0; i<sharedUserList.size(); i++){
            myRef.child(sharedUserList.get(i)).child("Emergency").child(email)
                    .child(email).setValue(formatDate+","+email);
        }
    }

    public void addMyListener(){
        sharedGpsListener=null;
        sharedFailedListener=null;
        sharedSuccessListener=null;
        sharedDeleteListener=null;
        sharedUserListener=null;

        sharedGpsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while(it.hasNext()) {

                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);

                    final String str = it.next().getValue().toString();

                    // 여기서 부터는 알림창의 속성 설정
                    builder.setTitle("위치공유요청")        // 제목 설정
                            .setMessage(str + " 님의 위치공유요청" + "\n허락하시겠습니까?")        // 메세지 설정
                            .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                            .setPositiveButton("승인", new DialogInterface.OnClickListener() {
                                // 확인 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRef.child(email).child("SharedUser").child(str).setValue(str);
                                    myRef.child(str).child("SharedUser").child(email).setValue(email);
                                    myRef.child(str).child("sharedGpsSuccess").child(email).child(email).setValue(email);
                                    myRef.child(email).child("sharedGps").child(str).removeValue();

                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .setNegativeButton("거절", new DialogInterface.OnClickListener() {
                                // 취소 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRef.child(str).child("sharedGpsFailed").child(email).child(email).setValue(email);
                                    myRef.child(email).child("sharedGps").child(str).removeValue();

                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                    android.support.v7.app.AlertDialog dialog = builder.create();    // 알림창 객체 생성
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
                    dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
                    dialog.show();    // 알림창 띄우기
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        sharedFailedListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                // 위치공유 거절한 유저이름 가져오기
                while(it.hasNext()) {
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);

                    final String str = it.next().getValue().toString();
                    // 여기서 부터는 알림창의 속성 설정
                    builder.setTitle("위치공유거절")        // 제목 설정
                            .setMessage(str + " 님께서\n요청을 거절하셨습니다.")        // 메세지 설정
                            .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                // 확인 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRef.child(email).child("sharedGpsFailed").child(str).removeValue();

                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                    android.support.v7.app.AlertDialog dialog = builder.create();    // 알림창 객체 생성
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
                    dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
                    dialog.show();    // 알림창 띄우기
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        sharedSuccessListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                // 위치공유 승인한 유저이름 가져오기
                while(it.hasNext()) {
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);

                    final String str = it.next().getValue().toString();
                    // 여기서 부터는 알림창의 속성 설정
                    builder.setTitle("위치공유승인")        // 제목 설정
                            .setMessage(str + " 님께서\n요청을 승인하셨습니다.")        // 메세지 설정
                            .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                // 확인 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRef.child(email).child("sharedGpsSuccess").child(str).removeValue();

                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                    android.support.v7.app.AlertDialog dialog = builder.create();    // 알림창 객체 생성
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
                    dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
                    dialog.show();    // 알림창 띄우기
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        sharedDeleteListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                // 위치공유 삭제한 유저이름 가져오기
                while(it.hasNext()) {
                    android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);

                    final String str = it.next().getValue().toString();
                    // 여기서 부터는 알림창의 속성 설정
                    builder.setTitle("위치공유삭제")        // 제목 설정
                            .setMessage(str + " 님께서\n위치공유를 삭제하였습니다.")        // 메세지 설정
                            .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                // 확인 버튼 클릭시 설정
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    myRef.child(email).child("deleteUser").child(str).removeValue();
                                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                    android.support.v7.app.AlertDialog dialog = builder.create();    // 알림창 객체 생성
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
                    dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
                    dialog.show();    // 알림창 띄우기
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        sharedUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                int i = 0;

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
        };

        SharedPreferences pref2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
        int bb = pref2.getInt("check2", 0);
        pref2.edit().remove("check2").commit();

        sharedUserList.clear();

        for (int i = 0; i < bb; i++) {
            SharedPreferences pref_shared2 = getSharedPreferences("sharedCheck2", Activity.MODE_PRIVATE);
            String s = pref_shared2.getString("checkUser2" + i, "0");
            sharedUserList.add(s);
            pref_shared2.edit().remove("checkUser2"+i).commit();
        }

        myRef.child(email).child("sharedGps").addChildEventListener(sharedGpsListener);
        myRef.child(email).child("sharedGpsFailed").addChildEventListener(sharedFailedListener);
        myRef.child(email).child("sharedGpsSuccess").addChildEventListener(sharedSuccessListener);
        myRef.child(email).child("deleteUser").addChildEventListener(sharedDeleteListener);
        myRef.child(email).child("SharedUser").addValueEventListener(sharedUserListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        myRef.child(email).child("sharedGps").removeEventListener(sharedGpsListener);
        myRef.child(email).child("sharedGpsFailed").removeEventListener(sharedFailedListener);
        myRef.child(email).child("sharedGpsSuccess").removeEventListener(sharedSuccessListener);
        myRef.child(email).child("deleteUser").removeEventListener(sharedDeleteListener);
        myRef.child(email).child("SharedUser").removeEventListener(sharedUserListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        myRef.child(email).child("sharedGps").removeEventListener(sharedGpsListener);
        myRef.child(email).child("sharedGpsFailed").removeEventListener(sharedFailedListener);
        myRef.child(email).child("sharedGpsSuccess").removeEventListener(sharedSuccessListener);
        myRef.child(email).child("deleteUser").removeEventListener(sharedDeleteListener);
        myRef.child(email).child("SharedUser").removeEventListener(sharedUserListener);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        checkGpsFirst = false;

        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed()
    {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);

        // 여기서 부터는 알림창의 속성 설정
        builder.setTitle("애플리케이션 종료")        // 제목 설정
                .setMessage("종료하시겠습니까?")        // 메세지 설정
                .setCancelable(false)        // 뒤로 버튼 클릭시 취소 가능 설정
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    // 확인 버튼 클릭시 설정
                    public void onClick(DialogInterface dialog, int whichButton) {
                        System.exit(0);
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    // 취소 버튼 클릭시 설정
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });

        android.support.v7.app.AlertDialog dialog = builder.create();    // 알림창 객체 생성
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CCE6E6FA")));
        dialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;
        dialog.show();    // 알림창 띄우기
    }

    // 위치정보 갱신 설정값 가져오기
    public void loadGpsGap(){
        myRef.child(email).child("UpdateGap").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();

                while (it.hasNext()){
                    SharedPreferences pref = getSharedPreferences("setUpGpsGap", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString("Gap",it.next().getValue().toString());
                    edit.commit();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        SharedPreferences pref = getSharedPreferences("setUpGpsGap", Activity.MODE_PRIVATE);
        String s = pref.getString("Gap", "1");
        setUpGapValue = Integer.valueOf(s);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (accelerormeterSensor != null)
            sensorManager.registerListener(this, accelerormeterSensor,
                    SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // 흔들기 감지
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);
            if (gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    mCount++;
                }

                if(mCount > 1){
                    // 위험알림
                    emergencyNoti();

                    mPlayer.setVolume(1.0f,1.0f);
                    mPlayer.setAudioStreamType(AudioManager.MODE_IN_COMMUNICATION);
                    mPlayer.start();

                    //Intent intent = new Intent(MainActivity.this, MainActivity.class);
                }

                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event){

        if(keycode == KeyEvent.KEYCODE_VOLUME_UP) {
            checkKeyCount++;
        }

        if(checkKeyCount == 3) {
            mPlayer.stop();
            try {
                mPlayer.prepare();
            }catch (Exception e){
            }
            mCount = 0;
            checkKeyCount = 0;
        }
        return super.onKeyDown(keycode, event);
    }
}