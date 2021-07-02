package com.spaghetti.assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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

	// Data
	protected final Game game;
	protected HashMap<String, AssetLoader> loaders = new HashMap<>();
	protected ArrayList<SheetEntry> loadQueue = new ArrayList<>();
	protected ArrayList<SheetEntry> unloadQueue = new ArrayList<>();

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
		registerAssetLoader("sound", new SoundLoader());
		registerAssetLoader("material", new MaterialLoader());
		registerAssetLoader("music", new MusicLoader());

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

	// Internal
	protected void internal_fillasset(SheetEntry asset) {
		// Get asset loader
		AssetLoader loader = getAssetLoader(asset.type);
		if (loader == null) {
			throw new RuntimeException("No loader registered for asset type " + asset.type);
		}

		// Attempt to load data
		Object[] data;
		try {
			data = loader.loadAsset(asset);
			// Attempt to setData in order to check for errors
			asset.asset.setData(data);
			if(!asset.asset.isFilled()) {
				throw new IllegalStateException("Asset didn't accept arguments provided by loader");
			}
		} catch (Throwable t0) {
			Logger.error("Couldn't load " + asset.type + " " + asset.name
					+ ", trying to use default version instead", t0);
			try {
				data = loader.provideDefault(asset);
				// Doing another check
				asset.asset.setData(data);
				if(!asset.asset.isFilled()) {
					throw new IllegalStateException("Attempt to use default version of asset type " +
							asset.type + " failed for " + asset.name);
				}
			} catch (Throwable t1) {
				throw new RuntimeException("Every attempt to load " + asset.type + " " + asset.name + " failed", t1);
			}
		}
	}
	
	protected void internal_nativeload(SheetEntry asset) {
		asset.asset.load();
		asset.loading = false;
	}
	
	protected void internal_nativeunload(SheetEntry asset) {
		asset.asset.unload();
		asset.unloading = false;
	}
	
	protected void internal_nativereset(SheetEntry asset) {
		asset.asset.reset();
		asset.resetting = false;
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
				entry.asset.setName(entry.name);
			}

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

	// 4 different loading methods + 1 method that automatically determines the best method
	public void loadAssetQueue(String name) {
		Logger.loading("[loadAssetQueue] Got request to queue assed loading of " + name);
		if(!checkAsset(name)) {
			return;
		}
		Logger.loading("[loadAssetQueue] Ready to process asset " + name);
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isLoaded() || asset.loading || loadQueue.contains(asset)) {
			Logger.loading("[loadAssetQueue] Asset loading or already loaded (or already in queue)" + name);
			return;
		}
		Logger.loading("[loadAssetQueue] Asset passed all tests, checking for dependencies " + name);
		
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load with same method
		if (dependencies != null && dependencies.length != 0) {
			for (String assetName : dependencies) {
				Logger.loading("[loadAssetQueue] Queueing dependency " + assetName + " of " + name);
				loadAssetQueue(assetName);
			}
		}
		
		// Add the asset to the loading queue
		synchronized(loadQueue) {
			loadQueue.add(asset);
		}
		Logger.loading("[loadAssetQueue] Added to queue asset " + name);
	}
	
	public void loadAssetLazy(String name) {
		Logger.loading("[loadAssetLazy] Got request to lazy load asset " + name);
		if(!checkAsset(name)) {
			return;
		}
		Logger.loading("[loadAssetLazy] Ready to process asset " + name);
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isLoaded() || asset.loading) {
			Logger.loading("[loadAssetLazy] Asset loading or already loaded " + name);
			return;
		}
		Logger.loading("[loadAssetLazy] Asset passed already-loading test " + name);
		
		// The asset is busy but not loading, forward it to the queue function
		if(asset.isBusy()) {
			Logger.loading("[loadAssetLazy] Asset is otherwise busy, forwarding it to loadAssetQueue" + name);
			loadAssetQueue(name);
			return;
		}
		Logger.loading("[loadAssetLazy] Asset passed all tests, checking for dependencies " + name);
		
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load with same method and re-queue this asset for later
		if (dependencies != null && dependencies.length != 0) {
			for (String assetName : dependencies) {
				Logger.loading("[loadAssetLazy] Lazy loading dependency " + assetName + " of " + name);
				loadAssetLazy(assetName);
			}
			Logger.loading("[loadAssetLazy] Dependencies were found, aborting lazy load and forwarding asset to loadAssetQueue " + name);
			loadAssetQueue(name);
			return;
		}
		
		// Begin lazy load here
		asset.loading = true;
		Logger.loading("[loadAssetLazy] Loading flag set for asset " + name);
		
		// Initialize load thread
		Thread loadThread = new Thread() {
			@Override
			public void run() {
				if (!ready) {
					return;
				}
				try {
					// Fill asset
					internal_fillasset(asset);
					Logger.loading("[loadAssetLazy] Filled asset " + name);
					
					// Queue native loading
					FunctionDispatcher dispatcher = game.getRendererDispatcher();
					long func = dispatcher.queue(() -> {
						internal_nativeload(asset);
						return null;
					});
					dispatcher.waitReturnValue(func);
					Logger.loading("[loadAssetLazy] Asset loaded " + name);
				} catch(Throwable t) {
					Logger.error("Error loading " + asset.type + " " + asset.name, t);
				}
				// Unregister this thread upon completion
				game.unregisterThread(this);
				Logger.loading("[loadAssetLazy] Unregistered thread for asset " + name);
			}
		};

		// Register thread and start it
		loadThread.setName("LOADER (" + asset.type + ", " + asset.name + ")");
		game.registerThread(loadThread);
		loadThread.start();
		Logger.loading("[loadAssetLazy] Load thread started for asset " + name);
	}
	
	public void loadAssetNow(String name) {
		Logger.loading("[loadAssetNow] Got request to instant load asset " + name);
		if(!checkAsset(name)) {
			return;
		}
		Logger.loading("[loadAssetNow] Ready to process asset " + name);
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isLoaded() || asset.loading) {
			Logger.loading("[loadAssetNow] Asset loading or already loaded " + name);
			return;
		}
		Logger.loading("[loadAssetNow] Asset passed already-loading test " + name);
		
		// The asset is busy doing something else, wait for it to be ready
		while(asset.isBusy()) {
			Utils.sleep(1);
		}
		
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load with same method
		if (dependencies != null && dependencies.length != 0) {
			for (String assetName : dependencies) {
				Logger.loading("[loadAssetNow] Instant loading dependency " + assetName + " of " + name);
				loadAssetNow(assetName);
			}
		}
		
		// Begin loading process
		asset.loading = true;
		Logger.loading("[loadAssetNow] Loading flag set for asset " + name);
		
		try {
			// Fill asset
			internal_fillasset(asset);
			Logger.loading("[loadAssetNow] Filled asset " + name);
			
			// Queue native loading
			FunctionDispatcher dispatcher = game.getRendererDispatcher();
			long func = dispatcher.queue(() -> {
				internal_nativeload(asset);
				return null;
			});
			dispatcher.waitReturnValue(func);
			Logger.loading("[loadAssetNow] Asset loaded " + name);
		} catch(Throwable t) {
			Logger.error("[loadAssetNow] Error loading " + asset.type + " " + asset.name, t);
		}
	}
	
	public void loadAssetDirect(String name) {
		Logger.loading("[loadAssetDirect] Got request to direct load asset " + name);
		if(!checkAsset(name)) {
			return;
		}
		Logger.loading("[loadAssetDirect] Ready to process asset " + name);
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isLoaded() || asset.loading) {
			Logger.loading("[loadAssetDirect] Asset loading or already loaded " + name);
			return;
		}
		Logger.loading("[loadAssetDirect] Asset passed already-loading test " + name);
		
		// The asset is busy doing something else, wait for it to be ready
		while(asset.isBusy()) {
			Utils.sleep(1);
		}
		
		// Retrieve dependencies
		String[] dependencies = null;
		try {
			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
		} catch (Throwable t) {
		}

		// Load with same method
		if (dependencies != null && dependencies.length != 0) {
			for (String assetName : dependencies) {
				Logger.loading("[loadAssetDirect] Direct loading dependency " + assetName + " of " + name);
				loadAssetDirect(assetName);
			}
		}
		
		// Begin loading process
		asset.loading = true;
		Logger.loading("[loadAssetDirect] Loading flag set for asset " + name);
		
		try {
			// Fill asset
			internal_fillasset(asset);
			Logger.loading("[loadAssetDirect] Filled asset " + name);
			
			// Perform native loading directly
			internal_nativeload(asset);
			Logger.loading("[loadAssetDirect] Asset loaded " + name);
		} catch(Throwable t) {
			Logger.error("Error loading " + asset.type + " " + asset.name, t);
		}
	}
	
	public void loadAssetAuto(String name, boolean lazy) {
		if(lazy) {
			loadAssetLazy(name);
		} else {
			if(Thread.currentThread() == game.getRenderer()) {
				loadAssetDirect(name);
			} else {
				loadAssetNow(name);
			}
		}
	}
	
