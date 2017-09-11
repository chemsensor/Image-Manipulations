package org.opencv.samples.imagemanipulations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.MatOfPoint;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;



import android.app.Activity;
import android.icu.text.DecimalFormat;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class ImageManipulationsActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_HIST      = 1;
    public static final int      VIEW_MODE_CANNY     = 2;
    public static final int      VIEW_MODE_SEPIA     = 3;
    public static final int      VIEW_MODE_SOBEL     = 4;
    public static final int      VIEW_MODE_ZOOM      = 5;
    public static final int      VIEW_MODE_PIXELIZE  = 6;
    public static final int      VIEW_MODE_POSTERIZE = 7;

    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewHist;
    private MenuItem             mItemPreviewCanny;
    private MenuItem             mItemPreviewSepia;
    private MenuItem             mItemPreviewSobel;
    private MenuItem             mItemPreviewZoom;
    private MenuItem             mItemPreviewPixelize;
    private MenuItem             mItemPreviewPosterize;
    private CameraBridgeViewBase mOpenCvCameraView;

    private Size                 mSize0;

    private Mat                  mIntermediateMat;
    private Mat                  mLabMat;
    private MatOfFloat           mLogMat;
    private Mat                  mOutMat;
    private Mat                  mMaskedMat;
    private Mat                  mMat0;
    private Mat                  mIntermediateMask1;
    private Mat                  mIntermediateMask2;
    private MatOfInt             mChannels[];
    private MatOfInt             mHistSize;
    private int                  mHistSizeNum = 25;
    private int                  frameNumber;
    private MatOfFloat           mRanges;
    private Mat                  mData;
    private Scalar               mColorsRGB[];
    private Scalar               mColorsHSV[];
    private Scalar               mColorsLAB[];
    private Scalar               mColorsHue[];
    private Scalar               mask_color_lower_limit;
    private Scalar               mask_color_upper_limit;
    private long                 startTime;
    private double               elapsedTime;
    private Scalar               avgSignals;
    private double               avgSignal;
    private int                  firstFrame=0;
    private Point                mP1;
    private Point                mP2;
    private Point                mP3;
    private Scalar               hsvMean;
    private Scalar               rgbMean;
    private Scalar               absMean;
    private Scalar               labMean;
    private float                mBuff[];
    private Mat                  mSepiaKernel;

    public static int           viewMode = VIEW_MODE_HIST;

    public MediaRecorder recorder;
    public FileWriter writer;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ImageManipulationsActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView = findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewHist  = menu.add("Histograms");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewSepia = menu.add("Sepia");
        mItemPreviewSobel = menu.add("Sobel");
        mItemPreviewZoom  = menu.add("Zoom");
        mItemPreviewPixelize  = menu.add("Pixelize");
        mItemPreviewPosterize = menu.add("Posterize");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        if (item == mItemPreviewHist)
            viewMode = VIEW_MODE_HIST;
        else if (item == mItemPreviewCanny)
            viewMode = VIEW_MODE_CANNY;
        else if (item == mItemPreviewSepia)
            viewMode = VIEW_MODE_SEPIA;
        else if (item == mItemPreviewSobel)
            viewMode = VIEW_MODE_SOBEL;
        else if (item == mItemPreviewZoom)
            viewMode = VIEW_MODE_ZOOM;
        else if (item == mItemPreviewPixelize)
            viewMode = VIEW_MODE_PIXELIZE;
        else if (item == mItemPreviewPosterize)
            viewMode = VIEW_MODE_POSTERIZE;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {

        mIntermediateMat = new Mat();
        mLabMat = new Mat();
        mLogMat=new MatOfFloat();
        mOutMat = new Mat();
        mMaskedMat= new Mat();
        mIntermediateMask1 = new Mat();
        mIntermediateMask2 = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mData = new Mat(6000,42,CvType.CV_64F);
        //mData = new Mat(2,42,CvType.CV_32F);
        mMat0  = new Mat();
        mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
        mColorsHSV = new Scalar[] { new Scalar(200, 200, 0, 255), new Scalar(255, 255, 255, 255), new Scalar(128, 128, 128, 255) };
        mColorsLAB = new Scalar[] { new Scalar(192, 192, 192, 255), new Scalar(200, 200, 0, 255), new Scalar(0, 200, 200, 255) };
        mColorsHue = new Scalar[] {
                new Scalar(255, 0, 0, 255),   new Scalar(255, 60, 0, 255),  new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255),  new Scalar(20, 255, 0, 255),  new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255),  new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255),  new Scalar(0, 0, 255, 255),   new Scalar(64, 0, 255, 255),  new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255),  new Scalar(255, 0, 0, 255)
        };
        //mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();
        mP3 = new Point();
        hsvMean= new Scalar(0.0,0.0,0.0);
        rgbMean= new Scalar(0.0,0.0,0.0);
        absMean= new Scalar(0.0,0.0,0.0);
        labMean= new Scalar(0.0,0.0,0.0);
        //mFrameData = new double[6000][42];
        startTime=System.nanoTime();
        frameNumber=-1;

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
        mask_color_lower_limit= new Scalar(0,100,0);
        mask_color_upper_limit= new Scalar(255,255,255);

        //mvideoWriter = new VideoWriter("test.avi", VideoWriter.fourcc('M','J','P','G'), 10, new Size(1920,1080));
        //mvideoWriter.open("test.avi", VideoWriter.fourcc('M','J','P','G'), 10, new Size(1920,1080));
            Log.d(TAG, "Starting recorder");
            recorder = new MediaRecorder();
            recorder.reset();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            CamcorderProfile cphigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            recorder.setProfile(cphigh);
            recorder.setOutputFile("/sdcard/ProcessedVideo.mp4");
            //recorder.setVideoSize(mOpenCvCameraView.mFrameWidth, mOpenCvCameraView.mFrameHeight);
            //recorder.setVideoSize(1280,720);
            //recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            //recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            //recorder.setOnInfoListener(this);
            //recorder.setOnErrorListener(this);
            Log.d(TAG, String.valueOf(cphigh.videoBitRate));
            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e("debug mediarecorder", "not prepare IOException");
            }
            mOpenCvCameraView.setRecorder(recorder);
            recorder.start();

        try {
            File gpxfile = new File("/sdcard/ProcessedData.txt");
            writer = new FileWriter(gpxfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();
        mIntermediateMat = null;

        if (mLogMat != null)
            mLogMat.release();
        mLogMat = null;

        if (mIntermediateMask1 != null)
            mIntermediateMask1.release();
        mIntermediateMask1 = null;

        if (mIntermediateMask2 != null)
            mIntermediateMask2.release();
        mIntermediateMask2 = null;

        if (mOutMat != null)
            mOutMat.release();
        mOutMat = null;

        if (mMaskedMat != null)
            mMaskedMat.release();
        mMaskedMat = null;

        recorder.release();

        try {
            writer.flush();
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
        //mvideoWriter.release();

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        //mvideoWriter.write(rgba);

        frameNumber++;
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        switch (ImageManipulationsActivity.viewMode) {
        case ImageManipulationsActivity.VIEW_MODE_RGBA:
            break;

        case ImageManipulationsActivity.VIEW_MODE_HIST:
            elapsedTime=(System.nanoTime()-startTime)/1.0e9;
            Imgproc.cvtColor(rgba,mIntermediateMat,Imgproc.COLOR_RGB2HSV_FULL);
            Imgproc.cvtColor(rgba,mLabMat,Imgproc.COLOR_RGB2Lab);

            Core.inRange(mIntermediateMat,new Scalar(0,10,0),new Scalar(30,255,255),mIntermediateMask1);
            Core.inRange(mIntermediateMat,new Scalar(225,10,0),new Scalar(255,255,255),mIntermediateMask2);
            Core.bitwise_or(mIntermediateMask1,mIntermediateMask2,mIntermediateMask2);
            mask_color_lower_limit= new Scalar(0,0,0);
            mask_color_upper_limit= new Scalar(255,40,255);
            Core.inRange(mIntermediateMat,mask_color_lower_limit,mask_color_upper_limit,mIntermediateMask1);
            Core.bitwise_or(mIntermediateMask1,mIntermediateMask2,mIntermediateMask1);
            Core.bitwise_not(mIntermediateMask1,mMat0);
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat mHierarchy = new Mat();
            Imgproc.findContours(mMat0, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            double maxArea = 0;
            int LargestContour = 0;
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                if (maxArea < contourArea) {
                    maxArea = contourArea;
                    LargestContour = contourIdx;
                }
            }
            mMat0.setTo(new Scalar(0));
            if (maxArea>(0))
                Imgproc.drawContours(mMat0, contours, LargestContour, new Scalar(255), -1);

            mOutMat.create(sizeRgba,rgba.type());
            mOutMat.setTo(new Scalar(0, 0, 0, 0));
            mMaskedMat.setTo(new Scalar(0, 0, 0));
            Core.bitwise_and(rgba,rgba,mMaskedMat,mMat0);
            Rect rect = new Rect();
            if (maxArea>(0)) {
                Imgproc.drawContours(mMaskedMat, contours, LargestContour, new Scalar(255, 0, 0, 255), 2);
                rect = Imgproc.boundingRect(contours.get(LargestContour));
            }

            Imgproc.rectangle(mMaskedMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 255), 5);
            Core.inRange(rgba.submat(rect), new Scalar(0, 0, 0), new Scalar(0, 0, 0), mIntermediateMask1);
            rgba.submat(rect).convertTo(mLogMat, CvType.CV_32F);
            mLogMat.setTo(new Scalar(1), mIntermediateMask1);
            Core.log(mLogMat, mLogMat);
            absMean = Core.mean(mLogMat, mMat0.submat(rect));

            Mat zoomCorner = mOutMat.submat(rows / 2, rows, 0, cols / 2 );
            Imgproc.resize(mMaskedMat, zoomCorner, zoomCorner.size());
            zoomCorner = mOutMat.submat(0, rows / 2, 0, cols / 2 );
            Imgproc.resize(rgba, zoomCorner, zoomCorner.size());
            zoomCorner.release();

            //String histMeanString[]=new String[3];
            //DecimalFormat histFormat= new DecimalFormat("#00.0");
            rgbMean=Core.mean(rgba.submat(rect),mMat0.submat(rect));
            Mat hist = new Mat();
            int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
            if(thikness > 5) thikness = 5;
            int offset = (int) (sizeRgba.width /2)+10;

            // RGB
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(rgba.submat(rect)), mChannels[c], mMat0.submat(rect), hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/6, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = (sizeRgba.height/4)-40;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Imgproc.line(mOutMat, mP1, mP2, mColorsRGB[c], thikness);
                }
                mP3.x=offset + (c * (mHistSizeNum + 10)) * thikness;
                mP3.y = (sizeRgba.height/4)-1;
                //histMeanString[c]=""+histFormat.format(rgbMean.val[c]);
                //Imgproc.putText(mOutMat,histMeanString[c],mP3,1,2.0,new Scalar (255,255,255,255));
                Imgproc.putText(mOutMat,String.format("%1$,.2f", rgbMean.val[c]),mP3,1,2.0,new Scalar (255,255,255,255));
            }
            // HSV
            hsvMean=Core.mean(mIntermediateMat.submat(rect),mMat0.submat(rect));
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(mIntermediateMat.submat(rect)), mChannels[c], mMat0.submat(rect), hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/6, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = (sizeRgba.height/2)-40;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    if (c==0) {
                        Imgproc.line(mOutMat, mP1, mP2, mColorsHue[h], thikness);
                    }
                    else {
                        Imgproc.line(mOutMat, mP1, mP2, mColorsHSV[c], thikness);
                    }
                }
                mP3.x=offset + (c * (mHistSizeNum + 10)) * thikness;
                mP3.y = (sizeRgba.height/2)-1;
                //histMeanString[c]=""+histFormat.format(hsvMean.val[c]);
                //Imgproc.putText(mOutMat,histMeanString[c],mP3,1,2.0,new Scalar (255,255,255,255));
                Imgproc.putText(mOutMat,String.format("%1$,.2f", hsvMean.val[c]),mP3,1,2.0,new Scalar (255,255,255,255));
            }
            // Lab
            labMean=Core.mean(mLabMat.submat(rect),mMat0.submat(rect));
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(mLabMat.submat(rect)), mChannels[c], mMat0.submat(rect), hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/6, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (sizeRgba.width/4)+ (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = (sizeRgba.height/4)-40;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Imgproc.line(mOutMat, mP1, mP2, mColorsLAB[c], thikness);
                }
                mP3.x=offset + (sizeRgba.width/4)+(c * (mHistSizeNum + 10)) * thikness;
                mP3.y = (sizeRgba.height/4)-1;
                //histMeanString[c]=""+histFormat.format(rgbMean.val[c]);
                //Imgproc.putText(mOutMat,histMeanString[c],mP3,1,2.0,new Scalar (255,255,255,255));
                Imgproc.putText(mOutMat,String.format("%1$,.2f", labMean.val[c]),mP3,1,2.0,new Scalar (255,255,255,255));
            }

            //mData.submat(frameNumber,frameNumber+1,0,1).put(frameNumber,0,(System.nanoTime()-startTime)/1.0e9);

            mData.put(frameNumber,0,elapsedTime);
            mData.put(frameNumber,10,maxArea);
            mData.put(frameNumber,11,rgbMean.val[0]);
            mData.put(frameNumber,12,rgbMean.val[1]);
            mData.put(frameNumber,13,rgbMean.val[2]);
            mData.put(frameNumber,14,hsvMean.val[0]);
            mData.put(frameNumber,15,hsvMean.val[1]);
            mData.put(frameNumber,16,hsvMean.val[2]);
            mData.put(frameNumber,17,absMean.val[2]-absMean.val[1]);
            mData.put(frameNumber,18,absMean.val[1]-absMean.val[0]);
            mData.put(frameNumber,19,absMean.val[2]-absMean.val[0]);
            mData.put(frameNumber,20,frameNumber);
            //mData.put(frameNumber,5,rows);
            //mData.put(frameNumber,6,cols);

            if (elapsedTime>10) {
                if (firstFrame==0) firstFrame=frameNumber;
                Core.inRange(mData.submat(0, frameNumber + 1, 0, 1), new Scalar(elapsedTime - 10), new Scalar(elapsedTime), mIntermediateMask1);
                Mat wLocMat = new Mat();
                Core.findNonZero(mIntermediateMask1,wLocMat);
                Core.MinMaxLocResult mmr = Core.minMaxLoc(mData.submat(0, frameNumber + 1, 0, 1),mIntermediateMask1);
                //Log.d(TAG, String.valueOf((int) mmr.minLoc.y));
                //avgSignals = Core.mean(mData.submat((int) mmr.minLoc.y, frameNumber + 1, 11, 20));
                //Log.d(TAG, String.valueOf(avgSignals.val[0]));
                for(int h=0; h<9; h++) {
                    avgSignals = Core.mean(mData.submat(0, frameNumber + 1, h+11, h+12), mIntermediateMask1);
                    avgSignal = avgSignals.val[0];
                    mData.put(frameNumber, h+1, avgSignal);
                }
                Mat regResult = new Mat();
                Mat onesMat=Mat.ones(mData.submat((int) mmr.minLoc.y, frameNumber + 1, 0, 1).size(),mData.type());
                List<Mat> src = Arrays.asList(onesMat, mData.submat((int) mmr.minLoc.y, frameNumber + 1, 0, 1));
                Mat Yreg = new Mat();
                Core.hconcat(src,Yreg);
                //Core.solve(mData.submat((int) mmr.minLoc.y, frameNumber + 1, 0, 1),mData.submat((int) mmr.minLoc.y, frameNumber + 1, 20, 21),regResult,Core.DECOMP_SVD);
                Core.solve(Yreg,mData.submat((int) mmr.minLoc.y, frameNumber + 1, 19, 20),regResult,Core.DECOMP_SVD);
                //Log.d(TAG, String.valueOf(regResult.get(0,0)[1]));
                //Log.d(TAG, "yh="+String.valueOf(Yreg.size().height)+" yw="+String.valueOf(Yreg.size().width));
                //Log.d(TAG, "xh="+String.valueOf(mData.submat((int) mmr.minLoc.y, frameNumber + 1, 20, 21).size().height)+" xw="+String.valueOf(mData.submat((int) mmr.minLoc.y, frameNumber + 1, 20, 21).size().width));
                //Log.d(TAG, "rh="+String.valueOf(regResult.size().height)+" rw="+String.valueOf(regResult.size().width));
                //Log.d(TAG, String.valueOf(regResult.get(0,0)[0]));
                //Log.d(TAG, String.valueOf(regResult.get(1,0)[0]));
                mData.put(frameNumber, 21, regResult.get(1,0)[0]);
            }

            double[] tempx = new double[frameNumber];
            mData.colRange(0,1).get(0, 0, tempx);
            double[] tempSat = new double[frameNumber];
            mData.colRange(15,16).get(0, 0, tempSat);
            double[] tempR = new double[frameNumber];
            mData.colRange(11,12).get(0, 0, tempR);
            double[] tempG = new double[frameNumber];
            mData.colRange(12,13).get(0, 0, tempG);
            double[] tempB = new double[frameNumber];
            mData.colRange(13,14).get(0, 0, tempB);

            double[] tempAbsRG = new double[frameNumber];
            mData.colRange(8,9).get(0, 0, tempAbsRG);
            Core.MinMaxLocResult tempAbsRGminmax = Core.minMaxLoc(mData.submat(firstFrame, frameNumber + 1, 8, 9));
            double[] tempAbsRB = new double[frameNumber];
            mData.colRange(9,10).get(0, 0, tempAbsRB);
            Core.MinMaxLocResult tempAbsRBminmax = Core.minMaxLoc(mData.submat(firstFrame, frameNumber + 1, 9, 10));
            double[] tempAbsRBslope = new double[frameNumber];
            mData.colRange(21,22).get(0, 0, tempAbsRBslope);
            Core.MinMaxLocResult tempAbsRBslopeminmax = Core.minMaxLoc(mData.submat(firstFrame, frameNumber + 1, 21, 22));
            mP2.x=0;
            for(int dp=0; dp<frameNumber; dp++) {
                mP1.x = (int) (sizeRgba.width/2)+ (tempx[dp]*(sizeRgba.width/4)/150);
                mP1.y = (int) (sizeRgba.height)-(tempSat[dp]*(sizeRgba.height/4)/255);
                Imgproc.circle(mOutMat,mP1,1,new Scalar(0, 200, 200, 255));
                mP1.y = (int) (sizeRgba.height)-(sizeRgba.height/4)-(tempR[dp]*(sizeRgba.height/4)/255);
                Imgproc.circle(mOutMat,mP1,1,new Scalar(200, 0, 0, 255));
                mP1.y = (int) (sizeRgba.height)-(sizeRgba.height/4)-(tempG[dp]*(sizeRgba.height/4)/255);
                Imgproc.circle(mOutMat,mP1,1,new Scalar(0, 200, 0, 255));
                mP1.y = (int) (sizeRgba.height)-(sizeRgba.height/4)-(tempB[dp]*(sizeRgba.height/4)/255);
                Imgproc.circle(mOutMat,mP1,1,new Scalar(0, 0, 200, 255));

                if (mP2.x!=0 && dp>firstFrame && firstFrame!=0) {
                    mP1.x = mP2.x;
                    mP1.y = mP2.y;
                    mP2.x = (int) (sizeRgba.width / 2 + sizeRgba.width / 4) + (tempx[dp] * (sizeRgba.width / 4) / 150);
                    if (tempAbsRBminmax.maxVal - tempAbsRBminmax.minVal > 0)
                        mP2.y = (int) (sizeRgba.height) - (sizeRgba.height / 4) - ((tempAbsRB[dp]- tempAbsRBminmax.minVal) * (sizeRgba.height / 4) / (tempAbsRBminmax.maxVal - tempAbsRBminmax.minVal));
                    else
                        mP2.y = (int) (sizeRgba.height) - (sizeRgba.height / 4);
                    if (Math.abs(tempAbsRBslope[dp])>0.001)
                        Imgproc.line(mOutMat, mP1, mP2, new Scalar(255, 0, 0, 255), 2);
                    else
                        Imgproc.line(mOutMat, mP1, mP2, new Scalar(0, 255, 0, 255), 2);
                    mP1.y = mP3.y;
                    mP3.x=mP2.x;
                    if (tempAbsRBslopeminmax.maxVal - tempAbsRBslopeminmax.minVal > 0)
                        mP3.y = (int) (sizeRgba.height) - ((tempAbsRBslope[dp] - tempAbsRBslopeminmax.minVal)* (sizeRgba.height / 4) / (tempAbsRBslopeminmax.maxVal - tempAbsRBslopeminmax.minVal));
                    else
                        mP3.y = (int) (sizeRgba.height);
                    Imgproc.line(mOutMat, mP1, mP3, new Scalar(255, 0, 255, 255), 2);
                }
                if ((mP2.x==0 && dp>firstFrame && firstFrame!=0)) {
                    mP2.x = (int) (sizeRgba.width / 2 + sizeRgba.width / 4) + (tempx[dp] * (sizeRgba.width / 4) / 150);
                    if (tempAbsRBminmax.maxVal - tempAbsRBminmax.minVal > 0)
                        mP2.y = (int) (sizeRgba.height) - (sizeRgba.height / 4) - ((tempAbsRB[dp]- tempAbsRBminmax.minVal) * (sizeRgba.height / 4) / (tempAbsRBminmax.maxVal - tempAbsRBminmax.minVal));
                    else
                        mP2.y = (int) (sizeRgba.height) - (sizeRgba.height / 4);
                    mP3.x=mP2.x;
                    if (tempAbsRBslopeminmax.maxVal - tempAbsRBslopeminmax.minVal > 0)
                        mP3.y = (int) (sizeRgba.height) - ((tempAbsRBslope[dp] - tempAbsRBslopeminmax.minVal)* (sizeRgba.height / 4) / (tempAbsRBslopeminmax.maxVal - tempAbsRBslopeminmax.minVal));
                    else
                        mP3.y = (int) (sizeRgba.height);
                }

                mP1.y = (int) (sizeRgba.height)-(tempAbsRB[dp]*(sizeRgba.height/4)/255);
                //Imgproc.circle(mOutMat,mP1,1,new Scalar(200, 0, 200, 255));
                mP1.y = (int) (sizeRgba.height)-(tempAbsRBslope[dp]*(sizeRgba.height/4)/255);
                //Imgproc.circle(mOutMat,mP1,1,new Scalar(200, 0, 200, 255));

            }

            String dump = mData.submat(frameNumber,frameNumber+1,0,24).dump();
            Log.d(TAG, dump);
            //Log.d(TAG, absMean.toString());
            //Log.d(TAG, rgbMean.toString());
            //Log.d(TAG, hsvMean.toString());
            //double tempDouble=mData.get(frameNumber,0)[0];
            //Log.d(TAG, String.valueOf(mData.get(frameNumber,0)[0]));

            /*mFrameData[frameNumber][0]=(System.nanoTime()-startTime)/1.0e9;
            mFrameData[frameNumber][2]=maxArea;
            mFrameData[frameNumber][21]=hsvMean.val[0];
            mFrameData[frameNumber][22]=hsvMean.val[1];
            mFrameData[frameNumber][23]=hsvMean.val[2];
            histMeanString[0]=histFormat.format(mFrameData[frameNumber][22]);*/

            //histMeanString[0]=histFormat.format(elapsedTime);
            mP3.x= sizeRgba.width/2+sizeRgba.width/4;
            mP3.y= (sizeRgba.height/4)+40;
            Imgproc.putText(mOutMat,"T="+String.format("%1$,.2f", elapsedTime)+" s",mP3,1,2.0,new Scalar (255,255,255,255));

            //histMeanString[0]=histFormat.format(frameNumber);
            mP3.x=sizeRgba.width/2+sizeRgba.width/4;
            mP3.y = (sizeRgba.height/4)+70;
            Imgproc.putText(mOutMat,"Frame="+String.format("%1$,.2f", (double) frameNumber),mP3,1,2.0,new Scalar (255,255,255,255));

            //histMeanString[0]=histFormat.format(tempSat[frameNumber-1]);
            //histMeanString[0]=String.valueOf(mData.get(frameNumber,5)[0]);
            mP3.x=sizeRgba.width/2+sizeRgba.width/4+sizeRgba.width/8;
            mP3.y = (sizeRgba.height/4)+40;
            if (Math.abs(mData.get(frameNumber, 21)[0])>0.001)
                Imgproc.putText(mOutMat,"RBavg="+String.format("%1$,.4f", mData.get(frameNumber,9)[0]),mP3,1,2.0,new Scalar (255,0,0,255));
            else
                Imgproc.putText(mOutMat,"RBavg="+String.format("%1$,.4f", mData.get(frameNumber,9)[0]),mP3,1,2.0,new Scalar (0,255,0,255));

            mP3.x=sizeRgba.width/2+sizeRgba.width/4+sizeRgba.width/8;
            mP3.y = (sizeRgba.height/4)+70;
            Imgproc.putText(mOutMat,"a(G/B)="+String.format("%1$,.4f", mData.get(frameNumber,17)[0]),mP3,1,2.0,new Scalar (255,255,255,255));

            mP3.x=sizeRgba.width/2+sizeRgba.width/4+sizeRgba.width/8;
            mP3.y = (sizeRgba.height/4)+100;
            Imgproc.putText(mOutMat,"a(R/G)="+String.format("%1$,.4f", mData.get(frameNumber,18)[0]),mP3,1,2.0,new Scalar (255,255,255,255));

            mP3.x=sizeRgba.width/2+sizeRgba.width/4+sizeRgba.width/8;
            mP3.y = (sizeRgba.height/4)+130;
            Imgproc.putText(mOutMat,"a(R/B)="+String.format("%1$,.4f", mData.get(frameNumber,19)[0]),mP3,1,2.0,new Scalar (255,255,255,255));

            //histMeanString[0]=histFormat.format(maxArea);
            mP3.x=sizeRgba.width/2+sizeRgba.width/4;
            mP3.y = (sizeRgba.height/4)+100;
            Imgproc.putText(mOutMat,"Pix="+String.format("%1$,.2f", (double) maxArea),mP3,1,2.0,new Scalar (255,255,255,255));

            //histMeanString[0]=histFormat.format(frameNumber/elapsedTime);
            mP3.x=sizeRgba.width/2+sizeRgba.width/4;
            mP3.y = (sizeRgba.height/4)+130;
            Imgproc.putText(mOutMat,"FPS="+String.format("%1$,.2f", frameNumber/elapsedTime),mP3,1,2.0,new Scalar (255,255,255,255));

            /* Saturation
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[1], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/4, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Imgproc.line(rgba, mP1, mP2, mWhilte, thikness);
            }
            // Hue
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[0], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/4, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (4 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Imgproc.line(rgba, mP1, mP2, mColorsHue[h], thikness);
            }*/
            try {
                writer.append(dump+"\r\n");
            }catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case ImageManipulationsActivity.VIEW_MODE_CANNY:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_SOBEL:
            Mat gray = inputFrame.gray();
            Mat grayInnerWindow = gray.submat(top, top + height, left, left + width);
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Sobel(grayInnerWindow, mIntermediateMat, CvType.CV_8U, 1, 1);
            Core.convertScaleAbs(mIntermediateMat, mIntermediateMat, 10, 0);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
            grayInnerWindow.release();
            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_SEPIA:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Core.transform(rgbaInnerWindow, rgbaInnerWindow, mSepiaKernel);
            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_ZOOM:
            //Mat zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
            Mat mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
            //Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size());
            Size wsize = mZoomWindow.size();
            Imgproc.rectangle(mZoomWindow, new Point(1, 1), new Point(wsize.width - 2, wsize.height - 2), new Scalar(255, 0, 0, 255), 2);
            //zoomCorner.release();
            mZoomWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_PIXELIZE:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.resize(rgbaInnerWindow, mIntermediateMat, mSize0, 0.1, 0.1, Imgproc.INTER_NEAREST);
            Imgproc.resize(mIntermediateMat, rgbaInnerWindow, rgbaInnerWindow.size(), 0., 0., Imgproc.INTER_NEAREST);
            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_POSTERIZE:
            /*
            Imgproc.cvtColor(rgbaInnerWindow, mIntermediateMat, Imgproc.COLOR_RGBA2RGB);
            Imgproc.pyrMeanShiftFiltering(mIntermediateMat, mIntermediateMat, 5, 50);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_RGB2RGBA);
            */
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
            rgbaInnerWindow.setTo(new Scalar(0, 0, 0, 255), mIntermediateMat);
            Core.convertScaleAbs(rgbaInnerWindow, mIntermediateMat, 1./16, 0);
            Core.convertScaleAbs(mIntermediateMat, rgbaInnerWindow, 16, 0);
            rgbaInnerWindow.release();
            break;
        }

        //return rgba;
        return mOutMat;
    }
}
