package com.spaghetti.physics;

import java.util.ArrayList;

/**
 * RaycastRequest contains the parameters as well as the {@link RaycastHit}
 * return values for {@link Physics#raycast(RaycastRequest)}
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
public class RaycastRequest<VecType, SecVecType, BodyClass extends RigidBody<VecType, SecVecType>> {

	/**
	 * PARAMETER: The beginning point of the ray to cast
	 */
	public VecType beginning;
	/**
	 * PARAMETER: The end point of the ray to cast
	 */
	public VecType end;

	/**
	 * RETURN: A list of {@link RaycastHit}'s
	 */
	public ArrayList<RaycastHit<VecType, SecVecType, BodyClass>> hits = new ArrayList<>(1);

}
