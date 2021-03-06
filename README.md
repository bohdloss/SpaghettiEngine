# SpaghettiEngine

Spaghetti engine is an open source personal project 2D game framework, and its main purpose is wrapping the low level libraries used into a more human friendly programming environment

### Dependencies

Its dependencies are:
- [LWJGL 3.2.3](https://github.com/LWJGL/lwjgl3)
- [JOML 1.10.1](https://github.com/JOML-CI/JOML)

Every dependency is included in the pom.xml file

### What can it do

Spaghetti engine features:
- Asset import system (models, textures, shaders, and custom asset types too)
- Multithread asset manager and loader
- Levels, game components and game objects framework
- Highly customizable and easy to use networking support

And you can do the following with it
- Render 3d models with materials
- Use the input system to control a player
- Use the event system to easily dispatch custom events across the level and network
- Easily manage networking for multiplayer
- (WIP) Switch between TCP and UDP protocols without changing a line of code
- Run servers
- (WIP) Physics
- Play sounds and music
- (SOON) Microphone input

### How to set it up

Requires the following commands installed on your system:
- javac (Windows: [Website](https://adoptopenjdk.net/releases.html?variant=openjdk8&jvmVariant=hotspot) - Linux: ```sudo apt install openjdk-8-jdk```)
- git (Windows: [Website](https://gitforwindows.org/) - Linux: ```sudo apt install git```)
- mvn (Windows: [Website](https://maven.apache.org/download.cgi) - Linux ```sudo apt install maven```)

First clone this repository to a folder of your choice

```
cd MyFolder
git clone https://github.com/bohdloss/SpaghettiEngine.git
```

Then cd into the project directory
```
cd SpaghettiEngine/SpaghettiEngine
```

Compilation:
- ```mvn clean compile``` To compile only the source files
- ```mvn clean package``` To compile the source files to a jar file
- ```mvn clean compile assembly:single``` To compile the source files to an executable jar file with dependencies included

This will create a new folder called ```target``` which contains the compiled files

Alternatively you can:
- Compile and run directly with the ```make-and-run``` script
- Compile only with the ```make``` script
- Run only with the ```run``` script

# Code examples

### Create a Game instance

First step necessary to do anything else

The code is:
```java
// Initialize an instance
GameBuilder builder = new GameBuilder();
builder.setUpdater(new Updater());
builder.setRenderer(new Renderer());

Game myGame = builder.build();

// Start game threads (will initialize the provided components)
myGame.begin();
```
Where Renderer is already implemented in the core package of the engine, while you will have to implement your own Updater class, and Client can be null

### Implement Updater class

In the near future level management will be made easier and overriding the Updater core optional

You will have to create a new class that extends Updater and implement 3 methods:
- Initialization code
- Loop code
- Termination code

The code of an example class is:
```java
// Extends Updater
public class MyUpdater extends Updater {

  protected void initialize0() {
    super.initialize0();
    // Initializing code goes here and not in the constructor
  }
  
  // This code will be executed in a loop whenever possible
  // Delta is the time passed since the last call to this method
  protected void loopEvents(float delta) {
    super.loopEvents(delta);
  }

  protected void terminate0() {
    super.terminate0();
    // Termination code here
  }

}
```
- The initialize0() function will be called when the thread starts, which happens to be when begin() is called on a Game object
- The loopEvents(float delta) function will be called every frame with a delta parameter, indicating the time passed since the last call
- The terminate0() function will be called when the game quits to dispose of unused resources

### How to create a Level and place a rotating Mesh into it

This lets you display your first images on screen

```java
private Level myLevel;
private Mesh myMesh;

protected void initialize0() {
	// Create a new Level
	myLevel = new Level();
	
	// Attach the level to the game
	// getGame() returns a reference to the current Game instance
	this.getGame().attachLevel(myLevel);
	
	/*
	* You will need a Camera to render the scene
	*/
	Camera camera = new Camera();
	/*
	* Create a mesh to place in the level
	* You will have to provide a Model and a Material too
	*/
	myMesh = new Mesh(Model.get("apple_model"), Material.get("apple_mat"));
	
	// Add our objects to the level
	myLevel.addObject(camera);
	myLevel.addObject(mesh);
	
	// Now the Renderer will use this camera to render the scene
	getGame().attachCamera(camera);
}
```
This will create a Camera to render your scene and a Mesh to be rendered

To rotate the mesh:
```java
// Store the rotation
private float i;

public void loopEvents(float delta) {
	super.loopEvents(delta);
	
	// Take delta into account to be framerate-independent
	i += 0.05 * this.getGame().getTickMultiplier(delta);
	
	// Change the rotation
	myMesh.setPitch(i);

}
```
All the following code can be found in the demo pacakge of this repository (```com.spaghetti.demo```)

To understand how to actually import your assets read the next example

### Import assets

Spaghetti engine offers a dynamic asset import system: all you have to do is specify the assets to import in a ```.txt``` file!

By default, the file ```/res/main.txt``` will be searched for asset import

Each line in the file indicates a different asset

Lines that are empty or start with // will be ignored

Each word in a line, separated by 1 space, indicates the following:
- First word: asset type (choose between - shader / shaderprogram / material / texture / model / sound / music)
- Second word: asset name
- All the other words are arguments

Here is the full documentation for the arguments that are needed for each asset type:

1) Shader
  - Shader location
  - Shader type (vertex / fragment / geometry / tess_control / tess_evaluation)
  
2) ShaderProgram
  - List of names of Shader-assets

3) Material
  - Name of a Texture-asset
  - Name of a ShaderProgram-asset
  
4) Texture
  - Texture location
  - (Optional) Texture filtering mode (linear / nearest)
  
5) Model
  - Model location
  - Operation type (find to search for a specific model in the specified file / unify to merge all models found into one)
  - (Optional) If operation type is 'find' specify the mesh to search for

6) Sound
  - Sound location
  
7) Music
  - Music location
  
The difference between sound and music is that when played music is being streamed, while a sound is fully loaded into memory beforehand.
Use Sound for relatively short audio files and Music for long ones. Please note that streaming from disk might have a performance impact.
  
This ```.txt``` file type may be referred to as Asset Sheet later in the documentation

An example of an asset sheet can be found in the folder ```/res/main.txt``` of this repository
