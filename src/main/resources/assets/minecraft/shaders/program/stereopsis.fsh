#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D LeftSampler;
uniform sampler2D RightSampler;
uniform float XOffset;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
  if (texCoord.x < 0.5) fragColor = texture(RightSampler, vec2(texCoord.x + 0.25 - XOffset, texCoord.y));
  else fragColor = texture(LeftSampler, vec2(texCoord.x - 0.25 + XOffset, texCoord.y));
  float darkSize = oneTexel.x * 64;
  if (darkSize > 1.0) darkSize = 1.0;
  float darkness = texCoord.x - 0.5;
  if (darkness < 0.0) darkness = -darkness;
  darkness -= 0.25;
  if (darkness < 0.0) darkness = -darkness;
  darkness = darkness * 4 - 1 + darkSize;
  if (darkness < 0.0) return;
  darkness /= darkSize;
  fragColor = vec4(fragColor.rgb * (1 - darkness), 1.0);
}
