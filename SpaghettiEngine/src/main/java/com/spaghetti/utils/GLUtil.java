package com.spaghetti.utils;

import com.spaghetti.core.Game;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;

public class GLUtil {

    private static HashMap<Game, Double> versions = new HashMap<>();

    public static double discoverVersion() {
        try {
            String raw = GL11.glGetString(GL11.GL_VERSION);
            String ver = raw.split(" ")[0];
            String[] split = ver.split("\\.");
            String major = split[0];
            String minor = split[1];

            double majorInt = Integer.parseInt(major);
            double minorInt = Integer.parseInt(minor);


            double glVersion = majorInt + (minorInt * 0.1);
            versions.put(Game.getInstance(), glVersion);
            Logger.info("Discovered OpenGL version " + major + "." + minor + " (" + glVersion + " float)");
            return glVersion;
        } catch(Throwable e) {
            double glVersion = 1.1f;
            versions.put(Game.getInstance(), glVersion);
            Logger.info("Couldn't determine OpenGL version, defaulting to 1.1 (" + glVersion + " float)");
            return glVersion;
        }
    }

    public static boolean isAtleast(double version) {
        Double glVersion = versions.get(Game.getInstance());
        return MathUtil.equalOrHigher(glVersion.floatValue(), (float) version, 0.03f);
    }

    public static void forgetVersion() {
        versions.remove(Game.getInstance());
    }

}
