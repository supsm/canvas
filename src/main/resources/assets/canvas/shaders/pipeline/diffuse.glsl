#include frex:shaders/api/world.glsl
#include frex:shaders/api/view.glsl
#include canvas:basic_light_config

/******************************************************
  canvas:shaders/pipeline/diffuse.glsl
******************************************************/

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
#ifdef VERTEX_SHADER
	out float pv_diffuse;
#else
	in float pv_diffuse;
#endif
#endif

const vec3 LIGHT1      = vec3 ( 0.104196384,  0.947239857, -0.303116754);
const vec3 LIGHT2      = vec3 (-0.104196384,  0.947239857,  0.303116754);
const vec3 LIGHT2_DARK = vec3 (-0.104196384, -0.947239857,  0.303116754);

const vec3 LIGHT1_GUI  = vec3 (-0.9334392 , 0.26269472, -0.24430016);
const vec3 LIGHT2_GUI  = vec3 (-0.10357137, 0.9766068 ,  0.18844642);

const vec3 LIGHT1_GUI_ENTITY = vec3 ( 0.140028, -0.700140, -0.70014);
const vec3 LIGHT2_GUI_ENTITY = vec3 (-0.196116, -0.980581,  0.00000);
/**
 * Formula mimics vanilla lighting for plane-aligned quads and is vaguely
 * consistent with Phong lighting ambient + diffuse for others.
 */
float p_diffuseBaked(vec3 normal) {
	// in nether underside is lit like top
	vec3 secondaryVec = frx_worldIsSkyDarkened == 1 ? LIGHT2_DARK : LIGHT2;

	float l1 = max(0.0, dot(LIGHT1, normal));
	float l2 = max(0.0, dot(secondaryVec, normal));

	return 0.5 + min(0.5, l1 + l2);
}

// for testing - not a good way to do it
float p_diffuseSky(vec3 normal) {
	float f = dot(frx_skyLightVector, normal);
	f = f > 0.0 ? 0.4 * f : 0.2 * f;
	return 0.6 + frx_skyLightTransitionFactor * f;
}

/**
 * Offers results similar to vanilla in GUI, assumes a fixed transform.
 */
float p_diffuseGui(vec3 normal) {
	vec3 light1 = mix(LIGHT1_GUI, LIGHT1_GUI_ENTITY, float(frx_isEntity));
	vec3 light2 = mix(LIGHT2_GUI, LIGHT2_GUI_ENTITY, float(frx_isEntity));

	normal = normalize(normal);

	float light = 0.4 + 0.6 * clamp(dot(normal.xyz, light1), 0.0, 1.0) + 0.6 * clamp(dot(normal.xyz, light2), 0.0, 1.0);
	return min(light, 1.0);
}

float p_diffuse (vec3 normal) {
	return frx_isGui ? p_diffuseGui(normal) : p_diffuseBaked(normal);
}
