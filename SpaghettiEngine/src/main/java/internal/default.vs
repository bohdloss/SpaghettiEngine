#version 120

attribute vec3 _vertices;
attribute vec2 _textures;
attribute vec3 _normals;

varying vec3 vertices;
varying vec2 textures;
varying vec3 normals;
varying vec4 position;

uniform mat4 projection;

void main() {
	vertices = _vertices;
	textures = _textures;
	normals = _normals;
	vec4 glPos = projection * vec4(_vertices, 1.0);
	position = glPos;
	gl_Position = glPos;
}