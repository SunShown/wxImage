package com.example.scrollimage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TimingLogger;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.OverScroller;

public class PhotoView extends ImageView {
    private Matrix matrix = new Matrix();
    private int mImageWidth;
    private int mImageHeight;
    protected Context mContext;
    private OverScroller mScroller;
    private int showContentHeight;
    private int screenHeight;
    /**
     * 上次滑动的坐标值
     */
    private int mLastMotionY;
    /**
     * 如果正在拖拽则为true
     */
    private boolean mIsBeingDragged = false;
    /**
     * 用户计算当前滚动速度
     */
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    public PhotoView(Context context) {
        super(context);
        this.mContext = context;
        initPhotoView();
    }

    public PhotoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initPhotoView();
    }

    public PhotoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initPhotoView();
    }

    private void initPhotoView(){
        mScroller = new OverScroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();

        setScaleType(ScaleType.MATRIX);
    }

    private void initImg(){
        Drawable drawable = getDrawable();
        if (drawable == null){
            return;
        }
        int[] location = new int[2];
        getLocationOnScreen(location);
        setScreen();
        showContentHeight = screenHeight - location[1];
        mImageHeight = drawable.getIntrinsicHeight();
        mImageWidth = drawable.getIntrinsicWidth();
        RectF dstF = new RectF(0,0,getWidth(),getHeight());
        RectF srcF = new RectF(0,0,mImageWidth,mImageHeight);
        if (mImageHeight > getHeight()){

            matrix.setRectToRect(srcF,dstF, Matrix.ScaleToFit.FILL);
        }else {
            matrix.setRectToRect(srcF,dstF, Matrix.ScaleToFit.CENTER);
        }
        setImageMatrix(matrix);
    }

    /**
     * 修正图片超出边界
     */
    private void fixTranslation(){
        RectF imageRectF =  new RectF(0,0,mImageWidth,mImageHeight);
        matrix.mapRect(imageRectF);
        float deltaX = 0, deltaY = 0;
        if (imageRectF.top > 0){
            //到达最顶端。
            deltaY = - imageRectF.top;
        }
        if(imageRectF.bottom <= showContentHeight){
            //到达最低端
            deltaY = showContentHeight - imageRectF.bottom;
        }
        matrix.postTranslate(deltaX,deltaY);
    }

    private void endDrag(){
        mIsBeingDragged = false;
    }

    private void setScreen(){
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        screenHeight = metrics.heightPixels;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initImg();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:{
                mLastMotionY = (int) event.getY();
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                int y = (int) event.getY();
                int yDiff = Math.abs( y- mLastMotionY);
                int deltaY = mLastMotionY - y;
                if ( yDiff > mTouchSlop){
                    mIsBeingDragged = true;
                    matrix.postTranslate(0,-deltaY);
                    fixTranslation();
                    setImageMatrix(matrix);
                }
                mLastMotionY = y;
                break;
            }
            case MotionEvent.ACTION_UP:{

                endDrag();
                break;
            }
        }
        return true;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();
        if (drawable != null){
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) Math.ceil(width * drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth());
            height = Math.max(height,MeasureSpec.getSize(heightMeasureSpec));
            setMeasuredDimension(width,height);
        }else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
