package com.valeriodipaolo.platformgame.core3d;

import static com.valeriodipaolo.platformgame.core3d.Gfx3D.*;

/** Shader OpenGL ES 2.0 del gioco: li usa il telefono (GlRenderer) e lo strumento da PC che riproduce i fotogrammi con la stessa grafica. */
public final class GlShaders {
    private GlShaders() {}

    /** Lato della mappa delle ombre (pixel), mezza ampiezza dell'area coperta e mezza profondità (metri). */
    public static final int SHADOW_SIZE = 2048;
    public static final float SHADOW_RADIUS = 28f, SHADOW_DEPTH = 50f, SHADOW_AHEAD = 13f;

    static final String HP = "#ifdef GL_FRAGMENT_PRECISION_HIGH\n#define HP highp\n#else\n#define HP mediump\n#endif\n";

    /** Fili d'erba e foglie che ondeggiano (uguale nella scena e nelle ombre). */
    static final String SWAY =
            "  float gust = 0.55 + 0.45 * sin(uTime * 0.45 + aPos.z * 0.11);\n"   // raffiche che corrono lungo il corridoio
            + "  float sw = aSway * gust;\n"
            + "  p.x += sw * 0.16 * (sin(uTime * 1.9 + aPos.z * 0.8 + aPos.x * 0.5) * 0.5 + 0.8);\n"
            + "  p.z += sw * 0.06 * cos(uTime * 1.4 + aPos.x * 0.7);\n";

    public static final String VERTEX =
            "uniform mat4 uMVP;\n"
            + "uniform mat4 uMV;\n"
            + "uniform vec4 uTint;\n"
            + "uniform vec3 uFog;\n"                       // inizio, 1/ampiezza, massimo
            + "uniform float uTime;\n"
            + "attribute float aSway;\n"
            + "attribute vec3 aPos;\n"
            + "attribute vec3 aNormal;\n"
            + "attribute vec2 aUV;\n"
            + "attribute vec3 aColor;\n"
            + "varying vec2 vUV;\n"
            + "varying vec3 vCol;\n"
            + "varying vec3 vN;\n"
            + "varying vec3 vP;\n"
            + "varying float vFog;\n"
            + "uniform mat4 uLightMVP;\n"
            + "varying highp vec3 vSh;\n"
            + "void main() {\n"
            + "  vec3 p = aPos;\n"
            + SWAY
            + "  vec4 vp = uMV * vec4(p, 1.0);\n"
            + "  vP = vp.xyz;\n"
            + "  vN = (uMV * vec4(aNormal, 0.0)).xyz;\n"
            + "  vCol = aColor * uTint.rgb;\n"
            + "  vFog = clamp((-vp.z - uFog.x) * uFog.y, 0.0, uFog.z);\n"
            + "  vUV = aUV;\n"
            + "  vSh = (uLightMVP * vec4(p + aNormal * 0.06, 1.0)).xyz * 0.5 + 0.5;\n"   // spostato lungo la normale: niente acne
            + "  gl_Position = uMVP * vec4(p, 1.0);\n"
            + "}\n";

