package net.ossrs.yasea;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLException;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

public class ScreenShotUtil {

    public static Bitmap takeFrame(GL10 gl, int mOutputWidth, int mOutputHeight) {
        Bitmap bmp = createBitmapFromGLSurface(0, 0, mOutputWidth, mOutputHeight, gl);
        return bmp;
    }

    // GLSurfaceView 截图
    private static Bitmap createBitmapFromGLSurface(int x, int y, int width, int height, GL10 gl) throws OutOfMemoryError {
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

    /**
     * 应用截屏
     *
     * @param activity
     * @return
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

}