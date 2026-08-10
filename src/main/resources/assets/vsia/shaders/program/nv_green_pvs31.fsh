#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;

out vec4 fragColor;

float random(vec2 st) {
    return fract(
        sin(dot(st, vec2(12.9898, 78.233)))
        * 43758.5453123
    );
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    float luminance = dot(
        color.rgb,
        vec3(0.299, 0.587, 0.114)
    );

    luminance = pow(luminance, 0.25);
    luminance *= 0.8;

    vec3 nv = luminance * vec3(0.25, 0.5, 0.08);

    float grain = random(
        texCoord * OutSize + Time * 60.0
    ) * 0.25;

    float fineGrain = random(
        texCoord * OutSize * 2.0 + Time * 45.0
    ) * 0.15;

    grain += fineGrain;

    float scanLines = sin(
        texCoord.y * OutSize.y * 2.0
    ) * 0.06;

    nv += vec3(grain + scanLines);

    float bloom = smoothstep(
        0.75,
        1.3,
        luminance
    );

    nv = mix(
        nv,
        vec3(0.85, 1.0, 0.85),
        bloom * 0.6
    );

    float overexposure = smoothstep(
        0.4,
        0.9,
        luminance
    );

    nv = mix(
        nv,
        vec3(1.0, 1.0, 0.95),
        overexposure * 1.2
    );

    // DUAL LENS (PVS-31)
    vec2 screenPos = (texCoord - 0.5)
    * vec2(OutSize.x / OutSize.y, 1.0);

    float leftLensDist = length(
        screenPos - vec2(-0.22, 0.0)
    );

    float rightLensDist = length(
        screenPos - vec2(0.22, 0.0)
    );

    float leftVig = 1.0 - smoothstep(
        0.50,
        0.65,
        leftLensDist
    );

    float rightVig = 1.0 - smoothstep(
        0.50,
        0.65,
        rightLensDist
    );

    float vig = max(leftVig, rightVig);

    nv *= vig;

    fragColor = vec4(nv, color.a);
}