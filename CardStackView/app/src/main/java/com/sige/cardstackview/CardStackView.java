package com.sige.cardstackview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by zhuozengsi on 3/28/16.
 */
public class CardStackView extends FrameLayout {

    private final static float PERSPECTIVE_FACTOR = 0.97f;
    private final static int FADE_DURATION = 200;
    private final static String TAG = "CardStackView";
    private final static float FADE_SLOP_RATIO = 0.2f;
    private final static float UNCOVERED_RATIO = 0.11f;
    private final static float CARD_WIDTH_RATIO = 0.8f;
    private final static float CARD_HEIGHT_RATIO = 0.5f;
    private CardView mTopCardView;
    private int activeCardNum;
    private int mTouchSlop;
    private int mFadeSlop;
    private int uncoveredHeight;
    private int cardViewElevation;
    private int mScreenWidth;
    private int cardViewHeight;
    private int mCardViewWidth;
    private LinkedList<CardView> mViewList;

    private OnTouchListener cardViewTouchListener = new OnTouchListener() {

        private int initialX;
        private int currentX;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(v == mTopCardView) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = (int) event.getRawX();
                        currentX = initialX;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int x = (int) event.getRawX();
                        if (Math.abs(x - currentX) > mTouchSlop) {
                            v.setTranslationX(x - initialX);
                            currentX = x;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "Touch up");
                        x = (int) event.getRawX();
                        if (x - initialX > mFadeSlop) {
                            showNext(v.getX(), (mScreenWidth + mCardViewWidth / 2));
                        } else if ((initialX - x) > mFadeSlop) {
                            showNext(v.getX(), -(mScreenWidth + mCardViewWidth / 2));
                        } else {
                            v.setTranslationX(0);
                        }
                        break;
                }
                return true;
            }
            return false;
        }
    };


    public CardStackView(Context context) {
        this(context, null);
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
        mViewList = new LinkedList<>();

        WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        uncoveredHeight = Math.round(heightPixels * UNCOVERED_RATIO);
        cardViewHeight = Math.round(heightPixels * CARD_HEIGHT_RATIO);
        mCardViewWidth = Math.round(mScreenWidth * CARD_WIDTH_RATIO);
        mFadeSlop = Math.round(mCardViewWidth * FADE_SLOP_RATIO);
        Log.i(TAG,"CardViewHeight: " + cardViewHeight);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        activeCardNum = 4;

    }

    public void setActiveCardNum(int num){
        activeCardNum = activeCardNum >= num ? num : activeCardNum;
    }

    public void setUncoveredHeight(int height){
        uncoveredHeight = height;
    }

    public void addCardView(final CardView cardView) {
        Log.i(TAG, cardView.toString());
        mTopCardView = cardView;
        cardView.setOnTouchListener(cardViewTouchListener);
        final int previousViewCount = mViewList.size();
        TextView textView = (TextView)cardView.findViewById(R.id.cardView_title);
        textView.setText("CardView: " + previousViewCount);
        addView(cardView);
        Log.i(TAG,"view index: " + indexOfChild(cardView));
        LayoutParams params = (LayoutParams) cardView.getLayoutParams();
        params.height = cardViewHeight;
        params.width = mCardViewWidth;
        params.gravity = Gravity.CENTER_HORIZONTAL;
        cardView.setLayoutParams(params);

        if(previousViewCount < activeCardNum) {
            //cardView.setCardElevation(cardViewElevation * previousViewCount);
            mViewList.add(cardView);
            Log.i(TAG, "pre Card View: " + previousViewCount);
            layoutAgain(0,previousViewCount);

        }else {
            //cardView.setCardElevation(cardViewElevation * activeCardNum);
            removeViewAt(0);
            mViewList.add(cardView);
            layoutAgain(previousViewCount-activeCardNum+1, previousViewCount);
        }

    }

    private void layoutAgain(int indexMin, int indexMax){

        int firstViewMarginTop = (int) Math.round(uncoveredHeight *
                (1.0 - Math.pow(PERSPECTIVE_FACTOR, (indexMax-indexMin))) / (1 - PERSPECTIVE_FACTOR));

        int topMargin = firstViewMarginTop;
        for (int i = indexMax; i >= indexMin; i--) {
            float scale = (float) Math.pow(PERSPECTIVE_FACTOR, indexMax - i);
            final CardView v = mViewList.get(i);
            v.setCardElevation((i-indexMin)*cardViewElevation);
            LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (i < indexMax) {
                topMargin = topMargin - (int) (uncoveredHeight * scale / PERSPECTIVE_FACTOR);
            }
            lp.topMargin = topMargin;
            v.setLayoutParams(lp);
            v.setScaleX(scale);
            v.setScaleY(scale);

            /**<p>
             * When the top cardView have been removed from the parent <code>CardStackView</code>,
             * it will be added to the first one of the <code>mViewList</code>, so it can form a
             * loop.
             * </p>
             * <p>
             * But we use the <code>ObjectAnimator</code>to translate the top cardView, it will
             * change the property of translationX, so we must reverse it to 0. Otherwise, though
             * we add the removed view to the <code>CardStackView</code>, it will reappear in the
             * position where it was removed.
             * </p>
             * <p>
             * Though not all the view in the <code>mViewList</code> need invoke this method, such as
             * the the views not removed, but some must. So we do it.
             * And we can't write it in the method {@see CardStackView#showNext(float from, float to)},
             * or it will meet some problem.
             * </p>
             * */
            v.setTranslationX(0.0f);

        }


    }

    public void addCard(CardView cardView){

    }

    private void showNext(float from, float to){

        final ObjectAnimator animator = ObjectAnimator.ofFloat(mTopCardView,"translationX",from,to);
        animator.setDuration(FADE_DURATION);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CardView temp = mTopCardView;
                removeView(mTopCardView);
                mViewList.removeLast();
                mViewList.addFirst(temp);
                mTopCardView = mViewList.getLast();
                final int viewNum = mViewList.size();
                if (viewNum <= activeCardNum) {
                    layoutAgain(0, viewNum - 1);
                    addView(temp);
                } else {
                    layoutAgain(viewNum - activeCardNum, viewNum - 1);
                    addView(mViewList.get(viewNum - activeCardNum));
                }
            }
        }, FADE_DURATION);
    }

    public void showNext(){
        showNext(0, (mCardViewWidth+mScreenWidth)/2);
    }



}
