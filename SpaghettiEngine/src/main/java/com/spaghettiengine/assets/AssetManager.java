package com.spaghettiengine.assets;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.spaghettiengine.core.Game;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.*;
import static com.spaghettiengine.assets.AssetType.*;

public final class AssetManager {

	// Always better keep a reference to game
	protected Game source;

	// Asset sheet links asset names to relative paths
	private AssetSheet sheet;

	// Cache empty assets
	private HashMap<String, RenderObject> cache = new HashMap<>();
	// Indicates flags associated with an asset
	private HashMap<String, AssetFlag> flags = new HashMap<>();

	// Lambdas' limitations workaround
	private Throwable lambda_error;

	public AssetManager(Game source) {
		this.source = source;
		sheet = new AssetSheet(this);
	}

	// Destroy every resource currently loaded
	public void deleteAll() {

		cache.forEach((str, asset) -> {
			asset.delete();
		});

	}

	// Destroy every resource currently loaded and collect possible garbage
	public void resetAll() {
		deleteAll();

		cache.forEach((str, asset) -> {
			asset.reset();
		});

		System.gc();
	}

	// Remove all dummy resources
	protected void removeDummy() {
		cache.clear();
		flags.clear();
	}

	// Reset this instance to its state upon creation
	public synchronized void destroy() {
		deleteAll();
		resetAll();
		removeDummy();
		sheet.clear();
	}

	// Initialize dummies
	public void initDummy() throws Throwable {
		lambda_error = null;

		sheet.sheet.forEach((name, asset) -> {
			try {

				instantiate(asset);

			} catch (Throwable t) {
				lambda_error = t;
			}
		});

		if (lambda_error != null) {
			throw lambda_error;
		}
	}

	@SuppressWarnings("unchecked")
	private void instantiate(SheetEntry cls) throws Throwable {
		try {
			Class<? extends RenderObject> customClass = (Class<? extends RenderObject>) Class.forName(cls.customType);
			Constructor<? extends RenderObject> custom = customClass.getConstructor(new Class<?>[0]);
			cache.put(cls.name, custom.newInstance(new Object[0]));
			flags.put(cls.name, new AssetFlag());
		} catch (Throwable t) {
			Logger.error(source, "Could not instantiate asset:" + "\nname=" + cls.name + "\nlocation=" + cls.location
					+ "\nisCustom=" + cls.isCustom + "\ncustomType=" + cls.customType, t);
			throw t;
		}
	}

	public void loadAssetSheetSource(String source) throws Throwable {

		// Invoke function
		sheet.loadAssetSheet(source);

		// Now reset dummies
		deleteAll();
		resetAll();
		removeDummy();
		initDummy();

	}

	public void loadAssetSheet(String sheetLocation) throws Throwable {

		try {

			String sheetSource = ResourceLoader.loadText(sheetLocation);
			loadAssetSheetSource(sheetSource);

			Logger.loading(source, "Successfully loaded asset sheet " + sheetLocation);

		} catch (Throwable t) {

			Logger.error(source, "Could not load asset sheet " + sheetLocation, t);
			throw t;

		}

	}

	private synchronized void flagAsset(String type, String name) {
		flagAsset(type, name, true);
	}

	private synchronized void flagAsset(String type, String name, boolean flag) {
		AssetFlag aflag = flags.get(name);
		if (flag) {
			if (!cache.get(name).valid()) {
				aflag.needLoad = flag;
			}
		} else {
			aflag.needLoad = flag;
		}
		aflag.type = type;
	}

	public synchronized void lazyLoad() throws Throwable {
		cache.forEach((name, asset) -> {
			AssetFlag flag = flags.get(name);
			if (flag.needLoad) {
				loadAsset(flag.type, name);
			}
		});
	}

	// Loader methods

	private void fillAsset(String type, String name) throws Throwable {
			SheetEntry info = sheet.sheet.get(name);
			switch (type) {
			case MODEL:
				Model model = model(name);
				AssetLoader.loadModel(model, info);
				break;
			case SHADER:
				Shader shader = shader(name);
				AssetLoader.loadShader(shader, info);
				break;
			case SHADERPROGRAM:
				ShaderProgram shaderProgram = shaderProgram(name);
				AssetLoader.loadShaderProgram(this, shaderProgram, info);
				break;
			case TEXTURE:
				Texture texture = texture(name);
				AssetLoader.loadTexture(texture, info);
				break;
			case MATERIAL:
				Material material = material(name);
				AssetLoader.loadMaterial(this, material, info);
				break;
			case CUSTOM:
				RenderObject custom = custom(name);
				AssetLoader.loadCustom(custom, info);
				break;
			}
	}

	public synchronized void loadAsset(String type, String name) {
		long load = source.getFunctionDispatcher().queue(new Function(() -> {
			fillAsset(type, name);
			custom(name).load();
			flagAsset(type, name, false);
			return null;
		}));
		try {
			source.getFunctionDispatcher().getReturnValue(load);
		} catch (Throwable t) {
			Logger.error("Could not load " + type + " " + name, t);
			source.stopAsync();
		}
	}

	// Lazy-loading getters

	private void check(String name) {
		if (cache.get(name) == null) {
			throw new NullPointerException("Non existant asset requested");
		}
	}

	public RenderObject custom(String name) {
		check(name);
		RenderObject ret = cache.get(name);
		flagAsset(CUSTOM, name);
		return ret;
	}

	public Model model(String name) {
		check(name);
		Model ret = (Model) cache.get(name);
		flagAsset(MODEL, name);
		return ret;
	}

	public Shader shader(String name) {
		check(name);
		Shader ret = (Shader) cache.get(name);
		flagAsset(SHADER, name);
		return ret;
	}

	public ShaderProgram shaderProgram(String name) {
		check(name);
		ShaderProgram ret = (ShaderProgram) cache.get(name);
		flagAsset(SHADERPROGRAM, name);
		return ret;
	}

	public Texture texture(String name) {
		check(name);
		Texture ret = (Texture) cache.get(name);
		flagAsset(TEXTURE, name);
		return ret;
	}

	public Material material(String name) {
		check(name);
		Material ret = (Material) cache.get(name);
		flagAsset(MATERIAL, name);
		return ret;
	}

	// Instant-loading getters

	public RenderObject requireCustom(String name) {
		check(name);
		RenderObject ret = cache.get(name);
		loadAsset(CUSTOM, name);
		return ret;
	}

	public Model requireModel(String name) {
		check(name);
		Model ret = (Model) cache.get(name);
		loadAsset(MODEL, name);
		return ret;
	}

	public Shader requireShader(String name) {
		check(name);
		Shader ret = (Shader) cache.get(name);
		loadAsset(SHADER, name);
		return ret;
	}

	public ShaderProgram requireShaderProgram(String name) {
		check(name);
		ShaderProgram ret = (ShaderProgram) cache.get(name);
		loadAsset(SHADERPROGRAM, name);
		return ret;
	}

	public Texture requireTexture(String name) {
		check(name);
		Texture ret = (Texture) cache.get(name);
		loadAsset(TEXTURE, name);
		return ret;
	}

	public Material requireMaterial(String name) {
		check(name);
		Material ret = (Material) cache.get(name);
		loadAsset(MATERIAL, name);
		return ret;
	}

}