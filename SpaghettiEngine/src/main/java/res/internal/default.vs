#version 120

attribute vec3 vertices;
attribute vec2 textures;
attribute vec3 normals;

varying vec2 tex_coords;
varying vec3 norms;

uniform mat4 projection;

void main() {
	tex_coords = textures;
	gl_Position = projection * vec4(vertices, 1.0);
}
