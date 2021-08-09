# version 120

uniform sampler2D sampler;

varying vec3 vertices;
varying vec2 textures;
varying vec3 normals;
varying vec4 position;

void main() {
	vec4 colorVec = texture2D(sampler, textures);
	if(colorVec.w == 0) {
		discard;
	}
	gl_FragColor = colorVec;
}
