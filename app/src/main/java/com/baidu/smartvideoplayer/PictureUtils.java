package com.baidu.smartvideoplayer;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * @author machao10
 * @version v
 * @since 17/11/25
 */

public class PictureUtils {
    private static final int ALPHA = 0xaa << 24;
    private static final int BLACK = ALPHA | 0x0;
    private static final int WHITE = ALPHA | 0xffffff;

    private static Vertext[] vertexts = new Vertext[4];

    private static int[][] sobel_h =
            {
                    {1, 2, 1},
                    {0, 0, 0},
                    {-1, -2, -1}};
    private static int sobel_v[][] =
            {
                    {1, 0, -1},
                    {2, 0, -2},
                    {1, 0, -1}};

    static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width,
                                      int height) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    static public double getLight(int rgb[]) {
        int i;
        double bright = 0;
        for (i = 0; i < rgb.length; ++i) {
            int localTemp = rgb[i];
            int r = (localTemp | 0xff00ffff) >> 16 & 0x00ff;
            int g = (localTemp | 0xffff00ff) >> 8 & 0x0000ff;
            int b = (localTemp | 0xffffff00) & 0x0000ff;
            bright = bright + 0.299 * r + 0.587 * g + 0.114 * b;
        }
        return bright / rgb.length;
    }

    /*
     * 获取八位灰度图像 数组声明为short是为了防止在后面使用过程中发生溢出
     */
    private static void getBrightArray(int[] rgb, short[] brightArray) {
        int localTemp, r, g, b;
        for (int i = 0; i < rgb.length; ++i) {
            localTemp = rgb[i];
            r = (localTemp >> 16) & 0xff;
            g = (localTemp >> 8) & 0xff;
            b = localTemp & 0xff;

            brightArray[i] = (short) (0.299 * r + 0.587 * g + 0.114 * b);
        }
    }

    static public void convertToGrey(int[] rgb, int[] grey) {
        int bright = 0;
        short[] brightArray = new short[rgb.length];

        getBrightArray(rgb, brightArray);

        for (int i = 0; i < rgb.length; i++) {
            bright = brightArray[i] & 0xff;
            grey[i] = (ALPHA | bright << 16 | bright << 8 | bright);
        }
    }

    static public void sobel(int[] grey, int width, int height, int[] sobelPic) {
        short[] brightArray = new short[grey.length];

        int sumH = 0, sumV = 0, sum = 0;

        for (int i = 0; i < grey.length; i++) {
            brightArray[i] = (short) (grey[i] & 0xFF);
        }

        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                sumH = sumV = 0;
                for (int m = 0; m < 3; m++) {
                    for (int n = 0; n < 3; n++) {
                        sumH += sobel_h[m][n]
                                * brightArray[(i - 1 + m) * width + j - 1 + n];
                        sumV += sobel_v[m][n]
                                * brightArray[(i - 1 + m) * width + j - 1 + n];
                    }
                }
                sumH = sumH > 0 ? sumH : sumH * -1;
                sumH = sumH > 0xff ? 0xff : sumH;

                sumV = sumV > 0 ? sumV : sumV * -1;
                sumV = sumV > 0xff ? 0xff : sumV;

                sum = (sumV + sumH) > 0xff ? 0xff : (sumH + sumV);
                sobelPic[i * width + j] = (ALPHA | sum << 16 | sum << 8 | sum);
            }
        }
    }

    /**
     * 二值化
     * @param sobelImage
     * @param height
     * @param width
     * @param T
     */
    public static void turn2(int[] sobelImage, int height, int width, int T) {
        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                int gray = sobelImage[i * width + j] & 0xff;
                int result = gray >= T ? 255 : 0;
                sobelImage[i * width + j] = (ALPHA | result << 16 | result << 8 | result);
            }
        }

    }

    /**
     * 划块(20*20)
     * @param sobelImage
     * @param height
     * @param width
     */
    public static void blockSplit(int[] sobelImage, int height, int width, float centerX, float centerY) {

        final int NUMX = 20;
        final int NUMY = 20;
        int DELTAX = width / NUMX;
        int DELTAY = height / NUMY;
        int scale[][] = new int[NUMY][NUMX];

        for (int i = 0; i < NUMY; i++) {
            for (int j = 0; j < NUMX; j++) {
                scale[i][j] = 0;
            }
        }

        for (int i = 0; i < NUMY; i++) {
            for (int j = 0; j < NUMX; j++) {
                for (int k = i * DELTAY; k < (i + 1) * DELTAY - 1; k++)
                    for (int l = j * DELTAX; l < (j + 1) * DELTAX - 1; l++) {
                        if ((sobelImage[k * width + l] & 0xff) == 255) {
                            scale[i][j] = scale[i][j] + 1;
                        }
                    }
            }
        }

        for (int i = 0; i < NUMY; i++) {
            for (int j = 0; j < NUMX; j++) {
                System.out.print(scale[i][j] + " ");
            }
            System.out.print("\n");
        }

        int count = 0;
        for (int i = 0; i < NUMY; i++) {
            for (int j = 0; j < NUMX; j++) {
                count = count + scale[i][j];
            }
        }
        int average = count / (NUMX * NUMY);

        for (int i = 0; i < NUMY; i++) {
            for (int j = 0; j < NUMX; j++) {
                if (scale[i][j] > average) {
                    scale[i][j] = 1;
                    System.out.print("1 ");
                } else {
                    scale[i][j] = 0;
                    System.out.print("0 ");
                }
            }
            System.out.print("\n");
        }

        int x = (int) Math.floor((double) NUMX * centerX);
        int y = (int) Math.floor((double) NUMY * centerY);

        Rect rect = findRect(scale, NUMY, NUMX, y, x);

        Log.d("TAG", "[" + rect.x +"," + rect.y + ", " + rect.width + ", " + rect.height + "]");
        drawRect(sobelImage, height, width, new Rect(rect.x * DELTAX, rect.y * DELTAY,
                rect.width * DELTAX - 1, rect.height * DELTAY - 1));

    }

    private static Rect findRect(int scale[][], int blockY, int blockX, int y, int x) {
        Rect rect = new Rect();

        int left = x, right = x;
        int top = y, bottom = y;

        // 点击位置是否是边缘
        boolean isEdge = scale[y][x] == 1;
        boolean tempEdge = isEdge;

        while (left > 0) {
            if (tempEdge) {
                if (scale[y][left] == 0)
                    break;
            }
            else {
                if (scale[y][left] == 1)
                    tempEdge = true;
            }
            left--;
        }

        tempEdge = isEdge;
        while (top > 0) {
            if (tempEdge) {
                if (scale[top][x] == 0)
                    break;
            }
            else {
                if (scale[top][x] == 1)
                    tempEdge = true;
            }
            top--;
        }

        tempEdge = isEdge;
        while (right < blockX) {
            if (tempEdge) {
                if (scale[y][right] == 0)
                    break;
            }
            else {
                if (scale[y][right] == 1)
                    tempEdge = true;
            }
            right ++;
        }

        tempEdge = isEdge;
        while (bottom < blockY) {
            if (tempEdge) {
                if (scale[bottom][x] == 0)
                    break;
            }
            else {
                if (scale[bottom][x] == 1)
                    tempEdge = true;
            }
            bottom ++;
        }

        rect.x = left;
        rect.y = top;
        rect.height = bottom - top + 1;
        rect.width = right - left + 1;

        return rect;
    }

    /**
     * 绘制矩形
     * @param image
     * @param height
     * @param width
     * @param rect
     */
    private static void drawRect(int[] image, int height, int width, Rect rect) {
        int left = rect.x;
        int right = left + rect.width;
        right = right < width ? right : width-1;

        Log.d("TAG", ">>>>left:" + left);
        Log.d("TAG", ">>>>right:" + right);

        int top = rect.y;
        int bottom = top + rect.height;
        bottom = bottom < height ? bottom : height - 1;
        Log.d("TAG", ">>>>top:" + top);
        Log.d("TAG", ">>>>bottom:" + bottom);

        Log.d("TAG", ">>>>width:" + width);
        Log.d("TAG", ">>>>height:" + height);

        for (int k = top; k < bottom; k++) {
            image[k * width + left] = (0xff | 0X00 << 16 | 0xff << 8 | 0);
            image[k * width + right] = (0xff | 0X00 << 16 | 0xff << 8 | 0);
        }
        for (int l = left; l < right; l++) {
            image[top * width + l] = ( 0X00 << 16 | 0xff << 8 | 0);
            image[bottom * width + l] = (  0X00 << 16 | 0xff << 8 | 0);
        }
    }

    /**
     * 大津阈值
     * @param sobelImage
     * @param height
     * @param width
     * @return
     */
    public static int otsu(int[] sobelImage, int height, int width) {
        int hist[] = new int[256];
        for (int i = 0; i < 256; i++) {
            hist[i] = 0;
        }

        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                int gray = sobelImage[i * width + j] & 0xff;
                hist[gray] = hist[gray] + 1;
            }
        }

        float w0, w1, u0tmp, u1tmp, u0, u1, deltaTmp, deltaMax = 0;

        int threshold = 0;

        //遍历所有从0到255灰度级的阈值分割条件，测试哪一个的类间方差最大
        for(int i = 0; i < 256; i++) {
            w0 = w1 = u0tmp = u1tmp = u0 = u1 = deltaTmp = 0;

            for(int j = 0; j < 256; j++)
            {
                if(j <= i)   //背景部分
                {
                    w0 += hist[j];
                    u0tmp += j * hist[j];
                }
                else   //前景部分
                {
                    w1 += hist[j];
                    u1tmp += j * hist[j];
                }
            }
            u0 = u0tmp / w0;
            u1 = u1tmp / w1;
            deltaTmp = (float)(w0 *w1* (u0 - u1) * (u0 - u1));
            if(deltaTmp > deltaMax)
            {
                deltaMax = deltaTmp;
                threshold = i;
            }
        }

        return threshold;

    }


    static int[] findFrame(int[] pic, int width, int height) {
        int[] frame = new int[pic.length];
        // for (Vertext v : vertexts)
        // {
        // v = new Pictures().new Vertext();
        // }

        for (int i = 0; i < width; i++)                // 上边缘
        {
            for (int j = 0; j < height / 2; j++) {
                if (pic[j * width + i] == WHITE) {
                    frame[j * width + i] = WHITE;
                    break;
                }
            }

            for (int j = height - 1; j > height / 2; j--) {
                if (pic[j * width + i] == WHITE) {
                    frame[j * width + i] = WHITE;
                    break;
                }
            }
        }

        for (int i = 0; i < height; i++)                // 左边缘
        {
            for (int j = 0; j < width / 2; j++) {
                if (pic[i * width + j] == WHITE) {
                    frame[i * width + j] = WHITE;

                    break;
                }
            }
            for (int j = width - 1; j > width / 2; j--) {
                if (pic[i * width + j] == WHITE) {
                    frame[i * width + j] = WHITE;
                    break;
                }
            }
        }

        return frame;
    }

    public static void skeleton(Bitmap bitmap, ImageView picture_result) {
//        System.loadLibrary("opencv_java3");
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(src, src, 1, 255,Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Mat dst = src.clone();

        int K = 0;//腐蚀至消失的次数
        Mat element =  Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3,3));
        Mat res = null;//骨架操作的结果
        do{
            Mat  dst2 = new Mat();
            Imgproc.morphologyEx(dst, dst2,  Imgproc.MORPH_OPEN, element);
            Mat tmp = new Mat();
            Core.subtract(dst, dst2, tmp);
            if(res == null){
                res = tmp;
            }else {
                Core.add(tmp, res, res);
            }
            K++;
            Imgproc.erode(src, dst, element, new Point(-1, -1), K);
        }while (Core.countNonZero(dst) > 0);

//        ConstantMorph.MY_MAT = res;//操作结果
//        ConstantMorph.MY_COUNT = K;

        Bitmap tmpbitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(res, tmpbitmap);
        picture_result.setImageBitmap(tmpbitmap);

    }

    class Vertext {
        int x = 0;
        int y = 0;
    }
}