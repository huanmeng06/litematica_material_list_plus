# Grayscale disabled-item icon on mc1.21.5

**Audience:** Codex (or any engineer) porting the disabled-row grayscale icon to
newer Minecraft versions.
**Status:** Solved on `mc1.21.5` after v1.7.0. The original v1.7.0 release shipped
this feature as a no-op on 1.21.5 because the old framebuffer binding/readback path
was gone; the release assets were later replaced with the fixed build.
**Goal of this document:** preserve the failed paths, the final working approach,
and the verification checklist so future `1.21.6+` ports do not rediscover the same
GPU pipeline problem from scratch.

> This document records everything already verified against the 1.21.5 mappings so
> you don't have to re-derive it. Every `class_####` / `method_####` below was read
> from the intermediary-mapped 1.21.5 jar with `javap`. The first half is historical
> handoff material from before the fix; the “Resolved implementation” section below is
> the current working shape.

---

## 0. Resolved implementation

The final 1.21.5 solution keeps the same visual contract as older versions:

- render the original vanilla GUI item icon, preserving 3D block model lighting;
- read back the rendered pixels;
- convert pixels to luminance while preserving alpha;
- upload and cache a dynamic texture;
- draw that cached texture for disabled rows.

The key difference is how 1.21.5 redirects `DrawContext.drawItem` to the offscreen
framebuffer. Since `class_276.method_1235(boolean)` / “bind framebuffer” is gone,
LMLP now uses a very narrow framebuffer getter override:

| File | Role |
| --- | --- |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/GrayscaleItemIcon.java` | Creates the 64×64 framebuffer, clears it transparent, renders the vanilla icon, starts async readback, converts to grayscale, uploads dynamic texture. |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/GrayscaleRenderTargetOverride.java` | Holds the temporary framebuffer while the offscreen icon draw is in progress. |
| `src/main/java/io/github/huanmeng06/lmlp/mixin/MinecraftClientFramebufferMixin.java` | Injects `class_310.method_1522()` and returns the temporary framebuffer only while the override is set. |
| `src/main/resources/litematica_material_list_plus.mixins.json` | Registers the framebuffer mixin. |

The critical flow is:

1. `prewarm(value)` starts a build for each unresolved icon key.
2. `GrayscaleItemIcon` creates `class_6367("lmlp grayscale item icon", 64, 64, true)`.
3. It clears color to `0x00000000`; this is what keeps the icon background transparent.
4. It flushes the outer `DrawContext` if one exists, disables scissor, backs up projection,
   and sets `GrayscaleRenderTargetOverride` to the offscreen framebuffer.
5. A fresh `DrawContext` draws `stack` at `(0, 0)` with a 0..16 orthographic projection.
6. A `finally` block always clears the override and restores projection/model-view/scissor.
7. `CommandEncoder.copyTextureToBuffer(...)` reads the color attachment asynchronously.
8. The callback builds a `NativeImage`, Y-flips the tight-packed buffer, converts ARGB
   pixels to grayscale, uploads `NativeImageBackedTexture`, and marks the cache entry ready.
9. `render(...)` returns `false` while pending/failed, so the caller safely draws the normal
   colored icon until the grayscale texture is ready.

Do not broaden the mixin. It should only affect `MinecraftClient.method_1522()` while
`GrayscaleRenderTargetOverride.get()` is non-null. If a future port sees the whole GUI,
world, or screenshots rendering into a 64×64 target, first check that every `set(...)`
has a matching `clear(...)` in `finally`.

Future-port smoke test:

- disabled block item is true grayscale and still 3D;
- disabled flat item is grayscale with no red/blue channel swap;
- wildcard rows (`{wood}` / `{color}`) eventually show grayscale after async prewarm;
- icon edges are transparent, not black boxed;
- scrolling/opening other GUI controls after icon build does not inherit bad scissor or projection state.

## 1. What the feature does (and why it's non-trivial)

