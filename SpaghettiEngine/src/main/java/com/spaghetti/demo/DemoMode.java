package com.spaghetti.demo;

import com.spaghetti.physics.d2.Shape2D;
import com.spaghetti.physics.d2.jbox2d.JBox2DPhysics;
import com.spaghetti.physics.d2.jbox2d.JBox2DRigidBody;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;
import com.spaghetti.world.GameMode;
import com.spaghetti.world.GameObject;
import com.spaghetti.world.GameState;
import com.spaghetti.world.Level;
import com.spaghetti.demo.player.Player;
import com.spaghetti.render.Camera;
import com.spaghetti.render.Mesh;
import com.spaghetti.render.UntransformedMesh;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.physics.d2.Physics2D;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import org.joml.Vector3f;

import java.util.Random;

public class DemoMode extends GameMode {

    protected Level level;
    protected Physics physics;
    protected Mesh square;
    protected Mesh square2;
    protected Mesh floor;
    protected Camera camera;
    protected Player player;

    protected GameObject floorContainer;

    @Override
    public void onBeginPlay() {
        if (!getGame().hasAuthority()) {
            return;
        }

        level = getGame().addLevel("myWorld");
        getGame().activateLevel("myWorld");

        // Init physics world
        level.addObject(Physics2D.getInstance());

        // Init floor meshes
        floorContainer = new GameObject();
        floorContainer.setRelativeScale(500, 1, 1);
        floorContainer.addComponent(new JBox2DRigidBody(RigidBody.BodyType.STATIC));
        floorContainer.getComponent(RigidBody2D.class).setFriction(0.3f);
        float length = floorContainer.getXScale();
        int halfLength = (int) (length / 2);
        for (int i = -halfLength; i <= halfLength - 1; i++) {
            Mesh floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
            floorContainer.addChild(floor);
            floor.setWorldScale(1, 1, 1);
            floor.setRelativePosition(i + 0.5f, 0, 0);
        }
        level.addObject(floorContainer);

        // Init skybox
        UntransformedMesh skybox = new UntransformedMesh(Model.get("square"), Material.get("m_skybox"));
        skybox.setRelativeScale(50, 50, 1);
        skybox.setRelativeZ(-10);
        level.addObject(skybox);

        // Stress test for physics
        int width = 10;
        int height = 10;
        Random r = new Random();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Mesh mesh = new Mesh(Model.get("apple_model"), Material.get("apple_mat"));
                RigidBody2D mesh_body = RigidBody2D.getInstance();
                mesh_body.setFriction(0.3f);
                mesh_body.setAngularDamping(0);
                Shape2D shape = new Shape2D();
                shape.setRadius(0.5f);
                mesh_body.setShape(shape);
                mesh.addComponent(mesh_body);
                mesh.setRelativePosition(i * 1.1f, j * 10, 0);
                mesh.setRelativeScale(-1f, r.nextBoolean() ? -1f : 1f, -1);
                level.addObject(mesh);
            }
        }

//		for (int i = 0; i < width; i++) {
//			for(int j = 0; j < height; j++) {
//				Mesh mesh = new Mesh(Model.get("square"), Material.get("defaultMAT"));
//				mesh.addComponent(new RigidBody2D());
//				mesh.setRelativePosition(1000 + i * 10, j * 10, 0);
//				mesh.setRelativeScale(2f, 2f, 1);
//				mesh.setProjectionCaching(false);
//				level.addObject(mesh);
//				mesh.getComponent(RigidBody2D.class).setShape(new Shape<Vector2f>(1));
//				mesh.getComponent(RigidBody2D.class).setMass(1000000);
//				System.out.println("Register rigidbody... " + (1 + ((i * height) + j)) + "/" + (width * height));
//			}
//		}

        // Init local player
        if (!getGame().isMultiplayer()) {
            Player p = new Player();
            p.setRelativePosition(0, 10, 0);
            level.addObject(p);
        }

        // Test physics on moving object
//		MovingMesh mm = new MovingMesh(Model.get("square"), Material.get("defaultMAT"));
//		mm.setRelativeScale(2, 2, 1);
//		RigidBody2D mm_body = new RigidBody2D();
//		mm_body.setFriction(0.3f);
//		mm_body.setAngularDamping(0);
//		mm.addComponent(mm_body);
//		level.addObject(mm);
    }

    public GameObject getFloorContainer() {
        return floorContainer;
    }

    @Override
    public void update(float delta) {
//        ThreadUtil.sleep(100);
    }

}