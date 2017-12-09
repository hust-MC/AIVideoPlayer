package com.baidu.smartvideoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements View.OnTouchListener{
    private static final String TAG = "TAG";

    private static final String VIDEO_FILE_SCHEMA = "file";
    private static final String VIDEO_CONTENT_SCHEMA = "content";
    TextureView mVideoView;
    MediaPlayer mPlayer = new MediaPlayer();

    private static final String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    private ImageView mPreview;
    private boolean mNeedSearch;
    private String mVideoUrl = "";

    private int mScreenWidth;
    private int mScreenHeight;

    private float ratioX, ratioY;


    /**
     * 通过OpenCV管理Android服务，异步初始化OpenCV
     */
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i("MCLOG", "OpenCV loaded successfully");
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handleIntent();

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        mScreenWidth = wm.getDefaultDisplay().getWidth();
        mScreenHeight = wm.getDefaultDisplay().getHeight();

        mVideoView = findViewById(R.id.video);
        mPreview = findViewById(R.id.preview);

        mVideoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("MC", "textureAvailable");
                Surface videoSurface = new Surface(surface);
                mPlayer.setSurface(videoSurface);
                try {
                    mPlayer.setDataSource(mVideoUrl);
                    mPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    // 注册OnPrepared回调，告诉播放器Prepare好之后该干嘛
                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mPlayer.start();
                        }
                    });
                    // 通知播放器开始做Prepare
                    mPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d("MC", "textureAvailable");

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d("MC", "textureAvailable");

                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.d("MC", "textureAvailable");
                if (mNeedSearch) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    long time1 = System.currentTimeMillis();
                    Log.d("MCLOG", "1 : " + time1);
                    Bitmap bmp = mVideoView.getBitmap();
                    Log.d("MCLOG", "2 : " + (System.currentTimeMillis() - time1));
/*                    AlgorithmBuilder builder = new AlgorithmBuilder(bmp);
=======
                    /*
                    AlgorithmBuilder builder = new AlgorithmBuilder(bmp);
>>>>>>> 对图像进行分块处理，得到感兴趣区域
                    Bitmap resultBmp = builder.Gray().Canny().transformToBitmap();

                    mPreview.setImageBitmap(resultBmp);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
<<<<<<< HEAD
*/
                    int width = bmp.getWidth();
                    int height = bmp.getHeight();
                    int[] rgb = new int[width * height];
                    int[] grey = new int[rgb.length];
                    int[] sobelPic = new int[rgb.length];

                    bmp.getPixels(rgb, 0, width, 0, 0, bmp.getWidth(), bmp.getHeight());

                    PictureUtils.convertToGrey(rgb, grey);
                    PictureUtils.sobel(grey, width, height, sobelPic);

                    int threshold =  PictureUtils.otsu(sobelPic, height, width);
                    Log.d("TAG", "threshold = " + threshold);

                    PictureUtils.turn2(sobelPic, height, width, threshold);
                    PictureUtils.blockSplit(sobelPic, height, width, ratioX, ratioY);



/*                    mMatSrc = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
                    Utils.bitmapToMat(bmp, mMatSrc);
                    Mat mMatGrey = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8SC1);
                    Imgproc.cvtColor(mMatSrc, mMatGrey, 1);
                    Mat mMatCanny = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8SC1);
                    Imgproc.Canny(mMatGrey, mMatCanny, 80, 100);
                    Bitmap result = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), Bitmap.Config.RGB_565 );
                    Utils.matToBitmap(mMatCanny, result);
                    */
                    Bitmap result = Bitmap.createBitmap(sobelPic, width, height, Bitmap.Config
                            .RGB_565);
                    mPreview.setImageBitmap(result);
                    result.compress(Bitmap.CompressFormat.JPEG, 80, baos);

                    ImgSearch.sampleOfNormalInterface(baos.toByteArray(), new ImageSearchResult());
                    mNeedSearch = false;

                }
            }
        });
        mVideoView.setOnTouchListener(this);
        mVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNeedSearch = true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d("MCLOG", "OpenCV library not found!");
        } else {
            Log.d("MCLOG", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getScheme() != null && intent.getData() != null) {
            if (intent.getScheme().equals(VIDEO_FILE_SCHEMA)) {
                mVideoUrl = intent.getData().getPath();
            }

            if (intent.getScheme().equals(VIDEO_CONTENT_SCHEMA)) {
                mVideoUrl = getFilePathFromMediaStore(intent.getData());
            }
        }
    }

    /**
     * 根据dataUri获取文件路径
     *
     * @param dataUri 数据uri
     * @return 文件路径
     */
    private String getFilePathFromMediaStore(Uri dataUri) {
        if (dataUri == null) {
            return null;
        }

        Cursor cursor = null;
        final String column = MediaStore.Video.Media.DATA;
        final String[] projection = {column};
        int columnIdx;

        try {
            cursor = getContentResolver().query(dataUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                columnIdx = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIdx);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            ratioX = event.getX()/mScreenWidth;
            ratioY = event.getY()/mScreenHeight;
        }
        return false;
    }

    class ImageSearchResult implements ImgSearch.ImageSearchListener {

        @Override
        public void onSuccess(final String result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onFail() {

        }
    }
}
