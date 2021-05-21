package com.spaghetti.assets;

import java.util.HashMap;

import com.spaghetti.audio.*;
import com.spaghetti.core.*;
import com.spaghetti.events.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.render.*;
import com.spaghetti.utils.*;

public class AssetManager {

	// Static data
	protected static final String APPEND = "" + "shader defaultVS /internal/default.vs vertex\n"
			+ "shader defaultFS /internal/default.fs fragment\n" + "shaderprogram defaultSP defaultVS defaultFS\n"
			+ "shader rendererVS /internal/renderer.vs vertex\n" + "shader rendererFS /internal/renderer.fs fragment\n"
			+ "shaderprogram rendererSP rendererVS rendererFS\n" + "model square /internal/square.obj find square\n"
			+ "texture defaultTXT /internal/default.png nearest\n" + "material defaultMAT defaultTXT defaultSP\n";

	// Final values
	protected final Game game;
	protected HashMap<String, AssetLoader> loaders = new HashMap<>();

	// Cache empty assets
	protected HashMap<String, SheetEntry> assets = new HashMap<>();

	// Ready flag
	protected boolean ready;

	public AssetManager(Game game) {
		this.game = game;

		registerAssetLoader("model", new ModelLoader());
		registerAssetLoader("shader", new ShaderLoader());
		registerAssetLoader("shaderprogram", new ShaderProgramLoader());
		registerAssetLoader("texture", new TextureLoader());
		registerAssetLoader("soundbuffer", new SoundBufferLoader());
		registerAssetLoader("material", new MaterialLoader());

		// Register shutdown hook
		game.getEventDispatcher().registerSignalHandler((isClient, issuer, signal) -> {
			if (signal == Signals.SIGSTOP) {
				if (game.isHeadless()) {
					destroy();
				} else {
					game.getRendererDispatcher().quickQueue(() -> {
						destroy();
						return null;
					});
				}
			}
		});
	}

	// Initialization / finalization of this object

	// Load an asset sheet at the specified location
	public void loadAssetSheet(String sheetLocation) {
		try {
			// Load the sheet file
			String sheetSource = ResourceLoader.loadText(sheetLocation);

			// Prepare for parsing
			HashMap<String, SheetEntry> map = new HashMap<>();

			// Split into lines
			sheetSource = APPEND + sheetSource;
			String[] lines = sheetSource.split("\\R");
			for (String line : lines) {
				// Ignore empty lines or comments
				String line_trimmed = line.trim();
				if (line_trimmed.equals("") || line_trimmed.startsWith("//")) {
					continue;
				}

				// Split into space separated tokens
				String[] tokens = line_trimmed.split(" ");

				// Fill entry information
				SheetEntry entry = new SheetEntry(this);
				entry.type = tokens[0];
				entry.name = tokens[1];
				entry.args = new String[tokens.length - 2];
				for (int i = 0; i < entry.args.length; i++) {
					entry.args[i] = tokens[i + 2];
				}

				// Save entry
				map.put(entry.name, entry);
			}

			// Destroy in case an asset sheet is already loaded
			destroy();

			// Finally apply the sheet
			assets.clear();
			assets.putAll(map);

			// Initialize all dummy assets
			for (SheetEntry entry : assets.values()) {
				getAssetLoader(entry.type).initializeAsset(entry);
			}
			Logger.info(game, "Finished instantiating dummy assets");

			// We're ready!
			ready = true;
			Logger.loading(game, "Successfully loaded asset sheet " + sheetLocation);
		} catch (Throwable t) {
			Logger.error(game, "Could not load asset sheet " + sheetLocation, t);
		}
	}

	// Reset this instance to its state upon creation
	public void destroy() {
		if (!ready) {
			return;
		}
		unloadAll(false);
		resetAll(false);
		assets.clear();
	}

	// Load / Unload methods

	// Load a single asset
	public void loadAsset(String name, boolean lazy) {
		if (!checkExists(name) || game.isHeadless()) {
			return;
		}

		// Check before loading
		SheetEntry asset = assets.get(name);
		if (asset.asset.valid()) {
			return;
		}
		asset.queued = true;

		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load dependencies
		if (dependencies != null && dependencies.length != 0) {
			for (String assetName : dependencies) {
				loadAsset(assetName, false);
			}
		}

		// Behave differently if the current thread is the renderer, to avoid deadlocks
		boolean isRenderer = game.getRenderer() == Thread.currentThread();

		// Initialize load thread
		Thread loadThread = new Thread() {
			@Override
			public void run() {
				if (!ready) {
					return;
				}

				// Get asset loader
				AssetLoader loader = getAssetLoader(asset.type);
				if (loader == null) {
					Logger.error("No loader registered for asset type " + asset.type);
				}

				// Attempt to load data
				Object[] data;
				try {
					data = loader.loadAsset(asset);
				} catch (Throwable t0) {
					Logger.error("Couldn't load " + asset.type + " " + asset.name
							+ ", trying to use default version instead", t0);
					try {
						data = loader.provideDefault(asset);
					} catch (Throwable t1) {
						Logger.error("Attempt to use default version of " + asset.type + " " + asset.name + " failed",
								t1);
						return;
					}
				}
				asset.asset.setData(data);

				// If the calling thread is a renderer, don't queue functions or deadlocks will
				// happen
				if (!isRenderer) {
					// Queue native loading to renderer thread
					FunctionDispatcher dispatcher = game.getRendererDispatcher();
					long func = dispatcher.queue(() -> {
						asset.asset.load();
						asset.needLoad = false;
						asset.queued = false;
						return null;
					}, true);

					// Wait if this is not lazy loading
					if (!lazy) {
						dispatcher.waitFor(func);
					}
				}

				// Unregister this thread upon completion
				game.unregisterThread(this);
			}
		};

		// Register thread and start it
		loadThread.setName(asset.type + " " + asset.name + " LOADER");
		game.registerThread(loadThread);
		loadThread.start();

		// Wait if not in lazy mode
		if (!lazy) {
			while (loadThread.isAlive()) {
				Utils.sleep(1);
			}
		}

		// If this is the renderer perform loading
		if (isRenderer) {
			asset.asset.load();
			asset.needLoad = false;
			asset.queued = false;
		}
	}

