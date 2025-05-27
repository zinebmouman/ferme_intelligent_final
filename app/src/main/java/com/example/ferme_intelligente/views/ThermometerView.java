package com.example.ferme_intelligente.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ThermometerView extends View {
    private float temperature = 0f;
    private final Paint bulbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tubePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ThermometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bulbPaint.setColor(Color.RED);
        tubePaint.setColor(Color.GRAY);
    }

    public void setTemperature(float temp) {
        this.temperature = temp;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float height = getHeight();
        float width = getWidth();
        float tubeTop = 20f;
        float tubeBottom = height - 60f;
        float tubeWidth = width / 4;
        float tubeLeft = width / 2 - tubeWidth / 2;
        float tubeRight = width / 2 + tubeWidth / 2;
        float levelHeight = (temperature / 50f) * (tubeBottom - tubeTop);
        float fillTop = tubeBottom - levelHeight;

        // Tube
        canvas.drawRect(tubeLeft, tubeTop, tubeRight, tubeBottom, tubePaint);
        // Level
        canvas.drawRect(tubeLeft, fillTop, tubeRight, tubeBottom, bulbPaint);
        // Bulb
        canvas.drawCircle(width / 2, tubeBottom + 20f, 30f, bulbPaint);
    }
}
