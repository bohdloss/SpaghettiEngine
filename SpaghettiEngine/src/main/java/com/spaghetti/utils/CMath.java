package com.spaghetti.utils;

import java.util.Random;

import org.joml.Vector2f;
import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameWindow;
import com.spaghetti.objects.Camera;

public final class CMath {

	private CMath() {
	}

	// Map a value (value) that is valid in a specified range (min1, max1)
	// to the equivalent in the the given new range (min2, max2)
	public static float remap(float value, float min1, float max1, float min2, float max2) {
		return lerp(reverseLerp(value, min1, max1), min2, max2);
	}

	// Linear interpolate in a range (a, b) using a percentage value (alpha)
	public static float lerp(float alpha, float a, float b) {
		if (alpha >= 1) {
			return b;
		}
		if (alpha <= 0) {
			return a;
		}
		return a + ((b - a) * alpha);
	}

	// Inverse function of lerp. Given the value (lerp) and the range (a, b) finds
	// the alpha value
	public static float reverseLerp(float lerp, float a, float b) {
		int range = inRangeIgnoreSign(lerp, a, b);
		if (range != 0) {
			return range == -1 ? 0 : 1;
		}
		return (lerp - a) / (b - a);
	}

	// Checks if a value (val) is in the specified range (a, b) regardless if a is
	// bigger than b
	public static int inRangeIgnoreSign(float val, float a, float b) {
		if (a <= b) {
			return val < a ? -1 : (val > b ? 1 : 0);
		} else {
			return val > a ? -1 : (val < b ? 1 : 0);
		}
	}

	// Just like floor function (in), except
	// you get to define the size of interval (step)
	public static float approx(float step, float in) {
		float toMult = 1 / step;
		float doMultIn = in * toMult;
		int previous = (int) doMultIn;
		int next = previous + 1;
		float toReturn = closest(in, previous / toMult, next / toMult);
		return toReturn;
	}

	// Finds the absolute difference between two values (a, b)
	public static float diff(float a, float b) {
		return fastAbs(a - b);
	}

	// Finds the value that is closest to the input (in) between the candidates (a,
	// b)
	public static float closest(float in, float a, float b) {
		float diffa = diff(a, in);
		float diffb = diff(b, in);
		return diffb < diffa ? b : a;
	}

