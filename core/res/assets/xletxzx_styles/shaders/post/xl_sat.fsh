#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SatConfig {
    float Saturation;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 o = clamp(vec3(l) + (c - vec3(l)) * Saturation, 0.0, 1.0);
    fragColor = vec4(o, 1.0);
}