For a **disabled** row in the item-id list editor, the item icon is drawn as a
true black/white/gray image (luminance), not a tint. Vanilla item rendering resets
its own shader color before drawing, so a color multiply can only *darken*; and
sampling the item's particle sprite loses the 3D look of block items. So the
working approach on every other version is:

1. Render the item **once, exactly as the GUI draws it**, into an offscreen
   framebuffer (64×64, i.e. 4× the 16px icon so it stays crisp at high GUI scale).
2. Read the framebuffer pixels back to CPU.
3. Convert each pixel to luminance (`0.299 R + 0.587 G + 0.114 B`, keep alpha).
4. Re-upload as a cached `NativeImage`/dynamic texture, keyed by item id.
5. Draw that cached texture in place of the colored icon.

The cache means the expensive path runs once per distinct icon, then it's a plain
textured blit every frame.

---

## 2. Code coordinates

All paths relative to repo root, on branch **`build-mc1.21.5`** (also the `mc1.21.5`
remote branch after the v1.7.0 push).

| File | Role |
| --- | --- |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/GrayscaleItemIcon.java` | The 1.21.5 implementation. Uses a temporary framebuffer override plus async GPU readback. |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/GrayscaleRenderTargetOverride.java` | Temporary offscreen render target holder. |
| `src/main/java/io/github/huanmeng06/lmlp/mixin/MinecraftClientFramebufferMixin.java` | Redirects `MinecraftClient.method_1522()` only during grayscale icon generation. |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/WidgetItemIdStringListEditEntry.java` | Caller. `prewarm(...)` at ~line 116 (row creation, outside render pass). `render(...)` at ~line 257 inside the draw path. |
| `src/main/java/io/github/huanmeng06/lmlp/gui/textlist/ItemIdListIconResolver.java` | Supplies `Display` records (`stack()`, `id()`) and `allIcons(value)` for wildcard rows. Unchanged, reuse as-is. |

**Caller contract (do not change the signatures — the callsite depends on them):**

```java
static void prewarm(String value);                 // build+cache all icons for this row value
static boolean render(class_332 context, class_1799 stack, String cacheKey,
                      int x, int y, int size);      // true = drew grayscale; false = caller draws colored