    // Le texture arrivano da Android con il colore già moltiplicato per la trasparenza.
    // Oggetti opachi: luce in spazio lineare (sole, cielo, terra, riflesso, bordo luminoso), foschia, tono filmico.
    public static final String FRAGMENT =
            HP
            + "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform sampler2D uShadow;\n"
            + "uniform float uShadowOn;\n"
            + "varying HP vec3 vSh;\n"
            + "varying HP vec2 vUV;\n"                     // coordinate texture precise: niente strisce lontano dall'inizio
            + "uniform vec3 uFogColor;\n"
            + "uniform vec3 uLightV;\n"                     // verso il sole, nello spazio della camera
            + "uniform vec3 uUpV;\n"                        // l'alto, nello spazio della camera
            + "uniform vec2 uSpec;\n"                       // forza e durezza del riflesso
            + "uniform float uOpaque;\n"
            + "uniform float uCut;\n"
            + "uniform float uEmissive;\n"
            + "uniform float uAlpha;\n"
            + "uniform vec3 uGrade;\n"
            + "varying vec3 vCol;\n"
            + "varying vec3 vN;\n"
            + "varying vec3 vP;\n"
            + "varying float vFog;\n"
            + "HP float depthAt(HP vec2 uv) { HP vec2 e = texture2D(uShadow, uv).rg; return e.x + e.y / 255.0; }\n"
            // quattro confronti mescolati come un filtro bilineare: bordo morbido, niente scalini
            + "float sunLight(float nl) {\n"
            + "  if (uShadowOn < 0.5) return 1.0;\n"
            + "  HP vec2 st = vSh.xy * " + SHADOW_SIZE + ".0 - 0.5;\n"
            + "  HP vec2 b = floor(st);\n"
            + "  vec2 f = st - b;\n"
            + "  b = (b + 0.5) / " + SHADOW_SIZE + ".0;\n"
            + "  HP float o = 1.0 / " + SHADOW_SIZE + ".0;\n"
            + "  HP float d = vSh.z - 0.0006 - 0.0016 * (1.0 - nl);\n"
            + "  float s00 = step(d, depthAt(b)), s10 = step(d, depthAt(b + vec2(o, 0.0)));\n"
            + "  float s01 = step(d, depthAt(b + vec2(0.0, o))), s11 = step(d, depthAt(b + vec2(o, o)));\n"
            + "  float s = mix(mix(s00, s10, f.x), mix(s01, s11, f.x), f.y);\n"
            + "  vec2 q = abs(vSh.xy * 2.0 - 1.0);\n"
            + "  return mix(s, 1.0, smoothstep(0.8, 1.0, max(max(q.x, q.y), abs(vSh.z * 2.0 - 1.0))));\n"
            + "}\n"
            + "void main() {\n"
            + "  vec4 t = texture2D(uTex, vUV);\n"
            + "  vec3 n = normalize(vN);\n"
            + "  if (uOpaque < 0.5) {\n"                     // trasparenze semplici: ombre, anelli, bandiera
            + "    float l = mix(0.6 + 0.4 * max(dot(n, uLightV), 0.0), 1.0, uEmissive);\n"
            + "    float a = t.a * uAlpha;\n"
            + "    if (a < 0.01) discard;\n"
            + "    vec3 rgb = t.rgb * vCol * l * uAlpha;\n"
            + "    gl_FragColor = vec4(mix(rgb, uFogColor * a, vFog), a);\n"
            + "    return;\n"
            + "  }\n"
            + "  if (uCut > 0.5 && t.a < 0.45) discard;\n"
            + "  vec3 tex = t.a > 0.001 ? t.rgb / t.a : t.rgb;\n"
            + "  vec3 srgb = min(tex * vCol, vec3(1.0));\n"
            + "  vec3 alb = srgb * srgb;\n"
            + "  vec3 v = -normalize(vP);\n"
            + "  float nl = max(dot(n, uLightV), 0.0);\n"
            + "  float hemi = dot(n, uUpV) * 0.5 + 0.5;\n"
            + "  vec3 amb = mix(vec3(" + GND_R + ", " + GND_G + ", " + GND_B + "), vec3(" + SKY_R + ", " + SKY_G + ", " + SKY_B + "), hemi);\n"
            + "  vec3 sun = vec3(" + SUN_R + ", " + SUN_G + ", " + SUN_B + ");\n"
            + "  float sh = nl > 0.0 ? sunLight(nl) : 1.0;\n"
            + "  nl *= sh;\n"
            + "  vec3 c = alb * (sun * (nl * 0.85) + amb * 1.6) * uGrade;\n"
            + "  if (uCut > 0.5) c += alb * sun * 0.3 * max(-dot(n, uLightV), 0.0) * sunLight(0.5) * uGrade;\n"   // foglie in controluce
            + "  if (uSpec.x > 0.0) {\n"
            + "    vec3 h = normalize(uLightV + v);\n"
            + "    float fres = pow(1.0 - max(dot(n, v), 0.0), 3.0);\n"
            + "    c += sun * uSpec.x * pow(max(dot(n, h), 0.0), uSpec.y) * nl;\n"
            + "    c += vec3(" + SKY_R + ", " + SKY_G + ", " + SKY_B + ") * fres * uSpec.x * 1.4;\n"
            + "  }\n"
            + "  vec3 fogc = uFogColor * uFogColor;\n"
            + "  c = mix(c, fogc, vFog);\n"
            + "  c = (c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14);\n"
            + "  c = sqrt(clamp(c, 0.0, 1.0));\n"
            + "  c = mix(vec3(dot(c, vec3(0.299, 0.587, 0.114))), c, 1.0);\n"
            + "  c = mix(c, srgb, uEmissive);\n"
            + "  gl_FragColor = vec4(c, 1.0);\n"
            + "}\n";


    // ---- ombre: profondità vista dal sole, scritta in due canali (OpenGL ES 2 non garantisce le texture di profondità)
    public static final String SHADOW_VERTEX =
            "uniform mat4 uMVP;\n"
            + "uniform float uTime;\n"
            + "attribute vec3 aPos;\n"
            + "attribute vec2 aUV;\n"
            + "attribute float aSway;\n"
            + "varying vec2 vUV;\n"
            + "void main() {\n"
            + "  vec3 p = aPos;\n"
            + SWAY
            + "  vUV = aUV;\n"
            + "  gl_Position = uMVP * vec4(p, 1.0);\n"
            + "}\n";

