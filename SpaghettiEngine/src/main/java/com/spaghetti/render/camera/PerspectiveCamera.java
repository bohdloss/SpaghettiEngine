package com.spaghetti.render.camera;

import com.spaghetti.render.Camera;
import com.spaghetti.utils.MathUtil;

public class PerspectiveCamera extends Camera {

    public PerspectiveCamera() {
        super();
        fov = (float) Math.toRadians(90);
    }

    protected void setProjectionMatrix() {
        projection.identity().perspective(fov, targetRatio, 1, -1);
        calcScale();
    }

    public void calcScale() {
        scale = 1;
    }

}
