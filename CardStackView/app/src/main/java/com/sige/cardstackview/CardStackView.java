package com.sige.cardstackview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuozengsi on 3/28/16.
 */
public class CardStackView extends FrameLayout {

    private final static int ACTIVE_VIEW_NUM = 4;
    private final static float SCALE_FACTOR = 0.97f;
    private final static String TAG = "CardStackView";
    private int mTouchSlop;
    private int uncoveredHeight;
    private int cardViewElevation;
    private int cardViewHeight;
    private List<View> mViewList;

    private OnTouchListener cardViewTouchListener = new OnTouchListener() {

        private int initialX;
        private int currentX;
        private ObjectAnimator animator;
        private VelocityTracker tracker = VelocityTracker.obtain();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            Log.i(TAG, v.toString() + ": touch");

//            if(animator == null){
//                animator = ObjectAnimator.ofFloat(v,"translationX",0,100);
//                animator.setDuration(500);
//                animator.start();
//                v.getX();
//            }
//
//            tracker.addMovement(event);
//            tracker.computeCurrentVelocity(1000);
//            float velocityX = tracker.getXVelocity();
//            float velocityY = tracker.getYVelocity();
//            Log.i(TAG,"velocity x: " + velocityX + "velocity y: " + velocityY);
            int action = event.getAction();
            switch (action){
                case MotionEvent.ACTION_DOWN:
                    initialX = (int)event.getRawX();
                    currentX = initialX;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int)event.getRawX();
                    if(Math.abs(x - currentX) > mTouchSlop){
                        v.setTranslationX(x-initialX);
                        Log.i(TAG, "delta X:" + (x - currentX));
                        currentX = x;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    Log.i(TAG,"Touch up");
                    if(Math.abs(event.getRawX() - initialX) > 300){
                        Log.i(TAG,"larger than mTouchSlop");
                    }
                    else {
                        v.setTranslationX(0);
                    }
                    break;
            }


            return true;
        }
    };


    public CardStackView(Context context) {
        this(context,null);
    }

    public CardStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){

        cardViewElevation = Math.round(getResources().getDimension(R.dimen.card_view_elevation));
        mViewList = new ArrayList<>();

        WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        uncoveredHeight = Math.round(heightPixels * 0.11f);
        cardViewHeight = Math.round(heightPixels * 0.5f);
        Log.i(TAG,"CardViewHeight: " + cardViewHeight);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        //mTouchSlop = 1000;

        Log.i(TAG, "slop: " + mTouchSlop);


    }

    public void setUncoveredHeight(int height){
        uncoveredHeight = height;
    }

    public void addCardView(final CardView cardView) {
        Log.i(TAG, cardView.toString());
        cardView.setOnTouchListener(cardViewTouchListener);
        addView(cardView);
        final int previousViewCount = mViewList.size();
        cardView.setCardElevation(cardViewElevation * previousViewCount);
        LayoutParams params = (LayoutParams) cardView.getLayoutParams();
        params.height = cardViewHeight;
        cardView.setLayoutParams(params);
        mViewList.add(cardView);
        Log.i(TAG, "pre Card View: " + previousViewCount);
        int firstViewMarginTop = (int) Math.round(uncoveredHeight * (1.0 - Math.pow(SCALE_FACTOR, previousViewCount)) / (1 - SCALE_FACTOR));
        int topMargin = firstViewMarginTop;
        for (int i = previousViewCount; i >= 0; i--) {

            float scale = (float) Math.pow(SCALE_FACTOR, previousViewCount - i);
            final View v = mViewList.get(i);
            Log.i(TAG, "view height: " + v.getHeight());
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (i < previousViewCount) {
                topMargin = topMargin - (int) (uncoveredHeight * scale / SCALE_FACTOR);
            }
            lp.topMargin = topMargin;
            v.setLayoutParams(lp);
            v.setScaleX(scale);
            v.setScaleY(scale);


        }

    }

    /**
     * {@inheritDoc}
     *
     * @param changed
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
}