    public static final String SHADOW_FRAGMENT =
            HP
            + "precision HP float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform float uCut;\n"
            + "varying vec2 vUV;\n"
            + "void main() {\n"
            + "  if (uCut > 0.5 && texture2D(uTex, vUV).a < 0.45) discard;\n"
            + "  HP vec2 e = fract(gl_FragCoord.z * vec2(1.0, 255.0));\n"
            + "  e.x -= e.y / 255.0;\n"
            + "  gl_FragColor = vec4(e, 0.0, 1.0);\n"
            + "}\n";

    // ---- dopo la scena: bagliore delle luci forti, correzione colore filmica, vignettatura
    public static final String POST_VERTEX =
            "attribute vec2 aPos;\n"
            + "varying vec2 vUV;\n"
            + "void main() { vUV = aPos * 0.5 + 0.5; gl_Position = vec4(aPos, 0.0, 1.0); }\n";

    /** Rimpicciolisce l'immagine tenendo solo le parti più luminose. */
    public static final String BRIGHT_FRAGMENT =
            "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform vec2 uTexel;\n"
            + "varying vec2 vUV;\n"
            + "void main() {\n"
            + "  vec3 c = (texture2D(uTex, vUV + uTexel * vec2(-1.0, -1.0)).rgb + texture2D(uTex, vUV + uTexel * vec2(1.0, -1.0)).rgb\n"
            + "      + texture2D(uTex, vUV + uTexel * vec2(-1.0, 1.0)).rgb + texture2D(uTex, vUV + uTexel * vec2(1.0, 1.0)).rgb) * 0.25;\n"
            + "  float l = max(c.r, max(c.g, c.b));\n"
            + "  gl_FragColor = vec4(c * smoothstep(0.66, 1.0, l), 1.0);\n"
            + "}\n";

    /** Sfocatura gaussiana in una direzione (5 letture grazie al filtro lineare). */
    public static final String BLUR_FRAGMENT =
            "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform vec2 uTexel;\n"
            + "varying vec2 vUV;\n"
            + "void main() {\n"
            + "  vec3 c = texture2D(uTex, vUV).rgb * 0.2270;\n"
            + "  c += (texture2D(uTex, vUV + uTexel * 1.3846).rgb + texture2D(uTex, vUV - uTexel * 1.3846).rgb) * 0.3162;\n"
            + "  c += (texture2D(uTex, vUV + uTexel * 3.2308).rgb + texture2D(uTex, vUV - uTexel * 3.2308).rgb) * 0.0703;\n"
            + "  gl_FragColor = vec4(c, 1.0);\n"
            + "}\n";

    public static final String COMPOSITE_FRAGMENT =
            "precision mediump float;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform sampler2D uBloom;\n"
            + "uniform vec2 uTexel;\n"
            + "varying vec2 vUV;\n"
            + "void main() {\n"
            // bordi lisciati (FXAA leggero): l'erba e le foglie ritagliate non sfarfallano più a scalini
            + "  vec3 L = vec3(0.299, 0.587, 0.114);\n"
            + "  vec3 cm = texture2D(uTex, vUV).rgb;\n"
            + "  float lnw = dot(texture2D(uTex, vUV + uTexel * vec2(-1.0, 1.0)).rgb, L), lne = dot(texture2D(uTex, vUV + uTexel * vec2(1.0, 1.0)).rgb, L);\n"
            + "  float lsw = dot(texture2D(uTex, vUV + uTexel * vec2(-1.0, -1.0)).rgb, L), lse = dot(texture2D(uTex, vUV + uTexel * vec2(1.0, -1.0)).rgb, L);\n"
            + "  float lm = dot(cm, L);\n"
            + "  float lmin = min(lm, min(min(lnw, lne), min(lsw, lse))), lmax = max(lm, max(max(lnw, lne), max(lsw, lse)));\n"
            + "  vec3 c = cm;\n"
            + "  if (lmax - lmin > max(0.06, lmax * 0.12)) {\n"
            + "    vec2 dir = vec2(-((lnw + lne) - (lsw + lse)), (lnw + lsw) - (lne + lse));\n"
            + "    float red = max((lnw + lne + lsw + lse) * 0.03125, 0.008);\n"
            + "    dir = clamp(dir / (min(abs(dir.x), abs(dir.y)) + red), -4.0, 4.0) * uTexel;\n"
            + "    vec3 a = 0.5 * (texture2D(uTex, vUV - dir * 0.1667).rgb + texture2D(uTex, vUV + dir * 0.1667).rgb);\n"
            + "    vec3 b2 = a * 0.5 + 0.25 * (texture2D(uTex, vUV - dir * 0.5).rgb + texture2D(uTex, vUV + dir * 0.5).rgb);\n"
            + "    float lb = dot(b2, L);\n"
            + "    c = (lb < lmin || lb > lmax) ? a : b2;\n"
            + "  }\n"
            + "  vec3 b = texture2D(uBloom, vUV).rgb;\n"
            + "  c = 1.0 - (1.0 - c) * (1.0 - b * 0.55);\n"                       // bagliore sommato "a schermo": niente bianco bruciato
            + "  float l = dot(c, vec3(0.2126, 0.7152, 0.0722));\n"
            + "  float green = clamp((c.g - max(c.r, c.b)) * 3.0, 0.0, 1.0);\n"
            + "  c = mix(vec3(l), c, 0.86 - 0.2 * green);\n"                         // colori meno da cartone, soprattutto il verde acido
            + "  c = mix(c, c * c * (3.0 - 2.0 * c), 0.35);\n"                       // un po' di contrasto filmico
            + "  c *= mix(vec3(0.94, 0.98, 1.07), vec3(1.03, 1.0, 0.95), smoothstep(0.05, 0.85, l));\n"   // ombre fredde, luci calde
            + "  vec2 q = vUV - 0.5;\n"
            + "  c *= mix(0.68, 1.0, smoothstep(0.82, 0.28, length(q * vec2(1.0, 0.85))));\n"
            + "  gl_FragColor = vec4(c, 1.0);\n"
            + "}\n";

