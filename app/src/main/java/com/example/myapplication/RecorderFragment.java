package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecorderFragment extends Fragment {
    private static final String TAG = "RecorderFragment";

    private AudioRecord recorder;
    private Button recorderButton;
    private Button playerButton;
    private TextView timer;
    private volatile boolean isRecording = false;
    private volatile boolean isPlaying = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {android.Manifest.permission.RECORD_AUDIO};

    private AudioTrack audioPlayer;
    private File audioFile;
    private static final int SAMPLE_RATE = 44100;
    private Thread recordingThread, playbackThread;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime;

    public RecorderFragment() {
        super(R.layout.fragment_recorder);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recorderButton = view.findViewById(R.id.recordAudioButton);
        playerButton = view.findViewById(R.id.playAudioButton);
        timer = view.findViewById(R.id.timer);

        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        recorderButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                record();
            }
            recorderButton.setText(isRecording ? "Stop recording" : "Start recording");
        });

        playerButton.setOnClickListener(v -> {
            if (isPlaying) {
                stopPlayback();
            } else {
                playRecord();
            }
            playerButton.setText(isPlaying ? "Stop playback" : "Start playback");
        });

        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedMillis / 1000);
                timer.setText(String.format("%d sec", seconds));
                if (isRecording) {
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRecording();
        stopPlayback();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecording();
        stopPlayback();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                getActivity().finish();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void record() {
        if (!permissionToRecordAccepted) {
            Toast.makeText(getContext(), "Permission to record audio not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            audioFile = File.createTempFile("audio", ".pcm", getContext().getCacheDir());
            Log.d(TAG, "Created temporary audio file: " + audioFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size");
            return;
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            return;
        }

        recorder.startRecording();
        isRecording = true;

        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);

        recordingThread = new Thread(() -> {
            try (FileOutputStream os = new FileOutputStream(audioFile)) {
                byte[] buffer = new byte[bufferSize];
                while (isRecording) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        os.write(buffer, 0, read);
                    } else {
                        Log.w(TAG, "Read error: " + read);
                    }
                }
                Log.d(TAG, "Recording completed, file size: " + audioFile.length());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        recordingThread.start();

        Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if (recorder != null && isRecording) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;

            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            timerHandler.removeCallbacks(timerRunnable);

            recorderButton.setText("Start recording");

            Toast.makeText(getContext(), "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void playRecord() {
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioPlayer = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();

            audioPlayer.setVolume(AudioTrack.getMaxVolume());

            audioPlayer.play();

            playbackThread = new Thread(() -> {
                try (FileInputStream fis = new FileInputStream(audioFile)) {
                    byte[] buffer = new byte[bufferSize];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        audioPlayer.write(buffer, 0, read);
                    }
                    Log.d(TAG, "Playback completed, file size: " + audioFile.length());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    audioPlayer.stop();
                    audioPlayer.release();
                    audioPlayer = null;
                    getActivity().runOnUiThread(() -> {
                        isPlaying = false;
                        playerButton.setText("Start playback");
                    });
                }
            });
            playbackThread.start();

            Toast.makeText(getContext(), "Playback started", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No recording found or file is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        if (audioPlayer != null && isPlaying) {
            isPlaying = false;
            audioPlayer.stop();
            audioPlayer.release();
            audioPlayer = null;
            playerButton.setText("Start playback");
        }
    }
}