	// Load every asset
	public void loadAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return;
		}

		assets.forEach((name, asset) -> {
			try {
				loadAsset(name, lazy);
			} catch (Throwable t) {
				Logger.error("Error unloading " + asset.type + " " + asset.name, t);
			}
		});
	}

	// Unload a single asset
	public void unloadAsset(String name, boolean lazy) {
		if (!checkExists(name) || game.isHeadless()) {
			return;
		}

		// Flag asset as being unloaded
		SheetEntry asset = assets.get(name);
		if (!asset.asset.valid()) {
			return;
		}
		asset.queued = true;

		// Queue unload function
		FunctionDispatcher dispatcher = game.getRendererDispatcher();
		long func = dispatcher.queue(() -> {
			asset.asset.unload();
			asset.queued = false;
			return null;
		}, true);

		// Wait if this is not lazy unloading
		if (!lazy) {
			dispatcher.waitFor(func);
		}
	}

	// Unload every asset
	public void unloadAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return;
		}
		boolean deletedOne = assets.size() > 0;
		for (SheetEntry entry : assets.values()) {
			try {
				unloadAsset(entry.name, lazy);
			} catch (Throwable t) {
				Logger.error("Error unloading " + entry.type + " " + entry.name, t);
			}
		}
		if (deletedOne) {
			Logger.info(game, "Finished unloading assets");
		}
	}

	// Clean garbage left behind by a single asset
	public void resetAsset(String name, boolean lazy) {
		if (!checkExists(name) || game.isHeadless()) {
			return;
		}

		// Flag asset as being reset
		SheetEntry asset = assets.get(name);
		if (!asset.asset.valid()) {
			return;
		}
		asset.queued = true;

		// Queue reset function
		FunctionDispatcher dispatcher = game.getRendererDispatcher();
		long func = dispatcher.queue(() -> {
			asset.asset.reset();
			asset.queued = false;
			return null;
		}, true);

		// Wait if this is not a lazy reset
		if (!lazy) {
			dispatcher.waitFor(func);
		}
	}

	// Clean garbage left behind by all assets
	public void resetAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return;
		}
		assets.forEach((str, asset) -> {
			resetAsset(str, lazy);
		});
	}

	// Utility methods

	public String getAssetType(String name) {
		if (!ready) {
			return null;
		}
		return assets.get(name).type;
	}

	public void lazyLoad() throws Throwable {
		if (!ready) {
			return;
		}
		assets.forEach((name, asset) -> {
			if (asset.needLoad && !asset.queued) {
				asset.queued = true;
				loadAsset(name, true);
			}
		});
	}

	public void registerAssetLoader(String assetType, AssetLoader loader) {
		if (loader == null || assetType == null) {
			throw new IllegalArgumentException();
		}
		loaders.put(assetType, loader);
	}

	public void unregisterAssetLoader(String assetType) {
		if (assetType == null) {
			throw new IllegalArgumentException();
		}
		loaders.remove(assetType);
	}

	public AssetLoader getAssetLoader(String assetType) {
		if (assetType == null) {
			throw new IllegalArgumentException();
		}
		return loaders.get(assetType);
	}

	// Internal check
	protected boolean checkExists(String name) {
		if (!ready) {
			Logger.warning("Can't check asset " + name + ", asset manager is not ready");
			return false;
		}
		if (assets.get(name) == null) {
			Logger.warning("Non existant asset requested: " + name);
			return false;
		}
		return true;
	}

	// Lazy-loading getters

	public Asset custom(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public Model model(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Model ret = (Model) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public Shader shader(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Shader ret = (Shader) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public ShaderProgram shaderProgram(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		ShaderProgram ret = (ShaderProgram) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public Texture texture(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Texture ret = (Texture) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public Material material(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Material ret = (Material) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	public SoundBuffer soundBuffer(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		SoundBuffer ret = (SoundBuffer) asset.asset;
		asset.needLoad = true;
		return ret;
	}

	// Instant-loading getters

	public Asset requireCustom(String name) {
		if (!checkExists(name)) {
			return null;
		}
		Asset ret = custom(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Model requireModel(String name) {
		if (!checkExists(name)) {
			return null;
		}
		Model ret = model(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Shader requireShader(String name) {
		if (!checkExists(name)) {
			return null;
		}
		Shader ret = shader(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public ShaderProgram requireShaderProgram(String name) {
		if (!checkExists(name)) {
			return null;
		}
		ShaderProgram ret = shaderProgram(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Texture requireTexture(String name) {
		if (!checkExists(name)) {
			return null;
		}
		Texture ret = texture(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Material requireMaterial(String name) {
		if (!checkExists(name)) {
			return null;
		}
		Material ret = material(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public SoundBuffer requireSoundBuffer(String name) {
		if (!checkExists(name)) {
			return null;
		}
		SoundBuffer ret = soundBuffer(name);
		if (!ret.valid()) {
			loadAsset(name, false);
		}
		return ret;
	}

	public Asset getAsset(String name) {
		SheetEntry entry = assets.get(name);
		return entry == null ? null : entry.asset;
	}

	public Game getGame() {
		return game;
	}

}