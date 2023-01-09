package net.ossrs.yasea.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author bff007
 */
public class BitmapUtil {
    public static final int maxWidth = 720;
    public static final int maxHeight = 1080;

    /**
     * 检查文件是否损坏
     * Check if the file is corrupted
     *
     * @return false为不损坏，true为损坏
     */
    public static boolean checkImgCorrupted(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return (options.mCancel || options.outWidth == -1 || options.outHeight == -1);
    }

    public static int[] getImageSize(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return new int[]{options.outWidth, options.outHeight};
    }

    //读取图片旋转角度
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degree = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degree = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degree = 270;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    private static final String picPublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "fifiplat";

    public static String getPicRootDirectory() {
        return picPublicDirectory;
    }

    //校正图片旋转角度
    public static String checkPictureDegree(String path) {
        int degree = 0;
        Bitmap tempBitmap = BitmapUtil.compressImageFromFile(path, BitmapUtil.maxWidth, BitmapUtil.maxHeight);
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                degree = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                degree = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                degree = 270;
            }

            if (degree != 0) {
                tempBitmap = BitmapUtil.rotateBitmap(tempBitmap, degree);
            } else {
                return path;
            }
            File f = new File(path);
            if (f.exists()) {
                f.delete();
            }
            FileOutputStream out = new FileOutputStream(f);
            tempBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * 旋转图片，使图片保持正确的方向。
     *
     * @param bitmap  原始图片
     * @param degrees 原始图片的角度
     * @return Bitmap 旋转后的图片
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 对图片进行压缩
     *
     * @param srcPath
     * @return
     */
    public static Bitmap compressImageFromFile(String srcPath, int maxW, int maxH) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只读边,不读内容
        BitmapFactory.decodeFile(srcPath, options);

        int w = options.outWidth;
        int h = options.outHeight;

        options.inSampleSize = calcuteInSampleSize(w, h, maxW, maxH);//设置采样率
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;//默认Bitmap.Config.ARGB_8888

        return BitmapFactory.decodeFile(srcPath, options);
    }

    private static int calcuteInSampleSize(int w, int h, int maxW, int maxH) {
        int inSampleSize = 1;

        if (w > maxW && h > maxH) {
            inSampleSize = 2;
            while ((w / inSampleSize > maxW) && (h / inSampleSize > maxH)) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static File bitmap2File(Bitmap bitmap) {
        String picRootDirectory = getPicRootDirectory();
        File dir = new File(picRootDirectory);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        final String imgName = "IMG_" + System.currentTimeMillis() + ".jpg";
        final File file = new File(picRootDirectory, imgName);

        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
            bos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    @NonNull
    public static String byteToFile(final byte[] data) {
        //拍照后的处理
        String picRootDirectory =getPicRootDirectory();
        File dir = new File(picRootDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = dir + File.separator + "IMG_" + System.currentTimeMillis() + ".jpg";
        File file = new File(path);
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    // Ignore
                    e.printStackTrace();
                }
            }
        }
        return path;
    }

    private Bitmap bytes2Bimap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    public static String getImgBase64(ImageView imageView) {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bb = bos.toByteArray();

        return Base64.encodeToString(bb, Base64.NO_WRAP);
    }


    /**
     * 将图片转换成Base64编码的字符串
     */
    public static String imageToBase64(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        InputStream is = null;
        byte[] data = null;
        String result = null;
        try {
            is = new FileInputStream(path);
            //创建一个字符流大小的数组。
            data = new byte[is.available()];
            //写入数组
            is.read(data);
            //用默认的编码格式进行编码
            result = Base64.encodeToString(data, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    // 保存并刷新图库显示
    private static String saveAndRefresh(Context context, Bitmap bitmap) {
        String cacheDirectory = Environment.getExternalStorageDirectory().getPath();
        long l = System.currentTimeMillis();
        String filePath = cacheDirectory + l + ".png";

        File imagePath = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
        } finally {
            try {
                fos.close();
                //最后通知图库更新
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//扫描单个文件
                intent.setData(Uri.fromFile(imagePath));//给图片的绝对路径
                context.sendBroadcast(intent);
            } catch (Exception e) {
            }
        }

        return filePath;
    }

    /**
     * 合并两张bitmap为一张
     *
     * @param background
     * @param foreground
     * @return Bitmap
     */
    public static Bitmap mergeBitmap(Bitmap background, Bitmap foreground) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
//        foreground =zoomImg(foreground,240,360);
        int fgWidth = foreground.getWidth();
        int fgHeight = foreground.getHeight();
        Bitmap newmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newmap);
        canvas.drawBitmap(background, 0, 0, null);
        canvas.drawBitmap(foreground, 0, 240, null);
//        canvas.save(Canvas.ALL_SAVE_FLAG);
//        canvas.restore();
        return newmap;
    }

    /**
     * 缩放bitmap
     * @param bm
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }
}
