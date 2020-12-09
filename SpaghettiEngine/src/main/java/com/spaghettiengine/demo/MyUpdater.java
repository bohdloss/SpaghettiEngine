package com.spaghettiengine.demo;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.components.Mesh;
import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.*;

public class MyUpdater extends Updater {

	protected Level level;

	protected Model square;
	protected ShaderProgram emptyShader;
	protected Material mat;
	protected Material mat2;
	protected Texture texture;
	protected Texture texture2;

	public MyUpdater(Game source) {
		super(source);
	}

	@Override
	public void init() {
		level = new Level();
		source.attachLevel(level);

		Function loadFunc = new Function(() -> {

			square = new Model(new float[] { -0.5f, 0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f, -0.5f, 0 },
					new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new int[] { 0, 1, 2, 2, 3, 0 });

			String vertSource = "#version 120\n" + "\n" + "attribute vec3 vertices;\n" + "attribute vec2 textures;\n"
					+ "\n" + "varying vec2 tex_coords;\n" + "\n" + "uniform mat4 projection;\n" + "\n"
					+ "void main() {\n" + "	tex_coords = textures;\n"
					+ "	gl_Position = projection * vec4(vertices, 1.0);\n" + "}";

			String fragSource = "# version 120\n" + "\n" + "uniform sampler2D sampler;\n" + "\n"
					+ "varying vec2 tex_coords;\n" + "\n" + "void main() {\n"
					+ "	gl_FragColor = texture2D(sampler, tex_coords);\n" + "}";

			Shader vertex = new Shader(vertSource, Shader.VERTEX_SHADER);
			Shader fragment = new Shader(fragSource, Shader.FRAGMENT_SHADER);

			emptyShader = new ShaderProgram(vertex, fragment);

			vertex.delete();
			fragment.delete();

			try {
				texture = new Texture(ResourceLoader.loadImage("/data/holesom2.png"));
				texture2 = new Texture(100, 100);
			} catch (Throwable e) {
				e.printStackTrace();
			}

			mat = new Material(texture, emptyShader);
			mat2 = new Material(texture2, emptyShader);

			return null;
		});
		long funcWait = source.getFunctionDispatcher().queue(loadFunc, source.getRendererId(), false);
		try {
			source.getFunctionDispatcher().waitReturnValue(funcWait);
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Camera camera = new Camera(level, null, source.getWindow().getWidth(), source.getWindow().getHeight());
		camera.setFov(3);
		Mesh a = new Mesh(level, null, square, mat);
		Mesh b = new Mesh(level, null, square, mat);
		b.setRelativeX(0.5);
		Mesh c = new Mesh(level, null, square, mat);

		level.attachCamera(camera);

		super.init();
	}

	double i;

	@Override
	protected void loopEvents() {
		super.loopEvents();
		i += 0.001;

		level.getComponent(1).setRotation(-i);

		GameComponent gc = level.getComponent(3);
		gc.setRelativeX(Math.cos(i) * 2);
		gc.setRelativeY(Math.sin(i) * 0.5);
		gc.setYScale((Math.cos(i + 0.3) + 1.2) * 0.5);

		level.getComponent(2).setRotation(i);
	}

}