//	public void loadAssetdfsghn(String name, boolean lazy) {
//		if (!checkExists(name) || game.isHeadless()) {
//			return;
//		}
//
//		// Check before loading
//		SheetEntry asset = assets.get(name);
//		if (asset.asset.valid() || asset.busy) {
//			return;
//		}
//		
//		// Retrieve dependencies
//		String[] dependencies = null;
//		try {
//			dependencies = getAssetLoader(asset.type).provideDependencies(asset);
//		} catch (Throwable t) {
//		}
//
//		// Load dependencies
//		if (dependencies != null && dependencies.length != 0) {
//			for (String assetName : dependencies) {
//				loadAsset(assetName, false);
//			}
//		}
//		
//		asset.busy = true;
//		
//		// In lazy mode we have the chance to load asset in a dedicated thread
//		if(lazy) {
//			// Initialize load thread
//			Thread loadThread = new Thread() {
//				@Override
//				public void run() {
//					if (!ready) {
//						return;
//					}
//					try {
//						// Fill asset
//						internal_fillasset(asset);
//						
//						// Queue native loading
//						FunctionDispatcher dispatcher = game.getRendererDispatcher();
//						dispatcher.queue(() -> {
//							internal_nativeload(asset);
//							asset.busy = false;
//							return null;
//						}, true);
//					} catch(Throwable t) {
//						Logger.error("Error loading " + asset.type + " " + asset.name, t);
//					}
//					// Unregister this thread upon completion
//					game.unregisterThread(this);
//				}
//			};
//	
//			// Register thread and start it
//			loadThread.setName(asset.type + " " + asset.name + " LOADER");
//			game.registerThread(loadThread);
//			loadThread.start();
//		} else {
//			// We must load the asset right now
//			
//			// Fill the asset
//			internal_fillasset(asset);
//			
//			// Queue native loading (immediately executed if this is the renderer thread)
//			FunctionDispatcher dispatcher = game.getRendererDispatcher();
//			long func = dispatcher.queue(() -> {
//				internal_nativeload(asset);
//				return null;
//			}, true);
//			
//			// Then wait
//			try {
//				dispatcher.waitReturnValue(func);
//			} catch(Throwable t) {
//				Logger.error("Error loading " + asset.type + " " + asset.name, t);
//			}
//			asset.busy = false;
//		}
//	}

	// Load every asset
	public void loadAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return;
		}

		assets.forEach((name, asset) -> {
			try {
				loadAssetAuto(name, false);
			} catch (Throwable t) {
				Logger.error("Error unloading " + asset.type + " " + asset.name, t);
			}
		});
	}

	// Same as load methods, 4 methods and a generic one
	public void unloadAssetQueue(String name) {
		if(!checkAsset(name)) {
			return;
		}
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(!asset.asset.isUnloaded() || asset.unloading || unloadQueue.contains(asset)) {
			return;
		}
		
		// TODO find if any asset currently loaded depends on this asset, and abort unloading
		
		// Add the asset to the loading queue
		synchronized(unloadQueue) {
			unloadQueue.add(asset);
		}
	}
	
	public void unloadAssetLazy(String name) {
		if(!checkAsset(name)) {
			return;
		}
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isUnloaded() || asset.unloading) {
			return;
		}
		
		// The asset is busy, forward it to the queue function
		if(asset.isBusy()) {
			unloadAssetQueue(name);
			return;
		}
		
		// Begin lazy unload here
		asset.unloading = true;
		
		// Initialize unload thread
		Thread unloadThread = new Thread() {
			@Override
			public void run() {
				if (!ready) {
					return;
				}
				
				try {
					// Queue native unloading
					FunctionDispatcher dispatcher = game.getRendererDispatcher();
					long func = dispatcher.queue(() -> {
						internal_nativeunload(asset);
						return null;
					});
					
					dispatcher.waitReturnValue(func);
				} catch(Throwable t) {
					Logger.error("Error unloading " + asset.type + " " + asset.name, t);
				}
				
				// Unregister this thread upon completion
				game.unregisterThread(this);
			}
		};

		// Register thread and start it
		unloadThread.setName("UNLOADER (" + asset.type + ", " + asset.name + ")");
		game.registerThread(unloadThread);
		unloadThread.start();
		
	}
	
	public void unloadAssetNow(String name) {
		if(!checkAsset(name)) {
			return;
		}
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isUnloaded() || asset.unloading) {
			return;
		}
		
		// The asset is busy, wait for it to be ready
		while(asset.isBusy()) {
			Utils.sleep(1);
		}
		
		// Begin unloading process
		asset.unloading = true;
		
		try {
			// Queue native unloading
			FunctionDispatcher dispatcher = game.getRendererDispatcher();
			long func = dispatcher.queue(() -> {
				internal_nativeunload(asset);
				return null;
			});
			
			dispatcher.waitReturnValue(func);
		} catch(Throwable t) {
			Logger.error("Error unloading " + asset.type + " " + asset.name, t);
		}
	}
	
	public void unloadAssetDirect(String name) {
		if(!checkAsset(name)) {
			return;
		}
		
		// Check if the asset is ready to be processed
		SheetEntry asset = assets.get(name);
		if(asset.asset.isUnloaded() || asset.unloading) {
			return;
		}
		
		// The asset is busy, wait for it to be ready
		while(asset.isBusy()) {
			Utils.sleep(1);
		}
		
		// Begin unloading process
		asset.unloading = true;
		
		try {
			// Perform native unloading directly
			internal_nativeunload(asset);
		} catch(Throwable t) {
			Logger.error("Error unloading " + asset.type + " " + asset.name, t);
		}
	}
	
	public void unloadAssetAuto(String name, boolean lazy) {
		if(lazy) {
			unloadAssetLazy(name);
		} else {
			if(Thread.currentThread() == game.getRenderer()) {
				unloadAssetDirect(name);
			} else {
				unloadAssetNow(name);
			}
		}
	}
	
