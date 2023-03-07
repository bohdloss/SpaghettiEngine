package com.spaghetti.demo;

import com.spaghetti.render.Mesh;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

import java.util.Random;

public class MovingMesh extends Mesh {

    public MovingMesh(Model model, Material material) {
        super(model, material);
    }

    public MovingMesh() {
        super();
    }

    float random = new Random().nextFloat() * 7;

    float i = 0;

    @Override
    public void commonUpdate(float delta) {
        i += 10 * getGame().getTickMultiplier(delta);

        float mod = (float) Math.sin(i);
        setRelativePosition((float) Math.cos(random) * mod, (float) Math.sin(random) * mod, 0);
    }

}