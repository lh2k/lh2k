package kr.ac.hansung.lh2k_project;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

/**
 * Created by Musong on 2017-05-19.
 */

public class HelpActivity extends Activity {

    private Button btn_help_page_back;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        btn_help_page_back = (Button) findViewById(R.id.btn_help_page_back);

        btn_help_page_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HelpActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(HelpActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
