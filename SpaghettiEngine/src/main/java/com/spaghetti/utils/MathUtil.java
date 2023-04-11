package com.spaghetti.utils;

import java.util.Random;

import org.joml.Vector2f;
import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameWindow;
import com.spaghetti.render.Camera;

public final class MathUtil {

	private MathUtil() {
	}

	/**
	 * Maps a value from the given range to the equivalent
	 * in the other range
	 *
	 * @param value The value in the first range
	 * @param min1 The lowermost value of the first range
	 * @param max1 The uppermost value of the first range
	 * @param min2 The lowermost value of the second range
	 * @param max2 The uppercase value of the second range
	 * @return The remapped value
	 */
	public static float remap(float value, float min1, float max1, float min2, float max2) {
		return lerp(reverseLerp(value, min1, max1), min2, max2);
	}

	/**
	 * Performs linear interpolation between two values given a percent
	 *
	 * @param alpha The percent
	 * @param a The lowermost value of the range
	 * @param b The uppermost value of the range
	 * @return The interpolated value
	 */
	public static float lerp(float alpha, float a, float b) {
		if (alpha >= 1) {
			return b;
		}
		if (alpha <= 0) {
			return a;
		}
		return a + ((b - a) * alpha);
	}

	/**
	 * Inverse of the {@link #lerp(float, float, float)} function
	 * Given the lerp value finds the alpha
	 *
	 * @param lerp The lerp value
	 * @param a The lowermost value of the range
	 * @param b The uppermost value of the range
	 * @return The percent or alpha
	 */
	public static float reverseLerp(float lerp, float a, float b) {
		int range = inRangeIgnoreSign(lerp, a, b);
		if (range != 0) {
			return range == -1 ? 0 : 1;
		}
		return (lerp - a) / (b - a);
	}

	/**
	 * Checks if a value is in the specified range ignoring
	 * the order of the given bounds
	 *
	 * @param value The value
	 * @param a One bound of the range
	 * @param b Another bound of the range
	 * @return 0 if the value is in range, -1 if out of range on the a side and 1 if on the b side
	 */
	public static int inRangeIgnoreSign(float value, float a, float b) {
		if (a <= b) {
			return value < a ? -1 : (value > b ? 1 : 0);
		} else {
			return value > a ? -1 : (value < b ? 1 : 0);
		}
	}

	/**
	 * Works like {@link #fastFloor(float)} but instead of
	 * rounding to the closest integer, lets you define a
	 * step value and rounds to the nearest step
	 *
	 * @param step The step size
	 * @param in The value to round
	 * @return The step that's closest to the value
	 */
	public static float approx(float step, float in) {
		float toMult = 1 / step;
		float doMultIn = in * toMult;
		int previous = (int) doMultIn;
		int next = previous + 1;
		float toReturn = closest(in, previous / toMult, next / toMult);
		return toReturn;
	}

	/**
	 * Finds the absolute of the difference between two values
	 *
	 * @param a The first value
	 * @param b The second value
	 * @return The absolute difference
	 */
	public static float diff(float a, float b) {
		return fastAbs(a - b);
	}

	/**
	 * Returns one of two values that the given input is closest to
	 *
	 * @param in The input
	 * @param a The first value
	 * @param b The second value
	 * @return The value that is closest to the given input
	 */
	public static float closest(float in, float a, float b) {
		float diffa = diff(a, in);
		float diffb = diff(b, in);
		return diffb < diffa ? b : a;
	}

	/**
	 * Computes the distance between two 2D points
	 * This function is the equivalent of computing the square root
	 * of {@link #distance2(float, float, float, float)}
	 *
	 * @param x1 The first point's x
	 * @param y1 The first point's y
	 * @param x2 The second point's x
	 * @param y2 The second point's y
	 * @return The distance
	 */
	public static float distance(float x1, float y1, float x2, float y2) {
		float lx = x1 - x2;
		float ly = y1 - y2;
		return (float) Math.sqrt(lx * lx + ly * ly);
	}

