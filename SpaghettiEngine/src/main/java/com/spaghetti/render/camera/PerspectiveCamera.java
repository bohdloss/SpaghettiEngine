package com.spaghetti.render.camera;

import com.spaghetti.render.Camera;

public class PerspectiveCamera extends Camera {

    public PerspectiveCamera() {
        setVisible(true);
        fov = 90;
    }

    protected void setProjectionMatrix() {
        projection.identity().perspective(fov, targetRatio, 1, -1);
        calcScale();
    }

}
