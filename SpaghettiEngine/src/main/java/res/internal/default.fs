# version 120

uniform sampler2D sampler;

varying vec2 tex_coords;
varying vec3 norms;

void main() {
	vec4 colorVec = texture2D(sampler, tex_coords);
	gl_FragColor = vec4(norms, 1);
}
