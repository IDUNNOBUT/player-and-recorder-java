package com.example.myapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.io.IOException;

public class PlayerFragment extends Fragment {
    String[] URLS = {
            "https://onlinetestcase.com/wp-content/uploads/2023/06/1-MB-OGG.ogg",
            "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/race1.ogg",
            "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/menu.ogg"
    };
    int[] images = {R.drawable.img_1, R.drawable.img_2, R.drawable.img_3};
    int currentAudioIndex = 0;
    boolean isPlaying = false;
    MediaPlayer mediaPlayer;
    ImageView albumCover;
    ImageButton prevButton, nextButton, playButton, loopButton;
    SeekBar seekBar;
    TextView time, duration;
    Thread updateSeekBarThread;

    public PlayerFragment() {
        super(R.layout.fragment_player);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        prevButton = view.findViewById(R.id.prevButton);
        nextButton = view.findViewById(R.id.nextButton);
        playButton = view.findViewById(R.id.playButton);
        loopButton = view.findViewById(R.id.loopButton);
        albumCover = view.findViewById(R.id.albumCover);
        seekBar = view.findViewById(R.id.seekBar);
        time = view.findViewById(R.id.time);
        duration = view.findViewById(R.id.duration);
        seekBar.setMax(100);

        prevButton.setOnClickListener(v -> prev());
        nextButton.setOnClickListener(v -> next());
        loopButton.setOnClickListener(v -> loop());
        playButton.setOnClickListener(v -> {
            if (isPlaying) {
                stop();
            } else {
                start();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    time.setText(convertMillisToTimeFormat(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (updateSeekBarThread != null) {
            updateSeekBarThread.interrupt();
            updateSeekBarThread = null;
        }
    }

    private void prev() {
        if (currentAudioIndex > 0) {
            currentAudioIndex--;
            albumCover.setImageResource(images[currentAudioIndex]);
            mediaPlayer.setLooping(false);
            loopButton.setImageTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            playCurrentAudio();
        }
    }

    private void next() {
        if (currentAudioIndex < URLS.length - 1) {
            currentAudioIndex++;
            albumCover.setImageResource(images[currentAudioIndex]);
            mediaPlayer.setLooping(false);
            loopButton.setImageTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            playCurrentAudio();
        }
    }

    private void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            updateSeekBar();
        } else {
            playCurrentAudio();
        }
        playButton.setImageResource(R.drawable.pause);
        isPlaying = true;
    }

    private void stop() {
        playButton.setImageResource(R.drawable.play);
        isPlaying = false;
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    private void loop() {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(!mediaPlayer.isLooping());
            loopButton.setImageTintList(ColorStateList.valueOf(mediaPlayer.isLooping() ? Color.BLUE : Color.TRANSPARENT));
        }
    }

    private void playCurrentAudio() {
        playButton.setImageResource(R.drawable.pause);
        isPlaying = true;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener((listener) -> next());
        try {
            mediaPlayer.setDataSource(URLS[currentAudioIndex]);
            mediaPlayer.prepare();
            mediaPlayer.start();
            updateSeekBar();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer != null) {
            duration.setText(convertMillisToTimeFormat(mediaPlayer.getDuration()));
            seekBar.setMax(mediaPlayer.getDuration());

            Handler handler = new Handler();
            Runnable updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        time.setText(convertMillisToTimeFormat(currentPosition));
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.post(updateSeekBarRunnable);
        }
    }

    private String convertMillisToTimeFormat(int millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
