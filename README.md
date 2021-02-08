# SpaghettiEngine

Spaghetti engine is an open source personal project 2D game framework, and its main purpose is wrapping the low level libraries used into a more human friendly programming environment

### Dependencies

Its dependencies are:
- [LWJGL 3.2.3](https://github.com/LWJGL/lwjgl3)
- [JOML 1.9.25](https://github.com/JOML-CI/JOML)

Every dependency is included in the pom.xml file

### What can it do

Spaghetti engine features:
- Asset import system (models, textures, shaders, and custom asset types too)
- Multi-threaded asset manager
- Levels, game components and game objects framework (WIP): no need to worry about weird openGL stuff

And you can do the following with it
- Render 3d models with materials
- Use the input system to control a player
- Use the event system to easily dispatch custom events across the level
- (WIP) Easily manage networking for multiplayer
- (WIP) Run servers
- (WIP) Physics
- (SOON) Play sounds and music

### How to set it up

Requires the following commands installed on your system:
- git
- maven
- jdk-8

First clone this repository to a folder of your choice

```
cd MyFolder
git clone https://github.com/bohdloss/SpaghettiEngine.git
```

Then use maven to build it as follows
```
cd SpaghettiEngine/SpaghettiEngine
mvn clean package
```

This will create a new folder called ```target``` which contains the compiled library as a ```.jar``` file

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

This step is necessary to load the first level

You will have to create a new class that extends Updater and implement 3 methods:
- Initialization code
- Loop code
- Termination code

The code of an example class is:
```java
// Import the parent class
import com.spaghettiengine.core.Updater;

// Extends Updater
public class MyUpdater extends Updater {

  public void initialize0() {
    super.initialize0();
    // Initializing code goes here and not in the constructor
  }
  
  // This code will be executed in a loop whenever possible
  // Delta is the time passed since the last call to this method
  public void loopEvents(double delta) {
    super.loopEvents(delta);
  }

  public void terminate0() {
    super.terminate0();
    // Termination code here
  }

}
```
- The initialize0() function will be called when the thread starts, which happens to be when begin() is called on a Game object
- The loopEvents(double delta) function will be called every frame with a delta parameter, indicating the time passed since the last call
- The terminate0() function will be called when the game quits to dispose of unused resources

### How to create a Level and place a rotating Mesh into it

This is necessary to display your first images on screen

In your initialize0() method:
```java
// Create a new Level
Level myLevel = new Level();

// Attach the level to the game
// getSource() returns a reference to the current Game instance
getSource().attachLevel(myLevel);

/*
* You will need a Camera to render the scene
* The first argument is the level the camera belongs to
* The second argument is the parent component, but since we
* are placing it directly into the level, it has no parent
*/
Camera camera = new Camera(myLevel, (GameComponent) null);

// Attach the camera to the level to signal it is the active one
myLevel.attachCamera(camera);

/*
* Create a mesh to place in the level
* The arguments are the same except you will have to provide
* a Model and a Material too
*/
Mesh mesh = new Mesh(myLevel, (GameComponent) null, Model.get("apple_model"), Material.get("apple_mat"));
```
This will create a Camera to render your scene and a Mesh to be rendered

To rotate the mesh, in your loopEvents(double delta) method:
```java
// Store the rotation
private double i;

public void loopEvents(double delta) {
  super.loopEvents(delta);

  // Increase it reagrdless of the framerate
  // Remember getSource() returns a reference to the current game object
  i += 0.05 * getSource().getTickMultiplier(delta);

  // Assuming you saved a reference to the level you created
  myLevel.getComponent(Mesh.class).setPitch(i);

}
```
All the following code can be found in the demo pacakge of this repository (```com.spaghettiengine.demo```)

To understand how to actually import your assets read the next example

### Import assets

Spaghetti engine offers a dynamic asset import system: all you have to do is specify the assets to import in a ```.txt``` file!

By default, a file called ```main.txt``` will be searched for asset import

Each line in the file indicates a different asset

Lines that start with // will be ignored

Each word in a line, separated by 1 space, indicates the following:
- First word: asset type (choose between - shader / shaderprogram / material / texture / model - or the full class name of your custom asset type)
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
  
5) Model
  - Model location (currently only .obj supported, use triangulate modifier on the model before exporting it in your model editor)
  
This ```.txt``` file type may be referred to Asset Sheet later in the documentation

An example of an asset sheet can be found in the folder ```/res/main.txt``` of this repository
