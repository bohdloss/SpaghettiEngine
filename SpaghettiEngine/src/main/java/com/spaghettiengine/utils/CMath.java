package com.spaghettiengine.utils;

import java.util.Random;

import org.joml.Vector2d;

import com.spaghettiengine.core.Game;
import com.spaghettiengine.core.GameWindow;
import com.spaghettiengine.core.Level;
import com.spaghettiengine.objects.Camera;

public final class CMath {

	private CMath() {
	}

	// Map a value (value) that is valid in a specified range (min1, max1)
	// to the equivalent in the the given new range (min2, max2)
	public static double remap(double value, double min1, double max1, double min2, double max2) {
		return lerp(reverseLerp(value, min1, max1), min2, max2);
	}

	// Linear interpolate in a range (a, b) using a percentage value (alpha)
	public static double lerp(double alpha, double a, double b) {
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
	public static double reverseLerp(double lerp, double a, double b) {
		int range = inRangeIgnoreSign(lerp, a, b);
		if (range != 0) {
			return range == -1 ? 0 : 1;
		}
		return (lerp - a) / (b - a);
	}

	// Checks if a value (val) is in the specified range (a, b) regardless if a is
	// bigger than b
	public static int inRangeIgnoreSign(double val, double a, double b) {
		if (a <= b) {
			return val < a ? -1 : (val > b ? 1 : 0);
		} else {
			return val > a ? -1 : (val < b ? 1 : 0);
		}
	}

	// Just like floor function (in), except
	// you get to define the size of interval (step)
	public static double approx(double step, double in) {
		double toMult = 1d / step;
		double doMultIn = in * toMult;
		int previous = (int) doMultIn;
		int next = previous + 1;
		double toReturn = closest(in, previous / toMult, next / toMult);
		return toReturn;
	}

	// Finds the absolute difference between two values (a, b)
	public static double diff(double a, double b) {
		return fastAbs(a - b);
	}

	// Finds the value that is closest to the input (in) between the candidates (a,
	// b)
	public static double closest(double in, double a, double b) {
		double diffa = diff(a, in);
		double diffb = diff(b, in);
		return diffb < diffa ? b : a;
	}

	// Finds the distance between two points: point1 (x1, y1) and point2 (x2, y2)
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt(distance2(x1, y1, x2, y2));
	}

	// Finds the magnitude between two points: point1 (x1, y1) and point2 (x2, y2)
	public static double distance2(double x1, double y1, double x2, double y2) {
		double lx = diff(x1, x2);
		double ly = diff(y1, y2);
		return lx * lx + ly * ly;
	}

	// Clamps the input (in) to have the given minimum value (min)
	public static double clampMin(double in, double min) {
		return in < min ? min : in;
	}

	// Clamps the input (in) to have the given maximum value (max)
	public static double clampMax(double in, double max) {
		return in > max ? max : in;
	}

	// Clamps the input(in) to fit in the given range (min, max)
	public static double clamp(double in, double min, double max) {
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
	public static double fastAbs(double in) {
		return in < 0 ? -in : in;
	}

	// Rounds the given input (x) to the closest integer
	public static int fastFloor(double x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	// Tests whether a value (in) is in the given range (min, max)
	public static boolean inrange(double in, double min, double max) {
		return (in >= min && max <= max);
	}

	// Converts a pixel coordinate to a world coordinate
	public static Vector2d toGLCoord(double x, double y, double scale, double width, double height, Vector2d pointer) {
		double visiblex, visibley, resx, resy;

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

		return pointer;
	}

	// Overload of toGLCoord
	public static Vector2d mGLCoord(Vector2d pointer) {
		Game game = Game.getGame();
		GameWindow win = game.getWindow();
		Level level = game.getActiveLevel();
		Camera cam = level.getActiveCamera();
		win.getMousePosition(pointer);
		return toGLCoord(pointer.x, pointer.y, cam.getCameraScale(), win.getWidth(), win.getHeight(), pointer);
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
	public static double lookAt(Vector2d point) {
		return lookAt(point.y, point.x);
	}

	// Finds the angle from (0, 0) to a point and adds PI to it
	public static double oppositeTo(Vector2d point) {
		return lookAt(point) + Math.PI;
	}

	// Finds the angle from (0, 0) to a point
	public static double lookAt(double x, double y) {
		return lookAt(0, 0, x, y);
	}

	// Finds the angle from (0, 0) to a point and adds PI to it
	public static double oppositeTo(double x, double y) {
		return lookAt(x, y) + Math.PI;
	}

	// Finds the angle from the given coordinates (x, y) to the second pair of coordinates (x2, y2)
	public static double lookAt(double x, double y, double x2, double y2) {
		return Math.atan2(y2 - y, x2 - x);
	}
	
	// Finds the angle from the given coordinates (from) to the second pair of coordinates (to)
	public static double lookAt(Vector2d from, Vector2d to) {
		return lookAt(from.x, from.y, to.x, to.y);
	}
	
	// Finds the angle from the given coordinates (x, y) to the second pair of coordinates (x2, y2) and adds PI to it
	public static double oppositeTo(double x, double y, double x2, double y2) {
		return lookAt(x, y, x2, y2) + Math.PI;
	}
	
	// Finds the angle from the given coordinates (from) to the second pair of coordinates (to) and adds PI to it
	public static double oppositeTo(Vector2d from, Vector2d to) {
		return lookAt(from, to) + Math.PI;
	}
	
	// Smooth lerp methods

	public static double lerpEaseIn(double in, double a, double b) {
		double curve = clamp((in < 0.3) ? (2.777 * Math.pow(in, 2)) : ((1.111 * in) - 0.111), 0, 1);
		return a + (b - a) * curve;
	}

	public static double lerpEaseOut(double in, double a, double b) {
		double curve = clamp((in > 0.7) ? ((-2.777 * Math.pow(in, 2)) + (5.555 * in) - 1.777) : (1.111 * in), 0, 1);
		return a + (b - a) * curve;
	}

	public static double lerpEaseBoth(double in, double a, double b) {
		double curve = clamp(
				(in < 0.3) ? (3.125 * Math.pow(in, 2))
						: ((in > 0.7) ? (-3.125 * Math.pow(in, 2) + (6.25 * in) - 2.125) : ((1.25 * in) - 0.125)),
				0, 1);
		return a + (b - a) * curve;
	}

	// Finds the highest value between (a, b)
	public static double max(double a, double b) {
		return a > b ? a : b;
	}

	// Finds the lowest value between (a, b)
	public static double min(double a, double b) {
		return a < b ? a : b;
	}

}
