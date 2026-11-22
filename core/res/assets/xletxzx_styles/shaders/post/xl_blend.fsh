#version 330

uniform sampler2D CurSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;

layout(std140) uniform BlendConfig {
    float Keep;
};

out vec4 fragColor;

void main() {
    vec4 cur = texture(CurSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);
    float k = prev.a > 0.5 ? Keep : 0.0;
    fragColor = vec4(mix(cur.rgb, prev.rgb, k), 1.0);
}
