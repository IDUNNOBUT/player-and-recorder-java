package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {
    FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 200);

        Button playerButton = findViewById(R.id.buttonPlayer);
        Button recorderButton = findViewById(R.id.buttonRecorder);

        playerButton.setOnClickListener(v -> {
            changeFragment(PlayerFragment.class);
        });

        recorderButton.setOnClickListener(v -> {
            changeFragment(RecorderFragment.class);
        });
    }

    protected void changeFragment(Class<? extends Fragment> fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment, null)
                .setReorderingAllowed(true)
                .commit();
    }
}