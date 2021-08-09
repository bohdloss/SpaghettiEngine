package com.spaghetti.physics;

import java.util.ArrayList;

public class RaycastRequest<VecType, BodyClass> {

	public VecType beginning;
	public VecType end;

	public ArrayList<RaycastHit<VecType, BodyClass>> hits = new ArrayList<>(1);

}