    /**
     * Matrice del sole per le ombre: proiezione parallela centrata davanti alla camera, agganciata alla griglia dei pixel della
     * mappa così le ombre non tremolano quando la camera si muove.
     */
    public static void lightMatrix(float[] view, float[] out) {
        float tx = view[12], ty = view[13], tz = view[14];
        float ex = -(view[0] * tx + view[1] * ty + view[2] * tz);
        float ey = -(view[4] * tx + view[5] * ty + view[6] * tz);
        float ez = -(view[8] * tx + view[9] * ty + view[10] * tz);
        float fx = -view[2], fz = -view[10];
        float fl = (float) Math.sqrt(fx * fx + fz * fz);
        if (fl > 1e-4f) { fx /= fl; fz /= fl; }
        float cx = ex + fx * SHADOW_AHEAD, cy = ey - 2f, cz = ez + fz * SHADOW_AHEAD;
        float dx = -LX, dy = -LY, dz = -LZ;                                    // direzione dei raggi
        float rx = dz, ry = 0, rz = -dx;                                       // destra = raggio x alto
        float rl = (float) Math.sqrt(rx * rx + rz * rz);
        rx /= rl; rz /= rl;
        float ux = ry * dz - rz * dy, uy = rz * dx - rx * dz, uz = rx * dy - ry * dx;   // alto = destra x raggio
        float px = rx * cx + ry * cy + rz * cz, py = ux * cx + uy * cy + uz * cz, pz = dx * cx + dy * cy + dz * cz;
        float texel = 2f * SHADOW_RADIUS / SHADOW_SIZE;
        px = (float) Math.floor(px / texel) * texel;
        py = (float) Math.floor(py / texel) * texel;
        float k = 1f / SHADOW_RADIUS, kd = 1f / SHADOW_DEPTH;
        out[0] = rx * k; out[4] = ry * k; out[8] = rz * k; out[12] = -px * k;
        out[1] = ux * k; out[5] = uy * k; out[9] = uz * k; out[13] = -py * k;
        out[2] = dx * kd; out[6] = dy * kd; out[10] = dz * kd; out[14] = -pz * kd;
        out[3] = 0; out[7] = 0; out[11] = 0; out[15] = 1;
    }

    /** La mesh, messa dove dice model, può gettare ombra dentro l'area coperta dalla matrice del sole? */
    public static boolean inShadowArea(float[] light, float[] model, Mesh m) {
        if (m.radius < 0) return true;
        float x = model[0] * m.cx + model[4] * m.cy + model[8] * m.cz + model[12];
        float y = model[1] * m.cx + model[5] * m.cy + model[9] * m.cz + model[13];
        float z = model[2] * m.cx + model[6] * m.cy + model[10] * m.cz + model[14];
        float sc = Math.max(Math.abs(model[0]) + Math.abs(model[1]) + Math.abs(model[2]), Math.max(Math.abs(model[4]) + Math.abs(model[5]) + Math.abs(model[6]),
                Math.abs(model[8]) + Math.abs(model[9]) + Math.abs(model[10])));
        float r = m.radius * sc / SHADOW_RADIUS;
        float lx = light[0] * x + light[4] * y + light[8] * z + light[12];
        float ly = light[1] * x + light[5] * y + light[9] * z + light[13];
        return lx > -1f - r && lx < 1f + r && ly > -1f - r && ly < 1f + r;
    }
}