```

The callsite (line ~257):
```java
if (this.enabled || !GrayscaleItemIcon.render(context, stack, display.id(), drawX, drawY, ITEM_ICON_SIZE)) {
    context.method_51427(stack, drawX, drawY);   // colored fallback
}
```
So returning `false` is always safe — you get the colored icon while the async
readback is still pending, or if the offscreen pass fails.

### Reference implementation (the version that works on 1.20.1–1.21.4)
The full working source is on any other build branch, e.g.:
```
git show dev-newFeature:src/main/java/io/github/huanmeng06/lmlp/gui/textlist/GrayscaleItemIcon.java
```
Its shape: `FB_SIZE=64`, `ICON_SPACE=16`, `Map<String,class_2960> CACHE`, plus
`buildGrayscaleTexture`, `isFullyTransparent`, `toGrayscale`. Steps 3–5 (luminance
conversion, transparency guard, cache/upload) are **version-independent and can be
copied verbatim**. Only step 1–2 (offscreen render + readback) need rewriting for
1.21.5's GPU pipeline.

---

## 3. Why the old approach does not compile/run on 1.21.5

The 1.20.6-era `buildGrayscaleTexture` relies on APIs that **no longer exist** on
1.21.5:

| Old API (≤1.21.4) | 1.21.5 status |
| --- | --- |
| `class_276.method_1235(boolean)` — bind framebuffer as current render target ("beginWrite") | **REMOVED.** `class_276` on 1.21.5 has *no* bind/target method at all (verified: full method list is `method_1234` resize, `method_1238` delete, `method_30277`/`method_30278` get color/depth `GpuTexture`, `method_68445`, `method_1231`, `method_1237`, `method_29329`, `method_58226`). |
| `class_6367(int,int,boolean,boolean)` ctor | Signature changed to `class_6367(String name, int w, int h, boolean useDepth)`. |
| `RenderSystem.applyModelViewMatrix()` | **REMOVED** (already handled during the 1.21.4 port). |
| `RenderSystem.backup/restore/setProjectionMatrix(..., class_8251)` | Matrix stack still exists but projection/output plumbing changed; not the blocker. |
| `RenderSystem.bindTexture(GpuTexture)` + `class_1011.method_4327(int,boolean)` (glReadPixels-style sync download from bound texture) | The synchronous "bind texture, read its pixels now" path is gone. Readback is now via `CommandEncoder.copyTextureToBuffer(...)` with an async `Runnable` callback. |
| `GlStateManager._disable/enableScissorTest()` | GL scissor no longer globally toggled this way; scissor is per-RenderPass. |

### The core architectural blocker
On 1.21.5, **there is no public API to redirect immediate GUI rendering
(`class_332` / DrawContext) to an arbitrary offscreen framebuffer.**

Traced call chain (all verified via `javap -c`):
- `class_332.method_51427(stack,x,y)` → `method_51424` → `method_51425`. Item draw is
  **still immediate-mode**: it builds a `class_10444` render state, then flushes via
  `method_51452()` → `class_4597$class_4598.method_22993()` (VertexConsumers.Immediate.draw).
- `Immediate.draw` → per-`class_1921` `method_22994` → `class_1921.method_60895(class_9801)`
  which is **abstract**, implemented by the concrete RenderLayer (MultiPhase).
- The color render target is resolved *inside* the RenderLayer's `OutputTarget`
  phase (`class_4668$class_5939` / `$class_4683`), which ultimately targets the
  main framebuffer (`class_310.method_1522()`, field `field_1689`, **final**).
- `RenderSystem` (1.21.5) exposes **no** `outputColorTexture` / `setRenderTarget` /
  framebuffer-override method (verified against the full static-method list).

So unlike ≤1.21.4, you cannot "bind my 64×64 framebuffer, call drawItem, read it
back." The draw always goes to the main framebuffer.

---

## 4. The readback primitive DOES exist (proven by vanilla)

`net.minecraft.class_318` (ScreenshotRecorder) `method_1663(class_276, Consumer<class_1011>)`
does exactly a GPU-texture → `NativeImage` readback on 1.21.5. Decompiled facts you
can copy:

**Allocate the pack buffer:**
```
GpuDevice dev = RenderSystem.getDevice();
GpuTexture tex = framebuffer.method_30277();            // color attachment
int bytes = width * height * tex.getFormat().pixelSize();
GpuBuffer buf = dev.createBuffer(() -> "lmlp gray readback",
        BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, bytes);
