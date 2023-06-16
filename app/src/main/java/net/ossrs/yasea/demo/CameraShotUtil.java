package net.ossrs.yasea.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.opengl.GLException;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import net.ossrs.yasea.SrsPublisher;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class CameraShotUtil {

    // 通过GLSurfaceView像素获取Bitmap
    private static Bitmap glSurfaceView2Bitmap(int x, int y, int width, int height, GL10 gl) throws OutOfMemoryError {
        int bitmapBuffer[] = new int[width * height];
        int bitmapSource[] = new int[width * height];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            gl.glReadPixels(x, y, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                    intBuffer);
            int offset1, offset2;

            for (int i = 0; i < height; i++) {
                offset1 = i * width;
                offset2 = (height - i - 1) * width;
                for (int j = 0; j < width; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }

        return Bitmap.createBitmap(bitmapSource, width, height, Bitmap.Config.RGB_565);
    }

    //通过PixelCopy获取Bitmap
    public static void surfaceView2Bitmap(SurfaceView surfaceView, final SnapCameraListener snapCameraListener) {
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

                    } else {
                        Log.i("gyx", "FAILED");
                    }
                }
            }, new Handler());
        }
    }

    public static void getCameraShot(SrsPublisher publisher, final SnapCameraListener snapCameraListener) {
        Camera mCamera = publisher.getCamera();

        mCamera.takePicture(null, null, (byte[] data, Camera camera) -> {
            camera.startPreview();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] newData = correctPhotoDegreeFromCamera(publisher, camera, data);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(newData, 0, newData.length);
                        snapCameraListener.onSnap(bitmap);
                        String fileName = !publisher.isCameraFaceFront() ? "/back.jpeg" : "/front.jpeg";
                        String path = AppApplication.getContext().getExternalCacheDir().getAbsolutePath() + fileName;

                        FileOutputStream outputStream = new FileOutputStream(path);
                        outputStream.write(newData);
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        });
    }

    static public byte[] correctPhotoDegreeFromCamera(SrsPublisher publisher, Camera camera, byte[] data) {
        Camera.Size size = camera.getParameters().getPictureSize();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(publisher.getCameraId(), cameraInfo);

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Matrix matrix = new Matrix();

        matrix.postRotate(cameraInfo.orientation);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, size.width, size.height, matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
        byte[] newData = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newData;
    }

    /**
     * 应用截屏
     */
    public static Bitmap activityShot(Activity activity) {
        /*获取windows中最顶层的view*/
        View view = activity.getWindow().getDecorView();
        //允许当前窗口保存缓存信息
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        //获取状态栏高度
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        WindowManager windowManager = activity.getWindowManager();
        //获取屏幕宽和高
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        //去掉状态栏
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, statusBarHeight, width, height - statusBarHeight);
        //销毁缓存信息
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    interface SnapCameraListener {
        void onSnap(Bitmap bitmap);
    }
}