#include frex:shaders/api/header.glsl
#include canvas:shaders/internal/flags.glsl
#include frex:shaders/api/material.glsl
#include frex:shaders/api/fragment.glsl
#include frex:shaders/api/sampler.glsl
#include canvas:shaders/internal/program.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/material_main.frag
******************************************************/

void _cv_startFragment() {
	int cv_programId = _cv_fragmentProgramId();

#include canvas:startfragment
}

void main() {
#ifndef PROGRAM_BY_UNIFORM
	if (_cv_programDiscard()) {
		discard;
	}
#endif
	frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);

#ifdef _CV_FRAGMENT_COMPAT
	compatData = frx_FragmentData(frx_sampleColor, frx_vertexColor);
#endif

#ifdef DEBUG_LIGHTING
	frx_fragColor = vec4(1.0, 1.0, 1.0, frx_sampleColor.a) * frx_vertexColor;
#else
	if (_cvu_context[_CV_TARGET_INDEX] == 1) {
		frx_fragColor = vec4(1.0, 1.0, 1.0, frx_sampleColor.a) * frx_vertexColor;
	} else {
		frx_fragColor = frx_sampleColor * frx_vertexColor;
	}
#endif

	frx_fragEmissive = frx_matEmissive;
	frx_fragLight = frx_vertexLight;
	frx_fragEnableAo = frx_matDisableAo == 0;
	frx_fragEnableDiffuse = frx_matDisableDiffuse == 0;

#ifdef PBR_ENABLED
	frx_fragReflectance = 0.04;
	frx_fragNormal = vec3(0.0, 0.0, 1.0);
	frx_fragHeight = 0.0;
	frx_fragRoughness = 1.0;
	frx_fragAo = 1.0;
#endif
	
	_cv_startFragment();

	if (frx_fragColor.a <= _cv_cutoutThreshold()) {
		discard;
	}

	frx_pipelineFragment();
}
