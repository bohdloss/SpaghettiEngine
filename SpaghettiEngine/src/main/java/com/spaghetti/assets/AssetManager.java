package com.spaghetti.assets;

import static com.spaghetti.assets.AssetType.*;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.spaghetti.core.Game;
import com.spaghetti.events.Signals;
import com.spaghetti.render.*;
import com.spaghetti.utils.*;

public final class AssetManager {

	// Always better keep a reference to game
	protected Game source;

	// Asset sheet links asset names to relative paths
	private AssetSheet sheet;

	// Cache empty assets
	private HashMap<String, Asset> cache = new HashMap<>();
	// Indicates flags associated with an asset
	private HashMap<String, AssetFlag> flags = new HashMap<>();

	// Lambdas' limitations workaround
	private Throwable lambda_error;
	private boolean ready;

	public AssetManager(Game source) {
		this.source = source;
		sheet = new AssetSheet(this);

		// Register shutdown hook
		source.getEventDispatcher().registerSignalHandler((isClient, issuer, signal) -> {
			if (signal == Signals.SIGSTOP) {
				if (source.isHeadless()) {
					destroy();
				} else {
					source.getRendererDispatcher().quickQueue(() -> {
						destroy();
						return null;
					});
				}
			}
		});
	}

	private boolean del1;

	// Destroy every resource currently loaded
	public void deleteAll() {
		if (!ready) {
			return;
		}

		del1 = false;
		cache.forEach((name, asset) -> {
			asset.delete();
			del1 = true;
		});
		if (del1) {
			Logger.info(source, "Finished deleting assets");
		}
	}

	// Destroy every resource currently loaded and collect possible garbage
	public void resetAll() {
		if (!ready) {
			return;
		}

		cache.forEach((str, asset) -> {
			asset.reset();
		});

		System.gc();
	}

	// Remove all dummy resources
	protected void removeDummy() {
		if (!ready) {
			return;
		}

		cache.clear();
		flags.clear();

		ready = false;
	}

	// Reset this instance to its state upon creation
	public synchronized void destroy() {
		if (!ready) {
			return;
		}

		deleteAll();
		resetAll();
		removeDummy();
		sheet.clear();
	}

	// Initialize dummies
	private void initDummy() throws Throwable {

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
		Logger.info(source, "Finished instantiating dummy assets");
	}

	@SuppressWarnings("unchecked")
	private void instantiate(SheetEntry cls) throws Throwable {
		try {
			// Instantiate asset
			Class<? extends Asset> customClass = (Class<? extends Asset>) Class.forName(cls.customType);
			Constructor<? extends Asset> custom = customClass.getConstructor(new Class<?>[0]);
			Asset asset = custom.newInstance(new Object[0]);
			asset.setName(cls.name);

			// Save metadata
			cache.put(cls.name, asset);
			String pkg = customClass.getPackage().getName();
			boolean isCustom = !pkg.endsWith("render");
			flags.put(cls.name, new AssetFlag(isCustom ? AssetType.CUSTOM : customClass.getSimpleName().toLowerCase()));
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

		ready = true;
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

	private synchronized void flagAsset(String name) {
		flagAsset(name, true);
	}

	private synchronized void flagAsset(String name, boolean flag) {
		if (!ready) {
			return;
		}

		AssetFlag aflag = flags.get(name);
		if (flag) {
			if (!cache.get(name).valid()) {
				aflag.needLoad = true;
			}
		} else {
			aflag.needLoad = false;
		}
	}

	public synchronized String getAssetType(String name) {
		if (!ready) {
			return null;
		}

		return flags.get(name).type;
	}

	public synchronized void lazyLoad() throws Throwable {
		if (!ready) {
			return;
		}

		cache.forEach((name, asset) -> {
			AssetFlag flag = flags.get(name);
			if (flag.needLoad && !flag.queued) {
				flag.queued = true;
				loadAsset(name, true);
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
			Asset custom = custom(name);
			AssetLoader.loadCustom(custom, info);
			break;
		}
	}

	public synchronized void loadAsset(String name, boolean lazy) {
		if (!ready) {
			return;
		}

		// Flag asset as being loaded
		AssetFlag flag = flags.get(name);
		flag.queued = true;

		// Queue load function
		long load = source.getRendererDispatcher().queue(() -> {
			try {
				// Loading code
				fillAsset(flag.type, name);
				custom(name).load();
				flagAsset(name, false);
			} catch (Throwable t) {
				// Catch exceptions
				Logger.error("Could not load " + flag.type + " " + name, t);
				source.stopAsync();
			} finally {
				flag.queued = false;
			}
			return null;
		}, true);

		if (!lazy) {
			// On lazy load mode don't wait for the asset to load
			source.getRendererDispatcher().waitFor(load);
		}
	}

	// Lazy-loading getters

	private void check(String name) {
		if (!ready) {
			return;
		}
		if (cache.get(name) == null) {
			throw new NullPointerException("Non existant asset requested: " + name);
		}
	}

	public Asset custom(String name) {
		check(name);
		Asset ret = cache.get(name);
		flagAsset(name);
		return ret;
	}

	public Model model(String name) {
		check(name);
		Model ret = (Model) cache.get(name);
		flagAsset(name);
		return ret;
	}

	public Shader shader(String name) {
		check(name);
		Shader ret = (Shader) cache.get(name);
		flagAsset(name);
		return ret;
	}

	public ShaderProgram shaderProgram(String name) {
		check(name);
		ShaderProgram ret = (ShaderProgram) cache.get(name);
		flagAsset(name);
		return ret;
	}

	public Texture texture(String name) {
		check(name);
		Texture ret = (Texture) cache.get(name);
		flagAsset(name);
		return ret;
	}

	public Material material(String name) {
		check(name);
		Material ret = (Material) cache.get(name);
		flagAsset(name);
		return ret;
	}

	// Instant-loading getters

	public Asset requireCustom(String name) {
		Asset ret = custom(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Model requireModel(String name) {
		Model ret = model(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Shader requireShader(String name) {
		Shader ret = shader(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public ShaderProgram requireShaderProgram(String name) {
		ShaderProgram ret = shaderProgram(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Texture requireTexture(String name) {
		Texture ret = texture(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Material requireMaterial(String name) {
		Material ret = material(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

}