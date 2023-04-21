package com.spaghetti.physics.d2;

import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.GameSettings;
import org.joml.Vector2f;

import com.spaghetti.physics.Physics;

import java.lang.reflect.InvocationTargetException;

public abstract class Physics2D extends Physics<Vector2f, Float, RigidBody2D> {

    public static Physics2D getInstance() {
        try {
            Class<? extends Physics2D> physicsClass = GameSettings.sgetEngineSetting("physics.d2.physicsClass");
            return physicsClass.getConstructor().newInstance();
        } catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
