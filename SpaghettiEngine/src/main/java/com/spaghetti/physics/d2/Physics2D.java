package com.spaghetti.physics.d2;

import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.GameSettings;
import org.joml.Vector2f;

import com.spaghetti.physics.Physics;

import java.lang.reflect.InvocationTargetException;

public abstract class Physics2D extends Physics<Vector2f, Float, RigidBody2D> {

    private static Class<? extends Physics2D> physicsClass;
    private static String physicsClassName;

    private static void updateClass() {
        try {
            // Change in body class, update class
            String setting = GameSettings.sgetEngineSetting("physics.d2.physicsClass");
            if (!setting.equals(physicsClassName)) {
                physicsClassName = setting;
                physicsClass = (Class<? extends Physics2D>) Class.forName(physicsClassName);
            }
        } catch(ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Physics2D getInstance() {
        try {
            updateClass();

            // Instantiate body and return
            return physicsClass.getConstructor().newInstance();
        } catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
