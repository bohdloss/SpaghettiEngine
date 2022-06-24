package com.spaghetti.physics;

/**
 * a RaycastHit object represents one of the return values from
 * {@link Physics#raycast(RaycastRequest)}
 * 
 * @author bohdloss
 *
 * @param <VecType>    Children classes are required to specify their own
 *                     positional vector class
 * @param <SecVecType> Children classes are required to specify their own
 *                     rotational vector class
 * @param <BodyClass>  Children classes are required to specify their own body
 *                     class
 */
public class RaycastHit<VecType, SecVecType, BodyClass extends RigidBody<VecType, SecVecType>> {

	/**
	 * The point where the ray collided
	 */
	public VecType point;
	/**
	 * The collision normal
	 */
	public SecVecType normal;
	/**
	 * The body the ray collided with
	 */
	public BodyClass body;

}
