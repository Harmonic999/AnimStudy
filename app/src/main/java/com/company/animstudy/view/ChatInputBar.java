package com.company.animstudy.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

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
    private static final int BASE_CV_LOCK_ROOT_HEIGHT_PX = (int) px(48);

    private enum State {
        MICROPHONE_READY,
        MICROPHONE_RECORDING_UNLOCKED,
        MICROPHONE_RECORDING_LOCKED,
        WAITING_FOR_AUDIO_SEND_CONFIRM
    }

    private Context context;

    private float baseX;
    private float baseY;

    private float maxX;

    private float currentX;
    private float currentY;

    private float screenWidth;
    private float screenHeight;

    private boolean isUserTouchDisabled;

    private State state;

    private ConstraintLayout clRoot;
    private FloatingActionButton fabSend;
    private CardView cvInputRoot;
    private CardView cvLockRoot;
    private ImageView ivLock;

    public ChatInputBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.chat_input_bar, this);
        this.context = context;

        state = State.MICROPHONE_READY;

        clRoot = findViewById(R.id.cl_root);

        fabSend = findViewById(R.id.fab_send);
        fabSend.setOnTouchListener(new OnDragListener());

        cvInputRoot = findViewById(R.id.cv_input_root);
        cvLockRoot = findViewById(R.id.cv_lock_root);

        ivLock = findViewById(R.id.iv_lock);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        screenWidth = getMeasuredWidth();
        screenHeight = getMeasuredHeight();

        maxX = screenWidth - PLAY_BTN_WIDTH_HEIGHT_PX * 2.5f;
    }

    private class OnDragListener implements OnTouchListener {

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

                    resetSendBtnState();
                    resetInputState();
                    resetLockRootState();

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

                            float onePercent = (baseY - maxY) / 100;
                            float currentDistance = destinationY - maxY;
                            float pathPercentLeft = currentDistance / onePercent;

                            float scaleFactor = pathPercentLeft / 100 * MAX_PLAY_BTN_SCALE;
                            fabSend.setScaleX(scaleFactor);
                            fabSend.setScaleY(scaleFactor);

                            ConstraintLayout.LayoutParams cvLockRootLayoutParams =
                                    (LayoutParams) cvLockRoot.getLayoutParams();
                            cvLockRootLayoutParams.bottomMargin =
                                    (int) (fabSendNewMarginBot - BASE_PLAY_BTN_BOT_END_MARGIN_PX
                                            + BASE_CV_LOCK_ROOT_BOT_MARGIN_PX);

                            int halfHeight = BASE_CV_LOCK_ROOT_HEIGHT_PX / 2;
                            cvLockRootLayoutParams.height = (int) (halfHeight + (halfHeight * pathPercentLeft / 100));

                            if (destinationY == maxY) {
                                lockRecording();
                                resetTouch();
                                switchSendFabToAudioState();
                            }
                        }
                    }

                    break;
            }

            return true;
        }
    }

    private void switchSendFabToAudioState() {
        isUserTouchDisabled = false;
        fabSend.setOnTouchListener(null);
        fabSend.setImageResource(R.drawable.ic_send_black_24dp);
        fabSend.setOnClickListener(new OnAudioSendListener());
    }

    private void switchSendFabToMicrophoneState() {
        state = State.MICROPHONE_READY;
        fabSend.setOnClickListener(null);
        fabSend.setOnTouchListener(new OnDragListener());
        fabSend.setImageResource(R.drawable.ic_mic_black_24dp);
    }

    private class OnAudioSendListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            showLock(false);
            stopRecording();
            switchSendFabToMicrophoneState();
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

    private void resetSendBtnState() {
        ConstraintLayout.LayoutParams fabSendLayoutParams =
                (ConstraintLayout.LayoutParams) fabSend.getLayoutParams();
        fabSendLayoutParams.setMarginEnd(BASE_PLAY_BTN_BOT_END_MARGIN_PX);
        fabSendLayoutParams.bottomMargin = BASE_PLAY_BTN_BOT_END_MARGIN_PX;
        fabSend.setLayoutParams(fabSendLayoutParams);
    }

    private void resetInputState() {
        ConstraintLayout.LayoutParams cvInputRootLayoutParams =
                (ConstraintLayout.LayoutParams) cvInputRoot.getLayoutParams();
        cvInputRootLayoutParams.setMarginEnd(BASE_CV_INPUT_ROOT_END_MARGIN_PX);
        cvInputRoot.setLayoutParams(cvInputRootLayoutParams);
    }

    private void resetLockRootState() {
        ConstraintLayout.LayoutParams cvLockRootLayoutParams =
                (LayoutParams) cvLockRoot.getLayoutParams();
        cvLockRootLayoutParams.bottomMargin = BASE_CV_LOCK_ROOT_BOT_MARGIN_PX;
        if (state != State.MICROPHONE_RECORDING_LOCKED) {
            cvLockRootLayoutParams.height = BASE_CV_LOCK_ROOT_HEIGHT_PX;
            showLock(false);
        }
        cvLockRoot.setLayoutParams(cvLockRootLayoutParams);
    }

    private void enableTemporalViewTransitions() {
        new Handler().postDelayed(() -> enableLayoutTransition(false),
                LAYOUT_TRANSITION_DURATION + 50); // + some delay

        enableLayoutTransition(true);
    }

    private void showLock(boolean show) {
        cvLockRoot.setVisibility(show ? VISIBLE : GONE);
    }

    private void lockRecording() {
        state = State.MICROPHONE_RECORDING_LOCKED;
        ivLock.setImageResource(R.drawable.ic_stop_black_24dp);
        ivLock.setColorFilter(ContextCompat.getColor(context, R.color.red));
        ivLock.setOnClickListener(v -> stopRecording());
    }

    private void stopRecording() {
        state = State.WAITING_FOR_AUDIO_SEND_CONFIRM;
        ivLock.setOnClickListener(null);
        ivLock.setImageResource(R.drawable.ic_lock_outline_black_24dp);
        ivLock.setColorFilter(ContextCompat.getColor(context, R.color.grey));
        resetLockRootState();
    }
}
