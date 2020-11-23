package com.spaghettiengine.core;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector2d;

public abstract class GameComponent implements Tickable, Renderable {

	//Hierarchy and utility
	
	private static long staticId=0;
	
	private static final void rebuildHierarchy(GameComponent caller, GameComponent child) {
		child.parent.parent = caller;
		child.hierarchy = caller.hierarchy + 1;
		
		Object[] obj = child.children.entrySet().toArray();
		for(Object current : obj) {
			@SuppressWarnings("unchecked") Entry<Long, GameComponent> entry = (Entry<Long, GameComponent>) current;
			GameComponent element = entry.getValue();
			
			rebuildHierarchy(child, element);
		}
		
	}
	
		//Instance methods and fields
	
	private int hierarchy; //This identifies how deep this component is in the hierarchy
	private long id; //This uniquely identifies any component
	private Level level;
	private GameComponent parent;
	private LinkedHashMap<Long, GameComponent> children = new LinkedHashMap<Long, GameComponent>();
	private Object[] entryset;
	private boolean rebuildListNeeded;
	
	public GameComponent(Level level, GameComponent parent) {
		this.level = level;
		this.parent = parent;
		this.id = staticId++;
		
		if(parent == null) hierarchy = 0;
		else hierarchy = parent.hierarchy + 1;
		
		this.level.ordered.put(this.id, this);
		
		rebuildListNeeded();
	}
	
	public final void addChildren(GameComponent component) {
		if(component.parent == this) return;
		if(component == this) return;
		
		rebuildHierarchy(this, component);
		
		children.put(component.id, component);
		
		rebuildListNeeded();
	}
	
	private final void rebuildList() {
		if(rebuildListNeeded) {
			entryset = children.entrySet().toArray();
			rebuildListNeeded = false;
		}
	}
	
	private final void rebuildListNeeded() {
		rebuildListNeeded = true;
	}
	
	@SuppressWarnings("unchecked")
	public final GameComponent getChild(int index) {
		rebuildList();
		return ((Entry<Long, GameComponent>) entryset[index]).getValue();
	}
	
	public final GameComponent getChild(long id) {
		rebuildList();
		return children.get(id);
	}
	
	public final void removeChild(long id) {
		children.remove(id);
		
		rebuildListNeeded();
	}
	
	public final int getChildrenAmount() {
		return children.size();
	}
	
	public final GameComponent getParent() {
		return parent;
	}
	
	public final void clearChildren() {
		children.forEach((key, component)->{
			component.destroy();
		});
		children.clear();
	}
	
	public final Level getLevel() {
		return level;
	}
	
	public final long getId() {
		return id;
	}
	
	public final void destroy() {
		level.ordered.remove(this.id);
		id = -1;
		hierarchy = -1;
		level = null;
		parent = null;
		clearChildren();
		children = null;
	}
	
	public final boolean isDestroyed() {
		return id == -1 || hierarchy == -1 || level == null || children == null;
	}
	
	public final GameComponent getBase() {
		if(hierarchy == 0) return this;
		if(hierarchy == 1) return parent;
		
		GameComponent last = parent;
		while(last.hierarchy != 0) {
			last = last.parent;
		}
		return last;
	}
	
	
	
	//World interaction
	
	protected Vector2d relativePos = new Vector2d();
	protected Vector2d scale = new Vector2d(1, 1);
	protected double rotation;
	
	//Cache
	private Vector2d getRelativePosCache = new Vector2d();
	private Vector2d getWorldPosCache = new Vector2d();
	private Vector2d getScaleCache = new Vector2d();
	
	//Position getters
	
	public final Vector2d getRelativePosition() {
		getRelativePosCache.x = relativePos.x;
		getRelativePosCache.y = relativePos.y;
		
		return getRelativePosCache;
	}
	
	public final Vector2d getWorldPosition() {
		if(parent == null) return getRelativePosition();
		
		getRelativePosition();
		getWorldPosCache.add(parent.getWorldPosition());
		return getWorldPosCache;
	}
	
	public final double getRelativeX() {
		return relativePos.x;
	}
	
	public final double getRelativeY() {
		return relativePos.y;
	}
	
	public final double getWorldX() {
		return getWorldPosition().x;
	}
	
	public final double getWorldY() {
		return getWorldPosition().y;
	}
	
	//Position setters
	
	public void setRelativePosition(Vector2d vec) {
		relativePos.x = vec.x;
		relativePos.y = vec.y;
	}
	
	public void setWorldPosition(Vector2d vec) {
		Vector2d worldPos = getWorldPosition();
		
		double xdiff = worldPos.x - vec.x;
		double ydiff = worldPos.y - vec.y;
		
		relativePos.x += xdiff;
		relativePos.y += ydiff;
	}
	
	public void setRelativeX(double x) {
		relativePos.x = x;
	}
	
	public void setRelativeY(double y) {
		relativePos.y = y;
	}
	
	public void setWorldX(double worldx) {
		Vector2d worldPos = getWorldPosition();
		
		double xdiff = worldPos.x - worldx;
		
		relativePos.x += xdiff;
	}
	
	public void setWorldY(double worldy) {
		Vector2d worldPos = getWorldPosition();
		
		double ydiff = worldPos.y - worldy;
		
		relativePos.y += ydiff;
	}
	
	//Scale getters
	
	public final Vector2d getScale() {
		getScaleCache.x = scale.x;
		getScaleCache.y = scale.y;
		
		return getScaleCache;
	}
	
	public final double getXScale() {
		return scale.x;
	}
	
	public final double getYScale() {
		return scale.y;
	}
	
	//Scale setters
	
	public void setScale(Vector2d vec) {
		scale.x = vec.x;
		scale.y = vec.y;
	}
	
	public void setXScale(double x) {
		scale.x = x;
	}
	
	public void setYScale(double y) {
		scale.y = y;
	}
	
	//Rotation getters
	
	public final double getRotation() {
		return rotation;
	}
	
	//Rotation setters
	
	public void setRotation(double rot) {
		this.rotation = rot;
	}
	
	
	//Physics
	
	protected boolean hasPhysics = false;
	protected Vector2d velocity = new Vector2d();
	protected Vector2d gravityMultiplier = new Vector2d(1,1);
	protected Vector2d gravityOverride = new Vector2d();
	
	protected boolean hasCollision = true;
	protected Vector2d size = new Vector2d(1,1);
	protected double mass = 10;
	
	//Interface methods
	
	protected Matrix4d cache;
	
	@Override
	public final void update(float delta) {
		if(hasPhysics) physicsUpdate(Game.getGame().getTickMultiplier(delta));
		
		fixedUpdate(delta);
		
		children.forEach((id, component)->{
			component.update(delta);
		});
	}
	
	public void fixedUpdate(float delta) {}
	
	protected void physicsUpdate(float multiplier) {
		
	}
	
	@Override
	public void render(Matrix4d projection) {
		cache = projection.translate(relativePos.x, relativePos.y, 0.0).rotate(rotation, 0, 0, 1).scale(scale.x, scale.y, 1);
		renderUpdate();
	}
	
	public void renderUpdate() {}
	
}
