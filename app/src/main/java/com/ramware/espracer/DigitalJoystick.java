package com.ramware.espracer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;


public class DigitalJoystick extends View {
    private static final int CENTER = 0;
    private static final int EDGE = 1;
    private int joystickX = CENTER;
    private int joystickY = CENTER;
    private Paint paint;
    private OnJoystickMoveListener joystickMoveListener;
    private static final float deadZone = 0.3f;

    public DigitalJoystick(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    @Override
    protected  void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, paint);
        paint.setColor(Color.BLUE);
        float joystickRadius = Math.min(width, height) / 8f;
        float posX = width / 2f + joystickX * (width / 2f - joystickRadius);
        float posY = height / 2f - joystickY * (height / 2f - joystickRadius);
        canvas.drawCircle(posX, posY, joystickRadius, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) / 2f;

            float dx = (x - centerX) /radius;
            float dy = (y - centerY) / radius;

            if(Math.abs(dx) < deadZone) dx = 0;
            if(Math.abs(dy) < deadZone) dy = 0;

            if (Math.abs(dx) > Math.abs(dy)) {
                joystickX = (dx > 0 ? EDGE : (dx < 0 ? -EDGE : CENTER));
                joystickY = CENTER;
            } else {
                joystickY = (dy < 0 ? EDGE : (dy > 0 ? -EDGE : CENTER));
                joystickX = CENTER;
            }
            if(joystickMoveListener != null) {
                joystickMoveListener.onMove(joystickX, joystickY);
            }
            invalidate();
            return true;
        } else if(event.getAction() == MotionEvent.ACTION_UP) {
            joystickX = CENTER;
            joystickY = CENTER;
            if (joystickMoveListener != null) {
                joystickMoveListener.onMove(joystickX, joystickY);
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
    public void setOnJoystickMoveListener(OnJoystickMoveListener listener) {
        this.joystickMoveListener = listener;
    }

    public interface OnJoystickMoveListener {
        void onMove(int x, int y);
    }
}
