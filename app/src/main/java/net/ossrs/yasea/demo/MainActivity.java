package net.ossrs.yasea.demo;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.faucamp.simplertmp.RtmpHandler;
import com.seu.magicfilter.utils.MagicFilterType;

import net.ossrs.yasea.ScreenShotUtil;
import net.ossrs.yasea.SrsCameraView;
import net.ossrs.yasea.SrsEncodeHandler;
import net.ossrs.yasea.SrsPublisher;
import net.ossrs.yasea.SrsRecordHandler;

import java.io.IOException;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity implements RtmpHandler.RtmpListener,
        SrsRecordHandler.SrsRecordListener, SrsEncodeHandler.SrsEncodeListener {

    private static final String TAG = "Yasea";
    public final static int RC_CAMERA = 100;

    private Button btnInit;
    private Button btnPublish;
    private Button btnSwitchCamera;
    private Button btnRecord;
    private Button btnSwitchEncoder;
    private Button btnPause;
    private Button bt_snapshot, bt_activity_shot;
    private ImageView iv_snapshot, iv_activity_shot;

    private SharedPreferences sp;
    private String rtmpUrl = "rtmp://srscs.iflyhed.com:1999/examAppLive/a31062263da54a0fbdd219fa4d6a4eea/alg10259";
    private String rtmpUrl2 = "rtmp://60.205.208.197:1935/live/stream";
    private String recPath = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";

    private SrsPublisher mPublisher;
    private SrsCameraView mCameraView;

    private int mWidth = 480;
    private int mHeight = 720;
    private boolean isPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_fiftest);

        // response screen rotation event
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);


    }

    private void requestPermission() {
        //1. 检查是否已经有该权限
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)) {
            //2. 权限没有开启，请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RC_CAMERA);
        } else {
            //权限已经开启，做相应事情
            isPermissionGranted = true;
            init();
        }
    }

    //3. 接收申请成功或者失败回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被用户同意,做相应的事情
                isPermissionGranted = true;
                init();
            } else {
                //权限被用户拒绝，做相应的事情
                finish();
            }
        }
    }

    private void init() {
        // restore data.
        // initialize url.
        final EditText efu = (EditText) findViewById(R.id.et_url);
        efu.setText(rtmpUrl);
        btnInit = findViewById(R.id.bt_init);
        btnPublish = (Button) findViewById(R.id.publish);
        btnSwitchCamera = (Button) findViewById(R.id.swCam);
        btnRecord = (Button) findViewById(R.id.record);
        btnSwitchEncoder = (Button) findViewById(R.id.swEnc);
        btnPause = (Button) findViewById(R.id.pause);
        iv_snapshot = findViewById(R.id.iv_snapshot);
        bt_snapshot = findViewById(R.id.bt_snapshot);
        bt_activity_shot = findViewById(R.id.bt_activity_shot);
        iv_activity_shot = findViewById(R.id.iv_activity_shot);
        btnPause.setEnabled(false);
        mCameraView = (SrsCameraView) findViewById(R.id.glsurfaceview_camera);

        bt_snapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                screenshot(mCameraView, new SnapCameraListener() {
                    @Override
                    public void onSnap(Bitmap bitmap) {
                        iv_snapshot.setImageBitmap(bitmap);
                    }
                });

            }
        });
        bt_activity_shot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenshot(mCameraView, new SnapCameraListener() {
                    @Override
                    public void onSnap(Bitmap bitmap) {
                        Bitmap activityBitmap = ScreenShotUtil.activityShot(MainActivity.this);
                        Bitmap mergeBitmap = BitmapUtil.mergeBitmap(activityBitmap, bitmap);
                        iv_activity_shot.setImageBitmap(mergeBitmap);
                    }
                });

            }
        });

        mCameraView.setCameraCallbacksHandler(new SrsCameraView.CameraCallbacksHandler() {
            @Override
            public void onCameraParameters(Camera.Parameters params) {
                //params.setFocusMode("custom-focus");
                //params.setWhiteBalance("custom-balance");
                //etc...
            }
        });
        btnInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                initPublisher();
                btnInit.setEnabled(false);
            }
        });
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPublish.getText().toString().contentEquals("推流")) {
                    String rtmpUrl = efu.getText().toString();
                    initPublisher();
                    mPublisher.startPublish(rtmpUrl);
                    mPublisher.startCamera();
                    if (btnSwitchEncoder.getText().toString().contentEquals("soft encoder")) {
                        Toast.makeText(getApplicationContext(), "Use hard encoder", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Use soft encoder", Toast.LENGTH_SHORT).show();
                    }
                    btnPublish.setText("停止");
                    btnSwitchEncoder.setEnabled(false);
                    btnPause.setEnabled(true);
                } else if (btnPublish.getText().toString().contentEquals("停止")) {
                    mPublisher.stopPublish();
                    mPublisher.stopRecord();
                    btnPublish.setText("推流");
                    btnRecord.setText("录制");
                    btnSwitchEncoder.setEnabled(true);
                    btnPause.setEnabled(false);
                    mPublisher.startCamera();
                }
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnPause.getText().toString().equals("暂停")) {
                    mPublisher.pausePublish();
                    btnPause.setText("恢复");
                } else {
                    mPublisher.resumePublish();
                    btnPause.setText("暂停");
                }
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int totalCameras = Camera.getNumberOfCameras();
                if (totalCameras > 2) totalCameras = 2;
                mPublisher.switchCameraFace((mPublisher.getCameraId() + 1) % totalCameras);
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnRecord.getText().toString().contentEquals("录制")) {
                    if (mPublisher.startRecord(recPath)) {
                        btnRecord.setText("暂停");
                    }
                } else if (btnRecord.getText().toString().contentEquals("暂停")) {
                    mPublisher.pauseRecord();
                    btnRecord.setText("恢复");
                } else if (btnRecord.getText().toString().contentEquals("恢复")) {
                    mPublisher.resumeRecord();
                    btnRecord.setText("暂停");
                }
            }
        });

        btnSwitchEncoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnSwitchEncoder.getText().toString().contentEquals("软解码")) {
                    mPublisher.switchToSoftEncoder();
                    btnSwitchEncoder.setText("硬解码");
                } else if (btnSwitchEncoder.getText().toString().contentEquals("硬解码")) {
                    mPublisher.switchToHardEncoder();
                    btnSwitchEncoder.setText("软解码");
                }
            }
        });
    }

    private void initPublisher() {
        if (mPublisher == null) {
            mPublisher = new SrsPublisher(mCameraView);
            mPublisher.setEncodeHandler(new SrsEncodeHandler(this));
            mPublisher.setRtmpHandler(new RtmpHandler(this));
            mPublisher.setRecordHandler(new SrsRecordHandler(this));
            mPublisher.setPreviewResolution(mHeight, mWidth);
            mPublisher.setOutputResolution(mWidth, mHeight); // 这里要和preview反过来
            mPublisher.setVideoHDMode();
            mPublisher.startCamera();
            mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
        }
    }

    public void screenshot(SurfaceView surfaceView, final SnapCameraListener snapCameraListener) {
        //需要截取的长和宽
        int outWidth = surfaceView.getWidth() * 2;
        int outHeight = surfaceView.getHeight() * 2;

        final Bitmap mScreenBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(surfaceView, mScreenBitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    if (PixelCopy.SUCCESS == copyResult) {
                        Log.i("gyx", "SUCCESS ");
                        snapCameraListener.onSnap(mScreenBitmap);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                File file = BitmapUtil.bitmap2File(mScreenBitmap);
//                                Log.d(TAG, "file path:" + file.getAbsolutePath());
                            }
                        });

                    } else {
                        Log.i("gyx", "FAILED");
                        // onErrorCallback()
                    }
                }
            }, new Handler());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else {
            switch (id) {
                case R.id.cool_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.COOL);
                    break;
                case R.id.beauty_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.BEAUTY);
                    break;
                case R.id.early_bird_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EARLYBIRD);
                    break;
                case R.id.evergreen_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.EVERGREEN);
                    break;
                case R.id.n1977_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.N1977);
                    break;
                case R.id.nostalgia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.NOSTALGIA);
                    break;
                case R.id.romance_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.ROMANCE);
                    break;
                case R.id.sunrise_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNRISE);
                    break;
                case R.id.sunset_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.SUNSET);
                    break;
                case R.id.tender_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TENDER);
                    break;
                case R.id.toast_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.TOASTER2);
                    break;
                case R.id.valencia_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.VALENCIA);
                    break;
                case R.id.walden_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WALDEN);
                    break;
                case R.id.warm_filter:
                    mPublisher.switchCameraFilter(MagicFilterType.WARM);
                    break;
                case R.id.original_filter:
                default:
                    mPublisher.switchCameraFilter(MagicFilterType.NONE);
                    break;
            }
        }
        setTitle(item.getTitle());

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPublisher != null && isPermissionGranted) {
            //if the camera was busy and available again
            mPublisher.startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPublisher != null) {
            mPublisher.pauseRecord();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPublisher != null) {
            mPublisher.stopPublish();
            mPublisher.stopRecord();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mPublisher != null) {
            mPublisher.stopEncode();
            mPublisher.stopRecord();
            btnRecord.setText("录制");
            mPublisher.setScreenOrientation(newConfig.orientation);
            if (btnPublish.getText().toString().contentEquals("停止")) {
                mPublisher.startEncode();
            }
            mPublisher.startCamera();
        }
    }

    private void handleException(Exception e) {
        try {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            mPublisher.stopPublish();
            mPublisher.stopRecord();
            btnPublish.setText("推流");
            btnRecord.setText("录制");
            btnSwitchEncoder.setEnabled(true);
        } catch (Exception e1) {
            //
        }
    }

    // Implementation of SrsRtmpListener.

    @Override
    public void onRtmpConnecting(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpConnected(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoStreaming() {
    }

    @Override
    public void onRtmpAudioStreaming() {
    }

    @Override
    public void onRtmpStopped() {
        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpDisconnected() {
        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtmpVideoFpsChanged(double fps) {
        Log.i(TAG, String.format("Output Fps: %f", fps));
    }

    @Override
    public void onRtmpVideoBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Video bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Video bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpAudioBitrateChanged(double bitrate) {
        int rate = (int) bitrate;
        if (rate / 1000 > 0) {
            Log.i(TAG, String.format("Audio bitrate: %f kbps", bitrate / 1000));
        } else {
            Log.i(TAG, String.format("Audio bitrate: %d bps", rate));
        }
    }

    @Override
    public void onRtmpSocketException(SocketException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    @Override
    public void onRtmpIllegalStateException(IllegalStateException e) {
        handleException(e);
    }

    // Implementation of SrsRecordHandler.

    @Override
    public void onRecordPause() {
        Toast.makeText(getApplicationContext(), "Record paused", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordResume() {
        Toast.makeText(getApplicationContext(), "Record resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordStarted(String msg) {
        Toast.makeText(getApplicationContext(), "Recording file: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordFinished(String msg) {
        Toast.makeText(getApplicationContext(), "MP4 file saved: " + msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRecordIOException(IOException e) {
        handleException(e);
    }

    @Override
    public void onRecordIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    // Implementation of SrsEncodeHandler.

    @Override
    public void onNetworkWeak() {
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "Network weak", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNetworkResume() {
        Toast.makeText(getApplicationContext(), "Network resume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEncodeIllegalArgumentException(IllegalArgumentException e) {
        handleException(e);
    }

    interface SnapCameraListener {
        void onSnap(Bitmap bitmap);
    }

}
