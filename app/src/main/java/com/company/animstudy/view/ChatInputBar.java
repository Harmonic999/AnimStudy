package com.company.animstudy.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import com.company.animstudy.R;
import com.company.animstudy.util.AndroidSystem;
import com.company.animstudy.util.Anim;

import static com.company.animstudy.util.Dimensions.*;

@SuppressLint("ClickableViewAccessibility")
public class ChatInputBar extends ConstraintLayout {

    private static final String TAG = "chat_input_bar";

    private static final int LAYOUT_TRANSITION_DURATION = 300;

    private static final float PLAY_BTN_WIDTH_HEIGHT_PX = px(56);
    private static final float MAX_PLAY_BTN_SCALE = 2;

    private static final int BASE_PLAY_BTN_BOT_END_MARGIN_PX = (int) px(8);
    private static final int BASE_CV_INPUT_ROOT_END_MARGIN_PX = (int) px(72);
    private static final int BASE_CV_LOCK_ROOT_BOT_MARGIN_PX = (int) px(100);

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
    private CardView cvLockRoot;

    public ChatInputBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.chat_input_bar, this);
        this.context = context;

        clRoot = findViewById(R.id.cl_root);

        fabSend = findViewById(R.id.fab_send);
        fabSend.setOnTouchListener(new DragListener());

        cvInputRoot = findViewById(R.id.cv_input_root);
        cvLockRoot = findViewById(R.id.cv_lock_root);
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
                        Anim.scaleView(fabSend, 1f, 1f);
                        enableTemporalViewTransitions();
                    }

                    resetViewsState();
                    break;

                case MotionEvent.ACTION_DOWN:

                    AndroidSystem.vibrate(context);
                    Anim.scaleView(fabSend, MAX_PLAY_BTN_SCALE, MAX_PLAY_BTN_SCALE);
                    showLock(true);

                    if (!isUserTouchDisabled) {
                        currentX = fabSend.getX() - event.getRawX();
                        currentY = fabSend.getY() - event.getRawY();

                        if (baseX == 0 && baseY == 0) {
                            baseX = fabSend.getX();
                            baseY = fabSend.getY();
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
                        float maxY = screenHeight - fabSend.getHeight() * 2.5f;
                        destinationY = destinationY < maxY ? maxY : destinationY;

                        boolean isMoveX = velocityX > velocityY
                                && destinationX <= baseX && fabSend.getY() == baseY;
                        boolean isMoveY = velocityY > velocityX
                                && destinationY <= baseY && fabSend.getX() == baseX;

                        if (isMoveX) {

                            ConstraintLayout.LayoutParams fabSendLayoutParams =
                                    (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
                            float fabSendNewMarginEnd = screenWidth - destinationX - fabSend.getWidth();
                            fabSendLayoutParams.setMarginEnd((int) fabSendNewMarginEnd);
                            fabSend.setLayoutParams(fabSendLayoutParams);

                            ConstraintLayout.LayoutParams cvInputRootLayoutParams =
                                    (ConstraintLayout.LayoutParams) cvInputRoot.getLayoutParams();
                            cvInputRootLayoutParams.setMarginEnd(
                                    (int) fabSendNewMarginEnd - BASE_PLAY_BTN_BOT_END_MARGIN_PX
                                            + BASE_CV_INPUT_ROOT_END_MARGIN_PX);

                            cvInputRoot.setLayoutParams(cvInputRootLayoutParams);

                            if (destinationX == maxX) {
                                resetTouch();
                            }

                        } else if (isMoveY) {

                            ConstraintLayout.LayoutParams fabSendLayoutParams =
                                    (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
                            float fabSendNewMarginBot = screenHeight - destinationY - fabSend.getHeight();
                            fabSendLayoutParams.bottomMargin = (int) fabSendNewMarginBot;
                            fabSend.setLayoutParams(fabSendLayoutParams);

                            ConstraintLayout.LayoutParams cvLockRootLayoutParams =
                                    (LayoutParams) cvLockRoot.getLayoutParams();
                            cvLockRootLayoutParams.bottomMargin =
                                    (int) (fabSendNewMarginBot - BASE_PLAY_BTN_BOT_END_MARGIN_PX
                                            + BASE_CV_LOCK_ROOT_BOT_MARGIN_PX);

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
        Anim.scaleView(fabSend, 1f, 1f);
        isUserTouchDisabled = true;
    }

    private void enableLayoutTransition(boolean enable) {
        if (enable) {
            Anim.setLayoutTransition(clRoot, LAYOUT_TRANSITION_DURATION);
        } else {
            clRoot.setLayoutTransition(null);
        }
    }

    private void resetViewsState() {
        ConstraintLayout.LayoutParams fabSendLayoutParams =
                (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
        fabSendLayoutParams.setMarginEnd(BASE_PLAY_BTN_BOT_END_MARGIN_PX);
        fabSendLayoutParams.bottomMargin = BASE_PLAY_BTN_BOT_END_MARGIN_PX;
        fabSend.setLayoutParams(fabSendLayoutParams);

        ConstraintLayout.LayoutParams cvInputRootLayoutParams =
                (ConstraintLayout.LayoutParams) cvInputRoot.getLayoutParams();
        cvInputRootLayoutParams.setMarginEnd(BASE_CV_INPUT_ROOT_END_MARGIN_PX);
        cvInputRoot.setLayoutParams(cvInputRootLayoutParams);

        ConstraintLayout.LayoutParams cvLockRootLayoutParams =
                (LayoutParams) cvLockRoot.getLayoutParams();
        cvLockRootLayoutParams.bottomMargin = BASE_CV_LOCK_ROOT_BOT_MARGIN_PX;
        cvLockRoot.setLayoutParams(cvLockRootLayoutParams);
    }

    private void enableTemporalViewTransitions() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                enableLayoutTransition(false);
            }
        }, LAYOUT_TRANSITION_DURATION + 50); // + some delay

        enableLayoutTransition(true);
    }

    private void showLock(boolean show) {
        cvLockRoot.setVisibility(show ? VISIBLE : GONE);
    }

}
