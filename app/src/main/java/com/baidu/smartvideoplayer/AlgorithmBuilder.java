package com.baidu.smartvideoplayer;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author machao
 * @version v0.1
 * @since 17/12/8
 */

/**
 * 图像算法构造器
 * 调用transformXXX之后，根据add的顺序执行算法。
 */
public class AlgorithmBuilder {

    private List<Algorithm> mAlgorithmsList = new ArrayList<>();
    private Mat mMat;

    AlgorithmBuilder(Bitmap bmp) {
        mMat = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(bmp, mMat);
    }

    AlgorithmBuilder Gray() {
        mAlgorithmsList.add(new Gray());
        return this;
    }

    AlgorithmBuilder Canny() {
        mAlgorithmsList.add(new Canny());
        return this;
    }

    /**
     * 执行已添加的算法，并获取执行后的mat。方便外部调试builder不支持的算法
     *
     * @return 执行后的mat
     */
    Mat transformToMat() {
        for (Algorithm algorithm : mAlgorithmsList) {
            mMat = algorithm.execute(mMat);
        }
        return mMat;

    }

    /**
     * 执行已添加的算法，并获取执行后的bitmap
     *
     * @return 执行算法后的bitmap
     */
    Bitmap transformToBitmap() {
        for (Algorithm algorithm : mAlgorithmsList) {
            mMat = algorithm.execute(mMat);
        }
        Bitmap bmp = Bitmap.createBitmap(mMat.width(), mMat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mMat, bmp);
        return bmp;
    }

    private static class Gray implements Algorithm {

        @Override
        public Mat execute(Mat src) {
            Mat mat = new Mat();
            src.assignTo(mat, CvType.CV_8UC1);
            Imgproc.cvtColor(src, mat, Imgproc.COLOR_RGB2GRAY);
            return mat;
        }
    }

    private static class Canny implements Algorithm {

        @Override
        public Mat execute(Mat src) {
            Mat mat = new Mat();
            src.assignTo(mat, CvType.CV_8UC1);
            Imgproc.Canny(src, mat, 80, 100);
            return mat;
        }
    }

    private interface Algorithm {
        Mat execute(Mat src);
    }

}