	/**
	 * Computes the magnitude between two 2D points
	 * This function is the equivalent of computing the power of 2
	 * of {@link #distance(float, float, float, float)}
	 *
	 * @param x1 The first point's x
	 * @param y1 The first point's y
	 * @param x2 The second point's x
	 * @param y2 The second point's y
	 * @return The magnitude
	 */
	public static float distance2(float x1, float y1, float x2, float y2) {
		float lx = x1 - x2;
		float ly = y1 - y2;
		return lx * lx + ly * ly;
	}

	/**
	 * Clamps the value to the given minimum value
	 *
	 * @param in The value
	 * @param min The minimum value
	 * @return The clamped value
	 */
	public static float clampMin(float in, float min) {
		return in < min ? min : in;
	}

	/**
	 * Clamps the value to the given maximum value
	 *
	 * @param in The value
	 * @param max The maximum value
	 * @return The clamped value
	 */
	public static float clampMax(float in, float max) {
		return in > max ? max : in;
	}

	/**
	 * Clamps the value to the given range
	 *
	 * @param in The value
	 * @param min The lowermost bound of the range
	 * @param max The uppermost bound of the range
	 * @return The clamped value
	 */
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

	/**
	 * Computes the absolute value of the input
	 *
	 * @param in The input value
	 * @return The absolute
	 */
	public static float fastAbs(float in) {
		return in < 0 ? -in : in;
	}