	// Finds the distance between two points: point1 (x1, y1) and point2 (x2, y2)
	public static float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(distance2(x1, y1, x2, y2));
	}

	// Finds the magnitude between two points: point1 (x1, y1) and point2 (x2, y2)
	public static float distance2(float x1, float y1, float x2, float y2) {
		float lx = diff(x1, x2);
		float ly = diff(y1, y2);
		return lx * lx + ly * ly;
	}

	// Clamps the input (in) to have the given minimum value (min)
	public static float clampMin(float in, float min) {
		return in < min ? min : in;
	}

	// Clamps the input (in) to have the given maximum value (max)
	public static float clampMax(float in, float max) {
		return in > max ? max : in;
	}

	// Clamps the input(in) to fit in the given range (min, max)
	public static float clamp(float in, float min, float max) {
		if (max > in && in > min) {
			return in;
		}
		if (in >= max) {
			return max;
		}
		if (in <= min) {
			return min;
		}
		return 0f;
	}

	// Finds the absolute value of the input (in)
	public static float fastAbs(float in) {
		return in < 0 ? -in : in;
	}

	// Rounds the given input (x) to the closest integer
	public static int fastFloor(float x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	// Tests whether a value (in) is in the given range (min, max)
	public static boolean inrange(float in, float min, float max) {
		return (in >= min && in <= max);
	}

	// Converts a pixel coordinate to a world coordinate
	public static void toGLCoord(float x, float y, float scale, float width, float height, Vector2f pointer) {

		float visiblex, visibley, resx, resy;

		visiblex = width / scale;
		visibley = height / scale;

		resx = clamp(x, 0, width);
		resy = clamp(y, 0, height);

		resx = (resx / width);
		resy = (resy / height);

		resx /= (1d / visiblex);
		resy /= -(1d / visibley);

		resx -= visiblex / 2f;
		resy += visibley / 2f;

		pointer.x = resx;
		pointer.y = resy;

	}

	// Overload of toGLCoord
	public static void mGLCoord(Vector2i pointer) {
		Game game = Game.getGame();
		GameWindow win = game.getWindow();
		Camera cam = game.getActiveCamera();
		win.getMousePosition(pointer);
		toGLCoord(pointer.x, pointer.y, cam.getCameraScale(), win.getWidth(), win.getHeight(), new Vector2f(pointer));
	}

	// Returns true only if an amount of (iterations) random consecutive booleans is
	// found to be true
	public static boolean random(Random r, int iterations) {
		while (iterations > 0) {
			boolean random = r.nextBoolean();
			if (!random) {
				return false;
			}
			iterations--;
		}
		return true;
	}

	// Finds the angle from (0, 0) to a point
	public static float lookAt(Vector2f point) {
		return lookAt(point.x, point.y);
	}

	// Finds the angle from (0, 0) to a point and adds PI to it
	public static float oppositeTo(Vector2f point) {
		return lookAt(point) + (float) Math.PI;
	}

	// Finds the angle from (0, 0) to a point
	public static float lookAt(float x, float y) {
		return lookAt(0, 0, x, y);
	}

	// Finds the angle from (0, 0) to a point and adds PI to it
	public static float oppositeTo(float x, float y) {
		return lookAt(x, y) + (float) Math.PI;
	}

	// Finds the angle from the given coordinates (x, y) to the second pair of
	// coordinates (x2, y2)
	public static float lookAt(float x, float y, float x2, float y2) {
		return (float) Math.atan2(y2 - y, x2 - x);
	}

	// Finds the angle from the given coordinates (from) to the second pair of
	// coordinates (to)
	public static float lookAt(Vector2f from, Vector2f to) {
		return lookAt(from.x, from.y, to.x, to.y);
	}

	// Finds the angle from the given coordinates (x, y) to the second pair of
	// coordinates (x2, y2) and adds PI to it
	public static float oppositeTo(float x, float y, float x2, float y2) {
		return lookAt(x, y, x2, y2) + (float) Math.PI;
	}

	// Finds the angle from the given coordinates (from) to the second pair of
	// coordinates (to) and adds PI to it
	public static float oppositeTo(Vector2f from, Vector2f to) {
		return lookAt(from, to) + (float) Math.PI;
	}

	// Smooth lerp methods

	public static float lerpEaseIn(float in, float a, float b) {
		float curve = clamp((in < 0.3f) ? (2.777f * (float) Math.pow(in, 2)) : ((1.111f * in) - 0.111f), 0, 1);
		return a + (b - a) * curve;
	}

	public static float lerpEaseOut(float in, float a, float b) {
		float curve = clamp(
				(in > 0.7f) ? ((-2.777f * (float) Math.pow(in, 2)) + (5.555f * in) - 1.777f) : (1.111f * in), 0, 1);
		return a + (b - a) * curve;
	}

	public static float lerpEaseBoth(float in, float a, float b) {
		float curve = clamp((in < 0.3f) ? (3.125f * (float) Math.pow(in, 2))
				: ((in > 0.7f) ? (-3.125f * (float) Math.pow(in, 2) + (6.25f * in) - 2.125f) : ((1.25f * in) - 0.125f)),
				0, 1);
		return a + (b - a) * curve;
	}

	// Finds the highest value between (a, b)
	public static float max(float a, float b) {
		return a > b ? a : b;
	}

	// Finds the lowest value between (a, b)
	public static float min(float a, float b) {
		return a < b ? a : b;
	}

	public static boolean equals(float a, float b, float delta) {
		return diff(a, b) <= delta;
	}

}
