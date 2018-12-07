package com.example.scrollimage;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

public class PhotoView2 extends ImageView implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private Matrix matrix = new Matrix();
    private static final int NONE = 0;   // 初始化
    private static final int DRAG = 1;   // 拖拽
    private static final int ZOOM = 2;   // 缩放

    private static final int ZOOM_MINN =-1;//缩放最小值 <minscale
    private static final int ZOOM_MIN =0;// minScaleRate<=缩放最小值 <1
    private static final int ZOOM_NORMAL = 1;//正常比例 =1
    private static final int ZOOM_BIG = 2;//  >1 且小于< maxscaleRate
    private static final int ZOOM_MAX = 3;  //= maxscaleRate
    private  int zoomStatus = ZOOM_NORMAL;
    private int mode = NONE;            //初始的模式

    private boolean canTranlate;//是否可以平移
    private boolean canScale;//是否可以缩放
    private int mImageWidth;
    private int mImageHeight;
    private int mImageSrcWidht;
    private int mImageSrcHeight;
    protected Context mContext;
    private OverScroller mScroller;
    private float maxscaleRate = 3;//放大 三倍
    private float minScaleRate = 1.0f/3;//最小缩放1/3
    /**
     * 初始化缩放比例
     */
    private float initScale;
    private RectF contentF;
    private float[] mMatrixValues = new float[9];
    /**
     * 上次滑动的坐标值
     */
    private int mLastMotionY;
    private int mLastMotionX;

    /**
     * 上次双手按下的坐标
     */
    private PointF mLastMidPoint;
    private float mLastDist;
    /**
     * 如果正在拖拽则为true
     */
    private boolean mIsBeingDragged = false;
    /**
     * 用户计算当前滚动速度
     */
    private VelocityTracker mVelocityTracker;
    private GestureDetector gestureDetector;
    private int mActivePointerId = -1;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mOverscrollDistance;
    private int mScrollX,mScrollY;
    private FlingRunnable mCurrentFlingRunnable;
    private int mTouchSlop;
    public PhotoView2(Context context) {
        super(context);
        mContext = context;
        initPhotoView();
    }

    public PhotoView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initPhotoView();
    }

    public PhotoView2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initPhotoView();
    }
    private void initPhotoView(){
        mScroller = new OverScroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mTouchSlop = configuration.getScaledTouchSlop();
        setScaleType(ScaleType.MATRIX);
        gestureDetector = new GestureDetector(mContext,this);
        mLastMidPoint = new PointF();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        initImg();
    }

    private void initImg(){
        Drawable drawable = getDrawable();
        if (drawable == null){
            return;
        }
        matrix.reset();
        mImageSrcHeight = drawable.getIntrinsicHeight();
        mImageSrcWidht = drawable.getIntrinsicWidth();
        float scaleX = getWidth()  * 1.0f / mImageSrcWidht;
        initScale = scaleX;
        mImageWidth = getWidth();
        mImageHeight = (int) (mImageSrcHeight * scaleX);
        matrix.postScale(scaleX,scaleX);
        if (mImageHeight < getHeight()){
            matrix.postTranslate(0,getHeight() /2 - mImageHeight /2);
        }

        contentF = new RectF(0,0,getWidth(),getHeight());
        postInvalidate();
    }
    /**
         * 修正图片超出边界
     */
    private void fixTranslation(){
        RectF imageRectF =  new RectF(0,0,mImageSrcWidht,mImageSrcHeight);
        matrix.mapRect(imageRectF);
        float deltaX = 0, deltaY = 0;
        if (imageRectF.height() >= getHeight()){
            //长图
            if (imageRectF.top > 0){
                //到达最顶端。
                deltaY = - imageRectF.top;
            }

            if(imageRectF.bottom <= contentF.bottom){
                //到达最低端
                deltaY =contentF.bottom- imageRectF.bottom;
            }
        }else {
            //小图
            deltaY = (getHeight() - imageRectF.height()) / 2 - imageRectF.top;
        }
        if (imageRectF.left > contentF.left){
            deltaX = - imageRectF.left;
        }
        if (imageRectF.right < contentF.right){
            deltaX  = contentF.right - imageRectF.right;
        }
        matrix.postTranslate(deltaX,deltaY);
    }

    private RectF getDisplayRect(){
        fixTranslation();
        RectF imageRectF = new RectF(0,0,mImageSrcWidht,mImageSrcHeight);
        matrix.mapRect(imageRectF);
        return imageRectF;
    }
    private void fixScale(){
        zoomStatus = getZoomStatus();
        if (zoomStatus == ZOOM_MINN || zoomStatus == ZOOM_MIN || zoomStatus == ZOOM_NORMAL){
            //需要恢复到normal 状态
           post(new AnimatiedZoomRunnable());
        }
    }

    /**
     * 检测是否撑满屏幕
     */
    private boolean checkMatchScreen(){
        RectF imageRectF =  new RectF(0,0,mImageSrcWidht,mImageSrcHeight);
        matrix.mapRect(imageRectF);
        if ((imageRectF.bottom -imageRectF.top) < getHeight()){
            //图片高度没有撑满整个屏幕
            return false;
        }
        return true;
    }
    private int getZoomStatus(){
        float scale = getScale() / initScale;
        if (scale < minScaleRate){
            return ZOOM_MINN;
        }else if (scale < 1 && scale >= minScaleRate){
            return ZOOM_MIN;
        }else if (scale == 1){
            return ZOOM_NORMAL;
        }else if (scale > 1 && scale < maxscaleRate){
            return ZOOM_BIG;
        }else {
            return ZOOM_MAX;
        }
    }
    private void endDrag(){
        mIsBeingDragged = false;
    }

    private void fling(int velocityX,int velocityY){
        mCurrentFlingRunnable = new FlingRunnable(mContext,this);
        mCurrentFlingRunnable.fling(getWidth(),getHeight(),velocityX,velocityY);
        post(mCurrentFlingRunnable);
    }

    /**
     * 记录两手指中间距离
     * @param event
     * @return
     */
    private PointF midPoint(MotionEvent event) {
        float x = (event.getX(0) + event.getX(1)) / 2;
        float y = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(x, y);
    }

    /**
     * 计算两点之间距离
     * @param event
     * @return
     */
    private float distance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);//两点间距离公式
    }
    private float getScale(){
        matrix.getValues(mMatrixValues);
        return mMatrixValues[Matrix.MSCALE_X];
    }
    private float getZoomScale(float scale){
        zoomStatus = getZoomStatus();
        float calcuScale = 1;
        switch (zoomStatus){
            case ZOOM_MINN:
                if(scale < 1){
                    //缩小
                    calcuScale = 1;
                }else {
                    calcuScale = scale;
                }
                 break;
            case ZOOM_MIN:
            case ZOOM_NORMAL:
            case ZOOM_BIG:
                calcuScale = scale;
                break;
            case ZOOM_MAX:
                if (scale > 1){
                    //放大
                    calcuScale =1;
                }else {
                    calcuScale = scale;
                }
                break;
        }
        return calcuScale;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initImg();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        gestureDetector.onTouchEvent(event);
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN:{
                if (mCurrentFlingRunnable != null){
                    mCurrentFlingRunnable.cancelFling();
                }
                mActivePointerId = event.getPointerId(0);
                if (!mScroller.isFinished()){
                    mScroller.abortAnimation();
                }
                mLastMotionY = (int) event.getY();
                mLastMotionX = (int) event.getX();
                mode = DRAG;

                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN:{
                //第二根手指按下
                if (event.getPointerCount() >= 2){
                    mode = ZOOM;
                    PointF midPoint = midPoint(event);
                    mLastMidPoint.set(midPoint.x,midPoint.y);
                    mLastDist = distance(event);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                if (mode == DRAG){
                    int y = (int) event.getY();
                    int x = (int) event.getX();
                    int deltaY = y -  mLastMotionY;
                    int deltaX = x - mLastMotionX;
                    if (checkMatchScreen()){
                        matrix.postTranslate(deltaX,deltaY);
                        fixTranslation();
                        mLastMotionX = x;
                        mLastMotionY = y;
                    }
                }else if (mode == ZOOM){
                    float scale = distance(event)/mLastDist ;
                    scale = getZoomScale(scale);
                    if (zoomStatus == ZOOM_NORMAL || zoomStatus == ZOOM_MIN || zoomStatus == ZOOM_MINN){
                        matrix.postScale(scale,scale,getWidth() / 2,getHeight() /2);
                    }else {
                        matrix.postScale(scale,scale,mLastMidPoint.x,mLastMidPoint.y);
                    }
                    mLastDist = distance(event);
                }

                postInvalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:{
                //两根手指只有一根抬起来的时候
                fixScale();
                mode = NONE;
                break;
            }
            case MotionEvent.ACTION_UP:{
                if (mode == DRAG){
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocitx = (int) velocityTracker.getXVelocity(mActivePointerId);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                    if (Math.abs(initialVelocity) > mMinimumVelocity || Math.abs(initialVelocitx) > mMinimumVelocity){
                        fling(-initialVelocitx,-initialVelocity);
                    }
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                endDrag();
                break;
            }
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setImageMatrix(matrix);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!mScroller.isFinished()){
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = scrollX;
            mScrollY = scrollY;
            onScrollChanged(mScrollX,mScrollY,oldX,oldY);
            if (clampedY){
                mScroller.springBack(mScrollX,mScrollY,0,0,0,mImageHeight);
            }
        }else {
            super.scrollTo(scrollX,scrollY);
        }
        awakenScrollBars();
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        float scale = 1;
        if (getScale() < initScale * maxscaleRate){
            //放大
            scale = maxscaleRate;
            matrix.postScale(scale,scale,e.getX(),e.getY());
        }else{
            //缩小
            matrix.postScale(initScale / getScale(),initScale / getScale(),e.getX(),e.getY());
        }
        fixTranslation();
        postInvalidate();
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public class FlingRunnable implements Runnable {
        private final ScrollerProxy mScroller;
        private int mCurrentX, mCurrentY;
        private ImageView imageView;
        public FlingRunnable(Context context,ImageView imageView) {
            mScroller = ScrollerProxy.getScroller(context);
            this.imageView = imageView;
        }
        public void cancelFling() {
            mScroller.forceFinished(true);
        }
        public void fling(int viewWidth, int viewHeight, int velocityX,
                          int velocityY) {
            final RectF rect = getDisplayRect();
            if (null == rect) {
                return;
            }

            final int startX = Math.round(-rect.left);
            final int minX, maxX, minY, maxY;

            if (viewWidth < rect.width()) {
                minX = 0;
                maxX = Math.round(rect.width() - viewWidth);
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(-rect.top);
            if (viewHeight < rect.height()) {
                minY = 0;
                maxY = Math.round(rect.height() - viewHeight);
            } else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX,
                        maxX, minY, maxY, 0, 0);
            }
        }
        @Override
        public void run() {
            if (mScroller.computeScrollOffset()){
                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();
                matrix.postTranslate(mCurrentX - newX,mCurrentY -newY);
                imageView.postInvalidate();
                mCurrentX = newX;
                mCurrentY = newY;
                Compat.postOnAnimation(imageView, this);
            }
        }
    }
    private class AnimatiedZoomRunnable implements Runnable{

        @Override
        public void run() {
            matrix.postScale(1.07f,1.07f,getWidth()/2,getHeight()/2);
            postInvalidate();
            if (getScale() < initScale){
                postDelayed(this,1000 / 60);
            }
            if (getZoomStatus() >ZOOM_NORMAL){
                //修复防止放大超过正常比例，恢复
                matrix.postScale(initScale / getScale(),initScale / getScale());
                fixTranslation();
            }
        }
    }
}
