package com.yt.usertsp0416;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {


    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;

    private Button btnPlay;
    private Button btnPause;

    private Button btnGetImg;

    private ImageView img;
    private Bitmap mBitmap;

    String rtsp ="rtsp://dbtqzh:admin123@192.168.36.65:554/stream/realtime?channel=1&streamtype=1";//"rtsp://192.168.36.192:8554";// "rtsp://dbtqzh:admin123@192.168.36.65:554/stream/realtime?channel=1&streamtype=1";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPlay=findViewById(R.id.btnPlay);
        btnPause=findViewById(R.id.btnPause);
        btnGetImg=findViewById(R.id.btnGetImg);

        img=findViewById(R.id.img);

        mSurfaceView = findViewById(R.id.surface);




        mMediaPlayer = new MediaPlayer();

        mMediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(rtsp));


        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                runOnUiThread(() -> {
                    if (holder != null && mMediaPlayer != null) {
                        mMediaPlayer.setDisplay(holder);
                    }

                    if (mMediaPlayer != null)
                        mMediaPlayer.start();

                });

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

//        try {
//            DealData();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//
//        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//        retriever.setDataSource(rtsp);
//        // 获取第一帧图片
//        Bitmap frame = retriever.getFrameAtTime(0);


        btnGetImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(frame!=null)
//                    img.setImageBitmap(frame);

                if(byteBuffer==null)
                {
                    Toast.makeText(MainActivity.this,"byteBuffer is null",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(byteBuffer.array().length == 0)
                {
                    Toast.makeText(MainActivity.this,"byteBuffer.array is null",Toast.LENGTH_SHORT).show();
                    return;
                }

                mBitmap= BitmapFactory.decodeByteArray(byteBuffer.array(),0,byteBuffer.array().length);
                if(mBitmap!=null){
                    img.setImageBitmap(mBitmap);
                }

            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMediaPlayer!=null && !mMediaPlayer.isPlaying()){
                    mMediaPlayer.start();
                }
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMediaPlayer!=null && mMediaPlayer.isPlaying()){
                    mMediaPlayer.pause();
                }
            }
        });

    }


    ByteBuffer byteBuffer=null;
    private void DealData() throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(rtsp);

        MediaFormat format = null; // 声明在循环外部
        int trackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                trackIndex = i;
                extractor.selectTrack(trackIndex);
                break;
            }
        }


        MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        codec.configure(format, mSurfaceView.getHolder().getSurface(), null, 0);
        codec.start();

        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isEOS = false;
        while (!isEOS) {
            int inputBufferIndex = codec.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isEOS = true;
                } else {
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }

            int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];


                // 处理每一帧的数据
                codec.releaseOutputBuffer(outputBufferIndex, false);

                byteBuffer=outputBuffer;

            }
        }

        codec.stop();
        codec.release();
        extractor.release();

    }

}