//	public void unloadAsset(String name, boolean lazy) {
//		if (!checkExists(name) || game.isHeadless()) {
//			return;
//		}
//
//		// Flag asset as being unloaded
//		SheetEntry asset = assets.get(name);
//		if (!asset.asset.valid()) {
//			return;
//		}
//		
//		asset.unloading = true;
//
//		// Queue unload function
//		FunctionDispatcher dispatcher = game.getRendererDispatcher();
//		long func = dispatcher.queue(() -> {
//			asset.asset.unload();
//			asset.busy = false;
//			return null;
//		}, true);
//
//		// Wait if this is not lazy unloading
//		if (!lazy) {
//			dispatcher.waitFor(func);
//		}
//	}

	// Unload every asset
	public void unloadAll(boolean lazy) {
		if (!ready || game.isHeadless()) {
			return;
		}
		boolean deletedOne = assets.size() > 0;
		for (SheetEntry entry : assets.values()) {
			try {
				unloadAssetAuto(entry.name, lazy);
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
		if (!checkAsset(name) || game.isHeadless()) {
			return;
		}

		// Flag asset as being reset
		SheetEntry asset = assets.get(name);
		if (!asset.asset.valid()) {
			return;
		}
//		asset.busy = true;

		// Queue reset function
		FunctionDispatcher dispatcher = game.getRendererDispatcher();
		long func = dispatcher.queue(() -> {
			asset.asset.reset();
//			asset.busy = false;
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

	public void update() throws Throwable {
		if (!ready) {
			return;
		}
		synchronized(loadQueue) {
			Iterator<SheetEntry> loadIterator = loadQueue.iterator();
			while(loadIterator.hasNext()) {
				SheetEntry asset = loadIterator.next();
				loadIterator.remove();
				loadAssetLazy(asset.name);
			}
		}
		
		synchronized(unloadQueue) {
			Iterator<SheetEntry> unloadItertor = unloadQueue.iterator();
			while(unloadItertor.hasNext()) {
				SheetEntry asset = unloadItertor.next();
				unloadItertor.remove();
				unloadAssetLazy(asset.name);
			}
		}
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
	protected boolean checkAsset(String name) {
		if (!ready) {
			Logger.warning("Can't check asset " + name + ", asset manager is not ready");
			return false;
		}
		if (assets.get(name) == null) {
			Logger.warning("Non existant asset requested: " + name);
			return false;
		}
		if(game.isHeadless()) {
			return false;
		}
		return true;
	}

	// Lazy-loading getters

	public Asset custom(String name) {
		if (!checkAsset(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if(!ret.valid()) {
			loadAssetQueue(ret.getName());
		}
		return ret;
	}

	public Model model(String name) {
		return (Model) custom(name);
	}

	public Shader shader(String name) {
		return (Shader) custom(name);
	}

	public ShaderProgram shaderProgram(String name) {
		return (ShaderProgram) custom(name);
	}

	public Texture texture(String name) {
		return (Texture) custom(name);
	}

	public Material material(String name) {
		return (Material) custom(name);
	}

	public Sound sound(String name) {
		return (Sound) custom(name);
	}

	// Instant-loading getters

	public Asset requireCustom(String name) {
		if (!checkAsset(name)) {
			return null;
		}
		SheetEntry asset = assets.get(name);
		Asset ret = asset.asset;
		if (!ret.valid()) {
			loadAssetAuto(name, false);
		}
		return ret;
	}

	public Model requireModel(String name) {
		return (Model) requireCustom(name);
	}

	public Shader requireShader(String name) {
		return (Shader) requireCustom(name);
	}

	public ShaderProgram requireShaderProgram(String name) {
		return (ShaderProgram) requireCustom(name);
	}

	public Texture requireTexture(String name) {
		return (Texture) requireCustom(name);
	}

	public Material requireMaterial(String name) {
		return (Material) requireCustom(name);
	}

	public Sound requireSound(String name) {
		return (Sound) requireCustom(name);
	}

	// Getters
	
	public boolean isAssetBusy(String name) {
		return assets.get(name).isBusy();
	}
	
	public boolean isAssetLoadQueued(String name) {
		return loadQueue.contains(assets.get(name));
	}
	
	public boolean isAssetUnloadQueued(String name) {
		return unloadQueue.contains(assets.get(name));
	}
	
	public Asset getAsset(String name) {
		SheetEntry entry = assets.get(name);
		return entry == null ? null : entry.asset;
	}

	public Game getGame() {
		return game;
	}

}