	/**
	 * Rounds the given value to the closest integer
	 *
	 * @param x The value
	 * @return The integer that is closest
	 */
	public static int fastFloor(float x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	/**
	 * Checks whether a value is in a range
	 *
	 * @param in The value
	 * @param min The lowermost bound of the range
	 * @param max The uppermost bound of the range
	 * @return Whether the value belongs to the range or not
	 */
	public static boolean inRange(float in, float min, float max) {
		return (in >= min && in <= max);
	}

	/**
	 * Converts a pixel coordinate on the window to the opengl coordinate space
	 *
	 * @param x The pixel-space x
	 * @param y The pixel-space y
	 * @param scale The camera scale
	 * @param width The window width
	 * @param height The window height
	 * @param pointer The pointer where the return value will be stored
	 */
	public static void toGLCoord(float x, float y, float scale, float width, float height, Vector2f pointer) {

		float visiblex, visibley, resx, resy;

		visiblex = width / scale;
		visibley = height / scale;

		resx = clamp(x, 0, width);
		resy = clamp(y, 0, height);

		resx /= width;
		resy /= height;

		resx /= (1f / visiblex);
		resy /= -(1f / visibley);

		resx -= visiblex / 2f;
		resy += visibley / 2f;

		pointer.x = resx;
		pointer.y = resy;

	}

	/**
	 * Overload of {@link #toGLCoord(float, float, float, float, float, Vector2f)}
	 * where the mouse coordinates are used and the other arguments are gathered
	 * from the game state
	 *
	 * @param pointer The pointer where the return value will be stored
	 */
	public static void mouseGLCoord(Vector2i pointer) {
		Game game = Game.getInstance();
		GameWindow win = game.getWindow();
		Camera cam = game.getLocalCamera();
		win.getMousePosition(pointer);
		toGLCoord(pointer.x, pointer.y, cam.getCameraScale(), win.getWidth(), win.getHeight(), new Vector2f(pointer));
	}

	/**
	 * This functions returns if a series of random booleans
	 * are all simultaneously true
	 *
	 * @param r The generator
	 * @param iterations The length of the boolean series
	 * @return Whether all the booleans were true or not
	 */
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

	/**
	 * Finds the angle between the point O(0, 0) and the given point
	 *
	 * @param point The point
	 * @return The angle
	 */
	public static float lookAt(Vector2f point) {
		return lookAt(point.x, point.y);
	}

	/**
	 * Finds the angle between the point O(0, 0) and the given point
	 * and adds PI to it, making it the opposite angle
	 *
	 * @param point The point
	 * @return The angle
	 */
	public static float oppositeTo(Vector2f point) {
		return lookAt(point) + (float) Math.PI;
	}

	/**
	 * Finds the angle between the point O(0, 0) and the given point
	 *
	 * @param x The point's x
	 * @param y The point's y
	 * @return The angle
	 */
	public static float lookAt(float x, float y) {
		return lookAt(0, 0, x, y);
	}

	/**
	 * Finds the angle between the point O(0, 0) and the given point
	 * and adds PI to it, making it the opposite angle
	 *
	 * @param x The point's x
	 * @param y The point's y
	 * @return The angle
	 */
	public static float oppositeTo(float x, float y) {
		return lookAt(x, y) + (float) Math.PI;
	}

	/**
	 * Finds the angle between the two given points
	 *
	 * @param x1 The first point's x
	 * @param y1 The first point's y
	 * @param x2 The second point's x
	 * @param y2 The second point's y
	 * @return The angle
	 */
	public static float lookAt(float x1, float y1, float x2, float y2) {
		return (float) Math.atan2(y2 - y1, x2 - x1);
	}

	/**
	 * Finds the angle between the two given points
	 *
	 * @param from The first point
	 * @param to The second point
	 * @return The angle
	 */
	public static float lookAt(Vector2f from, Vector2f to) {
		return lookAt(from.x, from.y, to.x, to.y);
	}

	/**
	 * Finds the angle between the two given points
	 * and adds PI to it, making it the opposite angle
	 *
	 * @param x1 The first point's x
	 * @param y1 The first point's y
	 * @param x2 The second point's x
	 * @param y2 The second point's y
	 * @return The angle
	 */
	public static float oppositeTo(float x1, float y1, float x2, float y2) {
		return lookAt(x1, y1, x2, y2) + (float) Math.PI;
	}

	/**
	 * Finds the angle between the two given points
	 * and adds PI to it, making it the opposite angle
	 *
	 * @param from The first point
	 * @param to The second point
	 * @return The angle
	 */
	public static float oppositeTo(Vector2f from, Vector2f to) {
		return lookAt(from, to) + (float) Math.PI;
	}

	// Smooth lerp methods

	/**
	 * Smooth lerp function that is smoother in the beginning
	 *
	 * @param in The alpha value
	 * @param a The lowermost bound of the range
	 * @param b The uppermost bound of the range
	 * @return The interpolated value
	 */
	public static float lerpEaseIn(float in, float a, float b) {
		float curve = clamp((in < 0.3f) ? (2.777f * (float) Math.pow(in, 2)) : ((1.111f * in) - 0.111f), 0, 1);
		return a + (b - a) * curve;
	}

	/**
	 * Smooth lerp function that is smoother in the end
	 *
	 * @param in The alpha value
	 * @param a The lowermost bound of the range
	 * @param b The uppermost bound of the range
	 * @return The interpolated value
	 */
	public static float lerpEaseOut(float in, float a, float b) {
		float curve = clamp(
				(in > 0.7f) ? ((-2.777f * (float) Math.pow(in, 2)) + (5.555f * in) - 1.777f) : (1.111f * in), 0, 1);
		return a + (b - a) * curve;
	}

	/**
	 * Smooth lerp function that is smoother in the beginning and end
	 *
	 * @param in The alpha value
	 * @param a The lowermost bound of the range
	 * @param b The uppermost bound of the range
	 * @return The interpolated value
	 */
	public static float lerpEaseBoth(float in, float a, float b) {
		float curve = clamp((in < 0.3f) ? (3.125f * (float) Math.pow(in, 2))
				: ((in > 0.7f) ? (-3.125f * (float) Math.pow(in, 2) + (6.25f * in) - 2.125f) : ((1.25f * in) - 0.125f)),
				0, 1);
		return a + (b - a) * curve;
	}

	/**
	 * Finds the biggest between two values
	 *
	 * @param a The first value
	 * @param b The second value
	 * @return The biggest value
	 */
	public static float max(float a, float b) {
		return a > b ? a : b;
	}

	/**
	 * Finds the smallest between two values
	 *
	 * @param a The first value
	 * @param b The second value
	 * @return The smallest value
	 */
	public static float min(float a, float b) {
		return a < b ? a : b;
	}

	/**
	 * Checks if two floating point values are equal
	 * within the given tolerance
	 *
	 * @param a The first value
	 * @param b The second value
	 * @param error The tolerance
	 * @return Whether the difference between the two values is lower than the error
	 */
	public static boolean equals(float a, float b, float error) {
		return diff(a, b) <= error;
	}

}
