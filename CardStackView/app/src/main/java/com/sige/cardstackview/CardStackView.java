package com.sige.cardstackview;

import android.animation.Animator;
import android.animation.AnimatorSet;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by zhuozengsi on 3/28/16.
 */
public class CardStackView extends FrameLayout {

    private final static String TAG = "CardStackView";

    /**The quotient of the size of adjacent views, the smaller one divides the bigger */
    private final static float PERSPECTIVE_FACTOR = 0.95f;

    /**The duration time when the top view removes*/
    private final static int FADE_DURATION = 200;

    /**The threshold of width that will trigger the animation of removing the top view,
     * is the the quotient for the moved distance and the CardView width.*/
    private final static float FADE_SLOP_RATIO = 0.2f;

    /**The value is the quotient for the uncovered height of the adjacent views and the CardView height*/
    private final static float UNCOVERED_RATIO = 0.11f;

    /**The CardView width and height divides by the screen width and height respectively.*/
    private final static float CARD_WIDTH_RATIO = 0.8f;
    private final static float CARD_HEIGHT_RATIO = 0.5f;

    // The top CardView.
    private CardView mTopCardView;

    //The count of CardView in the screen.
    private int activeCardNum;

    //The threshold which we can detect the moving action.
    private int mTouchSlop;

    //The threshold of width that will trigger the animation of removing the top view
    private int mFadeSlop;

    //The uncovered height for the adjacent views.
    private int uncoveredHeight;

    //The elevation offset of the CardView, and the elevation of the most bottom one is 0.
    private int cardViewElevation;

    //The size of the screen and the CardView.
    private int mScreenWidth;
    private int cardViewHeight;
    private int mCardViewWidth;

    //The LinkedList that stores the reference of all the CardViews.
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
        activeCardNum = 3;

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
            layoutAgain(0,previousViewCount, false);

        }else {
            //cardView.setCardElevation(cardViewElevation * activeCardNum);
            removeViewAt(0);
            mViewList.add(cardView);
            layoutAgain(previousViewCount-activeCardNum+1, previousViewCount,false);
        }

    }

    private void layoutAgain(int indexMin, int indexMax, boolean isAnimated) {

        int firstViewMarginTop = (int) Math.round(uncoveredHeight *
                (1.0 - Math.pow(PERSPECTIVE_FACTOR, (indexMax - indexMin))) / (1 - PERSPECTIVE_FACTOR));

        int translationY = firstViewMarginTop;

        if (isAnimated) {
            int deltaY = uncoveredHeight;
            long time1 = System.currentTimeMillis();
            List<AnimatorSet> animList = new LinkedList<>();
            for(int i = indexMax; i> indexMin; i--){
                CardView cardView = mViewList.get(i);
                //cardView.setTranslationX(0.0f);
                //Log.i(TAG,"translation X:" + cardView.getTranslationX() + ", translation Y: " + cardView.getTranslationY());
                //if(cardView.getTranslationX() != 0.0f)
                //    cardView.setTranslationX(0.0f);
                //Log.i(TAG,"translation X:" + cardView.getTranslationX() + ", translation Y: " + cardView.getTranslationY());
                AnimatorSet set = new AnimatorSet();
                float tranY = cardView.getTranslationY();
                float scale = cardView.getScaleX();
                float elevation = cardView.getCardElevation();
                set.playTogether(
                        ObjectAnimator.ofFloat(cardView, "translationY", tranY, tranY + deltaY),
                        ObjectAnimator.ofFloat(cardView, "scaleX", scale, scale / PERSPECTIVE_FACTOR),
                        ObjectAnimator.ofFloat(cardView, "scaleY", scale, scale / PERSPECTIVE_FACTOR),
                        ObjectAnimator.ofFloat(cardView, "cardElevation", elevation, elevation + cardViewElevation));
                set.setDuration(250);
                animList.add(set);
                deltaY *= PERSPECTIVE_FACTOR;
            }

            CardView cardView = mViewList.get(indexMin);
            cardView.setCardElevation(0.0f);
            cardView.setTranslationY(0.0f);
            float scale = deltaY*1.0f / uncoveredHeight;
            cardView.setScaleX(scale);
            cardView.setScaleY(scale);
//            AnimatorSet animatorSet = new AnimatorSet();
//            animatorSet.playTogether(
//                    ObjectAnimator.ofFloat(cardView, "translationX", cardView.getTranslationX(), 0),
//                    ObjectAnimator.ofFloat(cardView, "alpha", 0.0f, 1.0f));
//            animatorSet.setDuration(250);
//            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
//            animatorSet.start();
            for(Animator animator: animList){
                animator.start();
            }
            long time2 = System.currentTimeMillis();
            Log.i(TAG, "Last Time: " + (time2 - time1) + "ms");



        } else{
            for (int i = indexMax; i >= indexMin; i--) {
                float scale = (float) Math.pow(PERSPECTIVE_FACTOR, indexMax - i);
                final CardView v = mViewList.get(i);
                v.setCardElevation((i - indexMin) * cardViewElevation);
                if (i < indexMax) {
                    translationY = translationY - (int) (uncoveredHeight * scale / PERSPECTIVE_FACTOR);
                }
                v.setTranslationY(translationY);
                v.setScaleX(scale);
                v.setScaleY(scale);
            }
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

                removeView(mTopCardView);
                //After remove the top one from our view,let it return to the initial X quickly.
                //Also you can use the method @see View.setTranslationX, but not all the
                //time it will work.So we choose the animator whose duration is very short.
                ObjectAnimator.ofFloat(mTopCardView,"translationX", mTopCardView.getTranslationX(),0)
                        .setDuration(10).start();
                mViewList.removeLast();
                mViewList.addFirst(mTopCardView);
                final int viewNum = mViewList.size();
                if (viewNum <= activeCardNum) {
                    layoutAgain(0, viewNum - 1, true);
                    mTopCardView.setTranslationX(0);
                    addView(mTopCardView, 0);
                } else {
                    layoutAgain(viewNum - activeCardNum, viewNum - 1,true);
                    addView(mViewList.get(viewNum - activeCardNum),0);
                }
                mTopCardView = mViewList.getLast();
            }
        }, FADE_DURATION);
    }

    public void showNext(){
        showNext(0, (mCardViewWidth+mScreenWidth)/2);
    }



}