CommandEncoder enc = dev.createCommandEncoder();
enc.copyTextureToBuffer(tex, buf, 0, /*Runnable callback*/ () -> { ... }, 0);
```

**Inside the callback — build the NativeImage (verified from `class_318.method_68156`):**
```
try (GpuBuffer.ReadView view = enc.readBuffer(buf)) {
    ByteBuffer data = view.data();
    class_1011 img = new class_1011(width, height, false);
    int pixelSize = tex.getFormat().pixelSize();
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int argb = data.getInt((x + y * width) * pixelSize);   // TIGHTLY packed, NO row padding
            img.method_4305(x, height - y - 1, argb | 0xFF000000);  // Y-FLIP; force opaque alpha
        }
    }
    // ... consume img
}
buf.close();
```

**Critical, vanilla-confirmed details** (these are the ones that usually go wrong —
vanilla already got them right, so match it exactly):
- Buffer is **tightly packed**: index is `(x + y*width) * pixelSize`, i.e. **no 256-byte
  row alignment / stride**. Do not add padding math.
- **Y is flipped**: framebuffer row 0 is the bottom; write to `height - y - 1`.
- Pixel is read with `ByteBuffer.getInt(...)` and written straight to
  `class_1011.method_4305` (private setter, same byte order as `getInt`), OR'd with
  `0xFF000000` to force opaque. `method_4305` is the raw setter; `method_4315` is the
  raw getter (both public on 1.21.5 — used by the ≤1.21.4 luminance code too).
- Byte order / channel order: vanilla treats the int as-is and only forces alpha.
  For grayscale you compute luminance from whatever R/G/B the int yields — **verify
  in-game that channels aren't swapped** (the 1.21.4 port already had to swap R/B
  offsets because pixel methods went ARGB; recheck on 1.21.5).

---

## 5. Candidate implementation paths considered

This section is historical. The implemented solution is the narrow
`MinecraftClient.method_1522()` framebuffer getter override described in Section 0.

### Path A — offscreen RenderPass (most faithful, hardest)
Bypass `DrawContext`'s implicit target by creating your **own** `RenderPass` bound to
your 64×64 texture, and issue the item geometry into it.

- Create texture: `dev.createTexture("lmlp_gray", TextureFormat.RGBA8, 64, 64, 1)`.
- Open a render pass: `enc.createRenderPass(...)` targeting that texture (confirm the
  exact `createRenderPass` overload + clear args on 1.21.5 — not yet transcribed here).
- The hard part: item rendering wants the immediate `VertexConsumerProvider` + the GUI
  item render-state pipeline (`class_10442.method_65598` builds a `class_10444`, then
  `method_65604` draws it). You must get that geometry to flush **into your pass**, not
  the main framebuffer. Options: (a) a Mixin/accessor that lets you pass a target, or
  (b) drive `class_10442`/`class_10444` directly with your own pass + projection.
- Then `copyTextureToBuffer` from your texture → grayscale (Section 4).
- **Risk:** high. Reproducing lighting/enchant-glint/model transforms pixel-identically
  is the whole point; getting it wrong means "close but not the same," which fails the
  goal. Needs in-game iteration.

### Path B — draw to main framebuffer offscreen-in-space, then read back a region
Draw the item to a scratch location on the real framebuffer *before* the frame is
presented, `copyTextureToBuffer` just that 64×64 region (the 9-arg
`copyTextureToBuffer(tex, buf, mip, callback, x, y, w, h, ...)` overload exists), then
grayscale + cache, then it never draws colored there again (cache hit).
- **Risk:** the captured pixels include whatever was already on the framebuffer behind
  the icon (not transparent), and timing/flicker is tricky. Likely visible artifacts.

### Path C (fallback, original v1.7.0 behavior) — keep the colored icon on 1.21.5
This was shipped in the original v1.7.0 assets. It was safe, but did not meet the
requested effect. The v1.7.0 release assets were later replaced with the fixed jars.

**Current recommendation for future ports:** first try to keep the Section 0
framebuffer getter override. Only fall back to a custom RenderPass if future
Minecraft versions stop resolving item GUI output via `MinecraftClient.method_1522()`.

### Async prewarm model (fits the existing caller)
Readback is async (callback), but `prewarm()` is already called at row creation,
outside the render pass. So:
1. `prewarm(value)` → for each uncached icon, kick off render-to-texture +
   `copyTextureToBuffer`; in the callback, build the grayscale `class_1043` and put it
   in `CACHE`.
2. `render(...)` → if `CACHE` has a ready texture, blit it (return `true`); if not yet
   ready (callback pending) return `false` → colored icon shows for the frame(s) until
   the grayscale is cached, then it swaps in. This is an acceptable "grayscale appears
   a frame late" behavior and avoids any mid-frame framebuffer juggling.

---

## 6. Version-independent code you can copy verbatim

From the reference `GrayscaleItemIcon` (works on all other versions). Only the byte
offsets in `toGrayscale` may need adjustment if 1.21.5 channel order differs (verify
in-game):

```java
private static void toGrayscale(class_1011 image) {
    int width = image.method_4307();
    int height = image.method_4323();
    for (int py = 0; py < height; py++) {
        for (int px = 0; px < width; px++) {
            int color = image.method_4315(px, py);
            int r = color & 0xFF;
            int g = (color >>> 8) & 0xFF;
            int b = (color >>> 16) & 0xFF;
            int a = (color >>> 24) & 0xFF;
            int lum = Math.round(0.299F * r + 0.587F * g + 0.114F * b);
            image.method_4305(px, py, (a << 24) | (lum << 16) | (lum << 8) | lum);
        }
    }
}
// isFullyTransparent: same double loop, return false if any (px>>>24)!=0.
// Cache + upload: mc.method_1531().method_4617("lmlp_gray_icon", new class_1043(image));
// Draw cached: context.method_25293(textureId, x, y, size, size, 0,0, 64,64, 64,64);
```

`method_4617` returns a `class_2960` you cache in `Map<String,class_2960> CACHE`,
keyed by `display.id()`. Guard the map for the async case (a pending entry vs a
ready entry vs a known-null failure).

---

## 7. Build & verify

- **Local gradle.properties is gitignored and NOT per-branch.** Before building
  1.21.5 you must ensure `gradle.properties` holds the 1.21.5 values (this repo
  switches it per target). 1.21.5 deps:
  ```
  minecraft_version=1.21.5   java_release=21
  fabric_version=0.128.2+1.21.5   litematica_version=0.22.5   malilib_version=0.24.3
  rei_version=19.0.809   cloth_config_version=18.0.145   architectury_version=16.1.4
  modmenu_version=14.0.2
  mod_version=1.7.0+mc1.21.5   (bump as needed)
  ```
- Build: `JAVA_HOME=<jdk21> ./gradlew build` → jar at
  `build/libs/litematica_material_list_plus-1.7.0+mc1.21.5.jar`. Expect bytecode
  major version **65**.
- **In-game verification (mandatory — cannot be done headless):**
  1. Open the item-id list editor, disable a row with a **block item** (e.g. stone) —
     confirm the icon is true grayscale AND keeps its 3D block look (not a flat tint,
     not the particle sprite).
  2. Disable a row with a flat item (e.g. stick) — confirm luminance is correct, no
     channel swap (grayscale, not tinted red/blue).
  3. Check a **wildcard** row (`{wood}`/`{color}`) cycles through several grayscale
     icons (prewarm builds all via `ItemIdListIconResolver.allIcons`).
  4. Confirm **no flicker** on the rest of the GUI and no leftover scissor/viewport
     state corrupting other widgets after the offscreen pass.
  5. Confirm transparent background (icon edges antialiased against nothing, not black).

---

## 8. Ground rules from the original request
- The rest of the UI must look **identical** to the other versions — no layout or
  behavior change is acceptable just to enable this.
- If the effect can't be made pixel-faithful, **keep Path C** (colored fallback)
  rather than shipping a degraded/tinted approximation.
- `render()` returning `false` is always safe; never let an exception escape into the
  GUI frame — wrap the offscreen work and return `false` on any failure.

## 9. Key mapping quick-reference (1.21.5, intermediary)
```
class_332  DrawContext     .method_51427 drawItem  .method_51452 flush  .method_25293 blit
class_276  Framebuffer     .method_30277 getColorAttachment(GpuTexture)  .method_1522 (on class_310) main FB
class_6367 SimpleFramebuffer  ctor(String,int,int,boolean)
class_1011 NativeImage     .method_4305 setPixel(raw)  .method_4315 getPixel(raw)  .method_4307/4323 w/h
class_1043 NativeImageBackedTexture
class_318  ScreenshotRecorder  .method_1663 readback reference impl
CommandEncoder  .copyTextureToBuffer(tex,buf,mip,Runnable,layer[,x,y,w,h])  .readBuffer→GpuBuffer.ReadView.data()
GpuDevice  .createBuffer(Supplier,BufferType,BufferUsage,int)  .createCommandEncoder()  .createTexture(...)
BufferType.PIXEL_PACK   BufferUsage.STATIC_READ
```
