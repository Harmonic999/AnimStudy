package com.company.animstudy.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.company.animstudy.R;
import com.company.animstudy.util.AndroidSystem;
import com.company.animstudy.util.Animation;

import static com.company.animstudy.util.Dimensions.*;

@SuppressLint("ClickableViewAccessibility")
public class ChatInputBar extends ConstraintLayout {

    private static final String TAG = "ch_chat_input_bar";

    private static final int LAYOUT_TRANSITION_DURATION = 300;

    private static final float PLAY_BTN_WIDTH_HEIGHT_PX = px(56);
    private static final float MAX_PLAY_BTN_SCALE = 2;

    private static final int BASE_PLAY_BTN_MARGIN_PX = (int) px(8);
    private static final int BASE_CV_INPUT_ROOT_PX = (int) px(72);

    private Context context;

    private float baseX;
    private float baseY;

    private float maxX;

    private float currentX;
    private float currentY;

    private float screenWidth;
    private float screenHeight;

    private boolean isUserTouchDisabled;

    private ConstraintLayout clRoot;
    private FloatingActionButton fabSend;
    private CardView cvInputRoot;

    public ChatInputBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.chat_input_bar, this);
        this.context = context;

        clRoot = findViewById(R.id.cl_root);

        fabSend = findViewById(R.id.fab_send);
        fabSend.setOnTouchListener(new DragListener());

        cvInputRoot = findViewById(R.id.cv_input_root);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        screenWidth = getMeasuredWidth();
        screenHeight = getMeasuredHeight();

        maxX = screenWidth - PLAY_BTN_WIDTH_HEIGHT_PX * 2.5f;
    }

    private class DragListener implements OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_UP:
                    if (isUserTouchDisabled) {
                        isUserTouchDisabled = false;
                    } else {
                        Animation.scaleView(fabSend, 1f, 1f);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                enableLayoutTransition(false);
                            }
                        }, LAYOUT_TRANSITION_DURATION + 25); // + some delay

                        enableLayoutTransition(true);
                    }

                    ConstraintLayout.LayoutParams fabSendLayoutParams =
                            (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
                    fabSendLayoutParams.setMarginEnd(BASE_PLAY_BTN_MARGIN_PX);
                    fabSendLayoutParams.bottomMargin = BASE_PLAY_BTN_MARGIN_PX;
                    fabSend.setLayoutParams(fabSendLayoutParams);

                    ConstraintLayout.LayoutParams cvInputRootLayoutParams =
                            (ConstraintLayout.LayoutParams) cvInputRoot.getLayoutParams();
                    cvInputRootLayoutParams.setMarginEnd(BASE_CV_INPUT_ROOT_PX);
                    cvInputRoot.setLayoutParams(cvInputRootLayoutParams);

                    break;

                case MotionEvent.ACTION_DOWN:

                    AndroidSystem.vibrate(context);
                    Animation.scaleView(fabSend, MAX_PLAY_BTN_SCALE, MAX_PLAY_BTN_SCALE);

                    if (!isUserTouchDisabled) {
                        currentX = view.getX() - event.getRawX();
                        currentY = view.getY() - event.getRawY();

                        if (baseX == 0 && baseY == 0) {
                            baseX = view.getX();
                            baseY = view.getY();
                        }
                    }

                    break;

                case MotionEvent.ACTION_MOVE:

                    if (!isUserTouchDisabled) {
                        VelocityTracker velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000);
                        float velocityX = Math.abs(velocityTracker.getXVelocity());
                        float velocityY = Math.abs(velocityTracker.getYVelocity());
                        velocityTracker.recycle();

                        float destinationX = event.getRawX() + currentX;
                        destinationX = destinationX > baseX ? baseX : destinationX;
                        destinationX = destinationX < maxX ? maxX : destinationX;

                        float destinationY = event.getRawY() + currentY;
                        destinationY = destinationY > baseY ? baseY : destinationY;
                        float maxY = fabSend.getHeight() / 2;
                        destinationY = destinationY < maxY ? maxY : destinationY;

                        boolean isMoveX = velocityX > velocityY
                                && destinationX <= baseX && view.getY() == baseY;
                        boolean isMoveY = velocityY > velocityX
                                && destinationY <= baseY && view.getX() == baseX;

                        if (view.getX() != baseX && view.getY() != baseY) {
                            Log.i(TAG, "x=" + view.getX() + ", y=" + view.getY());
                        }

                        if (isMoveX) {

                            fabSendLayoutParams = (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
                            float fabSendNewMargin = screenWidth - destinationX - view.getWidth();
                            fabSendLayoutParams.setMarginEnd((int) fabSendNewMargin);
                            view.setLayoutParams(fabSendLayoutParams);

                            cvInputRootLayoutParams = (ConstraintLayout.LayoutParams) cvInputRoot.getLayoutParams();
                            cvInputRootLayoutParams.setMarginEnd(
                                    (int) fabSendNewMargin - BASE_PLAY_BTN_MARGIN_PX + BASE_CV_INPUT_ROOT_PX
                            );

                            cvInputRoot.setLayoutParams(cvInputRootLayoutParams);

                            if (destinationX == maxX) {
                                resetTouch();
                            }

                        } else if (isMoveY) {

                            fabSendLayoutParams = (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
                            float desiredMarginPX = screenHeight - destinationY - view.getHeight();
                            fabSendLayoutParams.bottomMargin = (int) desiredMarginPX;
                            view.setLayoutParams(fabSendLayoutParams);

                            float onePercent = (baseY - maxY) / 100;
                            float currentDistance = destinationY - maxY;
                            float pathPercentLeft = currentDistance / onePercent;

                            float scaleFactor = pathPercentLeft / 100 * MAX_PLAY_BTN_SCALE;
                            fabSend.setScaleX(scaleFactor);
                            fabSend.setScaleY(scaleFactor);

                            if (destinationY == maxY) {
                                resetTouch();
                            }
                        }
                    }

                    break;
            }

            return true;
        }
    }

    private void resetTouch() {
        MotionEvent e = MotionEvent.obtain(
                0,
                0,
                MotionEvent.ACTION_UP,
                0,
                0,
                0);

        fabSend.dispatchTouchEvent(e);
        Animation.scaleView(fabSend, 1f, 1f);
        isUserTouchDisabled = true;
    }

    private void enableLayoutTransition(boolean enable) {
        if (enable) {
            Animation.setLayoutTransition(clRoot, LAYOUT_TRANSITION_DURATION);
        } else {
            clRoot.setLayoutTransition(null);
        }
    }

}
