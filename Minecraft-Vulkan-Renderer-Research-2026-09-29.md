# Minecraft Java Edition's Vulkan Renderer & Ecosystem — State of Play

**Research date: 2026-09-29**
Prepared to assess the difficulty of porting an OpenGL-based client rendering mod.

---

## 0. Executive summary

**The single most important correction to common assumptions:** the Vulkan backend did **not** ship in 1.21.9/1.21.10. It landed in the **26.x** version line, and it is already in a **released, stable Minecraft version** — 26.2, released 2026-06-16.

| # | Question | Verdict |
|---|---|---|
| 1 | Which versions ship Vulkan? | **26.2** (2026-06-16) onward, incl. **26.3** (2026-09-15). First in 26.2-snapshot-1 (2026-04-07) |
| 2 | Opt-in or default? | **Opt-in / experimental.** `Default` still == `Prefer OpenGL`. Mutual fallback exists |
| 3 | Replace or coexist? | **Coexist now, replace later.** Mojang states OpenGL will be removed once Vulkan is stable |
| 4 | PostChain / post-processing removed? | **NO — `PostChain`, `RenderTarget`, `TextureTarget` still exist in 26.3.** Post-effect JSON survives |
| 5 | Core shader JSON pipeline? | **Removed, but in 1.21.5** (not 26.x), replaced by in-code `RenderPipeline`. `.vsh`/`.fsh` survive |
| 6 | Is there a cross-API abstraction? | **YES — confirmed.** One shared API, two backends (OpenGL + Vulkan) |
| 7 | Sodium Vulkan port? | **YES — Sodium 0.9.x ships experimental Vulkan support** (2026-06-16) |
| 8 | Iris Vulkan support? | **NO.** Iris works on 26.2/26.3 **only via the OpenGL backend**. A Vulkan shader loader ("Aperture") is announced but unreleased |
| 9 | NeoForge / Fabric? | **Fabric: fully current.** **NeoForge: 26.2 stable, 26.3 still all-beta** |

**Bottom line for a port:** you are not forced to port yet — OpenGL remains the default in 26.2/26.3, and a mod that stays on the Blaze3D/Renderpearl abstraction runs on both backends. But the API surface beneath a rendering mod has been rewritten across 1.21.5 → 26.3, and raw OpenGL is now explicitly unsupported. The precedent set by Sodium is the model to follow: **port onto Mojang's cross-API abstraction, not onto Vulkan directly.**

---

## 1. Version timeline and what shipped when

| Date | Version | Event |
|---|---|---|
| 2026-02-24 | — | Mojang publishes ["Another step towards Vibrant Visuals for Java Edition"](https://www.minecraft.net/en-us/article/another-step-towards-vibrant-visuals-for-java-edition) announcing the OpenGL → Vulkan migration. Vulkan snapshot testing "around summer 2026" |
| 2026-04-07 | **26.2-snapshot-1** | Vulkan renderer added (experimental) + "Graphics API" video option |
| 2026-04-09 | 26.2 Snapshot 2 | Vulkan bug fixes (incl. MC-307311) |
| 2026-06-16 | **26.2 "Chaos Cubed"** | **RELEASE** with experimental Vulkan renderer |
| 2026-09-15 | **26.3 "Wilderness Bound"** | **RELEASE**. Renderpearl split, Shaderc, OIT, SDL |
| 2026-09-22 | 26.4-snapshot-1 | Already in snapshots; no notable renderer changes |

26.2 details: protocol 776, data version 4903, resource pack format 88.0, **minimum Java SE 25**.
26.3 details: protocol 777, data version 5023, resource pack format 97.1.

### What Mojang said officially

The February 2026 article was widely covered (e.g. [Tom's Hardware](https://www.tomshardware.com/video-games/after-17-years-minecraft-java-edition-starts-replacing-opengl-with-a-vulkan-multi-threaded-renderer-update-will-prove-a-boon-for-performance-and-modding), [GIGAZINE](https://gigazine.net/gsc_news/en/20260224-minecraft-opengl-vulkan/)). Key reported points:

- Migration off OpenGL (which is deprecated, especially on macOS) to Vulkan, a multi-threaded API.
- **The transition affects mods that use OpenGL for rendering**; because graphics mods depend heavily on OpenGL, mod creators must migrate.
- Vulkan implementation would be introduced in snapshot testing around summer 2026, tested until stable, with **switching between OpenGL and Vulkan during the testing period**, and **OpenGL removed once Vulkan is thoroughly verified**.
- macOS uses **MoltenVK** to translate Vulkan → Metal.

The Minecraft Wiki reproduces the in-game changelog text for 26.2's Vulkan support:

> - As previously announced, it is intended to switch the game from OpenGL to Vulkan.
> - Vulkan is not supported by older hardware or drivers – OpenGL will be used as a fallback on those cases.
> - The current requirement is **Vulkan 1.2 with dynamic rendering and push descriptors**, but this requirement may increase or decrease over time.
> - Currently an experimental rendering backend, and may not be as performant or stable.
> - Under Vulkan, the player's dedicated graphics card is preferred over any integrated graphics, which is a change from OpenGL.
> - The player can see which backend is being used in the F3 debug overlay (in the `system_specs` section).
> - On macOS, MoltenVK is used to translate Vulkan to Metal.

> **"Vibrant Visuals"** is Mojang's branding for the broader graphics modernisation effort. The Java Edition Vulkan article is titled *"Another step towards Vibrant Visuals for Java Edition"*. **Confirmed:** Vibrancy Visuals is the programme name, not the name of the Vulkan backend. *(I could not fetch minecraft.net article pages directly — the site blocked the fetch tool; I relied on the Minecraft Wiki mirrors, which reproduce the changelog verbatim, plus news coverage.)*

---

## 2. Does Vulkan REPLACE OpenGL, or coexist?

### Confirmed: coexistence, with mutual fallback — for now

26.2 added **Video Settings → "Graphics API"** with three values:

| Setting | Behaviour |
|---|---|
| `Default` | The recommended engine. **Currently identical to "Prefer OpenGL"** |
| `Prefer Vulkan (Experimental)` | Attempts Vulkan, **falls back to OpenGL** if it doesn't work |
| `Prefer OpenGL` | Attempts OpenGL, **falls back to Vulkan** if it doesn't work |

Additional confirmed behaviour:
- The setting **auto-changes if a crash is detected during startup**.
- Crash on `Prefer Vulkan` → setting becomes `Default`.
- Crash on `Default` → setting becomes `Prefer OpenGL`; with `Prefer OpenGL` set, **no interaction with Vulkan occurs** (to avoid Vulkan-induced crashes).
- Under `Default`, the game still gathers Vulkan system info at startup.

### The removal plan

Mojang's stated intent is that **OpenGL will be removed once the Vulkan backend is stable** — confirmed independently by Fabric:

> "26.2 introduces the ability for the backend to be changed between the default OpenGL backend and an experimental Vulkan backend, **with OpenGL planned to be removed once the Vulkan backend is stable**." — [Fabric for Minecraft 26.2](https://fabricmc.net/2026/06/15/262.html)

> "26.1 is also expected to be **the last Minecraft release to solely support OpenGL**, with 26.2 snapshots expected to allow for the backend to be changed between OpenGL and Vulkan" — [Fabric for Minecraft 26.1](https://fabricmc.net/2026/03/14/261.html)

**As of 26.3 (2026-09-15), OpenGL has NOT been removed.** *Unconfirmed:* whether 26.4 or a later version removes it. No date has been announced.

---

## 3. The shader / post-processing system — what actually changed

This is the area most commonly mis-described. **Two separate rewrites happened, in different versions.**

### 3a. The core shader JSON pipeline was removed in **1.21.5** — not in the Vulkan versions

NeoForged's 1.21.4 → 1.21.5 primer, section "Render Pipeline Rework":

> "First, **shader JSONs no longer exist**. This is replaced by a `RenderPipeline`, which is effectively an in-code substitute."

> "Previously, a pipeline was constructed using a JSON that contained all metadata from the vertex and fragment shader to their defined values, samplers, and uniforms. However, this has all been replaced with an in-code solution... This is known as a `RenderPipeline`."

**Critical nuance:** only the JSON *program definitions* died. The `.vsh`/`.fsh` GLSL source files **survive** and are still at `assets/<namespace>/shaders/core/`. The 26.3 primer's own examples use `assets/examplemod/shaders/core/example_oit_supported.vsh`/`.fsh`.

`RenderPipeline.Snippet` is the modern replacement for shared/copy-pasted shader JSON: `builder(snippet...)` extends a reusable partial pipeline.

### 3b. `PostChain` and friends were **NOT** removed

**Confirmed:** `PostChain`, `RenderTarget`, and `TextureTarget` **still exist in 26.3**. They appear in the 26.3 primer's *delta* list (i.e. they existed in 26.2 and were modified in 26.3):

```
- `PostChain`
    - `id` - The identifier of the post effect.
    - `getReferencedExternalTargets` - Gets any referenced targets in the post effect.
    - `closePersistentTargets` - Destroys the buffers for any persistent targets.
- `RenderTarget` now takes in a nullable `GpuFormat` for the depth instead of just a `boolean` ...
    - `useDepth` replaced by `depthFormat` ... `format` -> `colorFormat`
    - `copyColorFrom` - Copies the color texture from another `RenderTarget`.
- `TextureTarget` now takes in a nullable `GpuFormat` for the depth ...
```

Post-effect JSON **survives** under `assets/minecraft/post_effect/`. It was **reformatted in 1.21.5**, not deleted:

```json5
// Before 1.21.5
{ "program": "minecraft:post/box_blur",
  "uniforms": [ { "name": "BlurDir", "values": [ 1.0, 0.0 ] } ] }

// 1.21.5+
{ "vertex_shader":   "minecraft:post/blur",
  "fragment_shader": "minecraft:post/box_blur",
  "uniforms": [
      { "name": "BlurDir", "type": "vec2", "values": [ 1.0, 0.0 ] },  // "type" now REQUIRED
      { "name": "Radius",  "type": "float" }                          // "values" now optional
  ] }
```

The `program` indirection was replaced by explicit per-pass `vertex_shader`/`fragment_shader`; `type` became required; `values` became optional (deferred via `PostChain#process(..., pass -> pass.setUniform("Radius", 0.4f))`).

26.3 post-effect changes: `assets/minecraft/post_effect/transparency.json` removed; new `ServerPlayer#sendPostEffects`/`addPostEffect`/`clearPostEffects`/`getPostEffects`/`removePostEffect`; `Player#postEffects`; `GameRenderState#requestedPostEffects`; `useShaderTransparency` removed; `ShaderManager#isPostEffectValid` + `#getAvailablePostEffects` added.

**Independent corroboration:** the third-party **Vulkan PostFX** mod's README states its post-chains execute "through Minecraft's `ShaderManager` and ... the current `minecraft_postchain` backend", and that "**The current stable execution path is still based on Minecraft's post-chain system.**" — i.e. Mojang's PostChain is alive and is the supported path under Vulkan.

### 3c. The abstraction classes — introduced **1.21.5**, generalised **26.2**, relocated **26.3**

All of `RenderPipeline`, `GpuBuffer`, `GpuTexture`, `GpuDevice`, `CommandEncoder`, `RenderPass` were introduced **together in 1.21.5** under the primer's "Abstracting OpenGL" heading — **not** 1.21.6 and **not** 1.21.9.

- `GpuDevice` — "an interface that acts like a general implementation of a render library like OpenGL or Vulkan"
- `GpuTexture` — "responsible for managing anything to do with creating, writing, and releasing a texture"
- `CommandEncoder` — `writeToTexture`, `copyTextureToTexture`, `clearTexture`, `presentTexture`, `createRenderPass`
- `RenderPass` — `setPipeline`, `bindSampler`, `setUniform`, `setVertexBuffer`, `setIndexBuffer`, `draw*`

**1.21.6–1.21.8** continued the work (RenderTypes/RenderPipelines/RenderStates; 1.21.8 = GUI render states). **1.21.9 did not introduce `RenderPipeline`** — it already existed; that version's work was `OrderedRenderCommandQueue` plus world/entity/particle/block-entity rendering rework.

**26.2 generalised it:**
- `TextureFormat` + `VertexFormatElement` formats → **`GpuFormat`** enum — critically, "their mappings do **not have one-to-one parity for the GL codes previously used**"
- `DestFactor` + `SourceFactor` → **`BlendFactor`** + **`BlendOp`**
- Dynamic `VertexFormat` via `$Builder#addAttribute` (max 16 elements)
- `VertexFormat$Mode` → **`PrimitiveTopology`**; `$IndexType` → **`IndexType`**
- `RenderPipeline` now supports **up to 16 vertex buffers** and **up to 8 `ColorTargetState`s**
- **`BindGroupLayout`** replaces the flat sampler list + `RenderPipeline#UniformDescription`; duplicates throw at shader compile
- `RenderPass` can define a sub-rectangle render area
- New telemetry `TelemetryEventType#GRAPHICS_CAPABILITIES` with `BACKEND_NAME`, `BACKEND_FAILURE_REASON`, `BACKEND_FAILURE_MISSING_CAPABILITIES`

**26.3 relocated everything into "Renderpearl":**

> "The render API that previously lived in Blaze3d has been **split into a separate project called Renderpearl**. All rendering classes that originally lived in Blaze3d have been migrated to `com.mojang.renderpearl`."

| Layer | Purpose |
|---|---|
| `com.mojang.renderpearl.api.*` | Interfaces for elements the game interacts with |
| `com.mojang.renderpearl.frontend.*` | Implemented APIs; "handle most of the validation" |
| `com.mojang.renderpearl.backend.*` | "manage the actual logic to communicate with a graphics library" |

Key moves: `blaze3d.opengl.*` → `renderpearl.backend.opengl.*`; `blaze3d.vulkan.*` → `renderpearl.backend.vulkan.*`; `CommandEncoder` → `frontend.FrontendCommandEncoder`; `GpuDevice` → `frontend.FrontendGpuDevice`; `RenderPass` → `frontend.FrontendRenderPass`; `GpuBuffer`/`GpuTexture` became **interfaces**.

**26.3 also removed** `RenderSystem#outputColorTextureOverride` and `#outputDepthTextureOverride` — output textures must now be specified per-`RenderPass` in try-with-resources.

### 3d. Other 26.3 breaking rendering changes

- **Shaderc** now compiles shaders for **both OpenGL and Vulkan** via `GlslCompiler`.
- `#moj_import` **renamed to `#include`**.
- `in`/`out` uniforms **must** declare `layout(location = N)`.
- New macros: `RENDERPEARL_DEPTH_IS_ZERO_TO_ONE`, `RENDERPEARL_EXPLICIT_DEPTH_INVARIANCE`, `RENDERPEARL_INSTANCE_INDEX_INCLUDES_BASE_INSTANCE`.
- Most vanilla shaders now **require `GL_ARB_separate_shader_objects`** "as a requirement of Vulkan".
- **Order Independent Transparency** (Wavelet OIT, [arXiv 2201.00094](https://arxiv.org/pdf/2201.00094)) via the "improved transparency" option: `OIT_ALPHA_ONLY` define, `minecraft:oit.glsl`, `OitPipelineSet`, `RenderSetup#setOitPipelines`, `PreparedRenderType#drawFromBufferOit`.
- **SDL replaces GLFW** — input scancode values change; `TextInputManager#onTextInputFocusChange` must be called or character input breaks.
- Feature-rendering overhaul in 26.2 "completely replace[d] `MultiBufferSource` and any direct vertex uploads outside of vanilla chunk rendering"; `Font` draw methods removed.

---

## 4. Is there a cross-API abstraction? (the key portability question)

### Confirmed: YES — one shared API, two pluggable backends

**Decisive statement** — Fabric's official docs carry this warning banner:

> **WARNING:** "Use of raw OpenGL is not supported on Minecraft 26.2 as it has released with an optional Vulkan backend. Instead, you must use the **`Blaze3D` abstraction layer, which sits between your code and the rendering backend (either OpenGL or Vulkan)**." — [Fabric Docs, Basic Rendering Concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts)

A mod written against the abstraction **works on both backends**. Mods must **not** target Vulkan directly — and raw OpenGL is explicitly unsupported.

Supporting evidence:

1. **Parallel backend packages, one shared API.** 26.2 primer: "for all classes in `com.mojang.blaze3d.opengl`, there is generally a **parallel found in `com.mojang.blaze3d.vulkan`**." Modder-facing types live in the *shared* namespace, not in either backend package.
2. **26.3 formalised it** into `api`/`frontend`/`backend` with interface + frontend-implementation pairs (`api.commands.CommandEncoder` + `frontend.FrontendCommandEncoder`, etc.) — textbook shared-frontend-over-pluggable-backends.
3. **All modding guidance targets the shared layer** — every example in the 1.21.5/26.2/26.3 primers uses `RenderPipeline`, `RenderSystem.getDevice()`, `RenderPass`, `GpuTexture`, never a Vulkan-specific type.
4. **Unified shader compilation** — Shaderc compiles for both; `renderpearl.backend.opengl.GlPipelineRecompiler` recompiles SPIR-V module into GLSL for the GL path.
5. **Backend choice is a runtime video setting** with auto-fallback — mods aren't involved.

### Where the boundary sits

```
        mod / vanilla game code
                 ▼
  RenderPipeline · GpuDevice · CommandEncoder · RenderPass
  GpuBuffer · GpuTexture · BindGroupLayout · GpuFormat
  BlendFunction · DepthStencilState · PrimitiveTopology
         (26.2: com.mojang.blaze3d.*
      →  26.3: com.mojang.renderpearl.api.* / .frontend.*)
                 ▼
     ┌───────────┴───────────┐
 backend.opengl.*      backend.vulkan.*
 GlDevice,             VulkanDevice,
 GlCommandEncoder,     VulkanCommandEncoder,
 GlRenderPass,         VulkanRenderPass,
 GlTexture, GlBuffer,  VulkanGpuTextureView,
 GlPipelineRecompiler  VulkanFeatureSets
   (SPIR-V → GLSL)
```

**Mods live above `RenderPipeline`/`GpuDevice`; `renderpearl.backend.*` is Mojang-internal. There is NO supported Vulkan-specific mod-facing shader API.**

### Important caveat: Vulkan support RAISES the OpenGL floor

Because one shader source must serve both backends, portability is achieved by *constraining* OpenGL rather than branching per-API. Vanilla shaders now require `GL_ARB_separate_shader_objects` "as a requirement of Vulkan". A mod that relied on old-GL flexibility may find its minimum spec raised even while running on OpenGL.

---

## 5. Sodium and Iris

### Sodium — has a Vulkan port

**Sodium 0.9.0 for Minecraft 26.2**, released **2026-06-16**, official release notes:

> "This is the first version to ***experimentally* support Vulkan**. To access it, use the Graphics API option in video settings.
> - Updated to Minecraft 26.2
> - **Moved all rendering to Mojang's Blaze3D API**
> - Added Asynchronous Graph Culling and Frame-Independent Task Scheduling"

Sodium's Modrinth page:

> "**Sodium 0.9.x is now available for Minecraft 26.2, *with early support for Vulkan!*** The performance of the Vulkan backend is already in a good place (even faster than before on many devices), and we expect to be able to improve it even further with future updates."

**This is the blueprint for your port:** Sodium achieved Vulkan support by **moving onto Mojang's Blaze3D abstraction**, not by writing a Vulkan renderer. Release notes through 0.9.3-alpha.1 (2026-09-20) reference `VKIndirectContext.addCommand` and other Vulkan-side internals, confirming a genuine dual-backend implementation.

Version lines confirmed:
- **0.9.x** → Vulkan-era: MC 26.1.2, 26.2, 26.3 (0.9.2 for 26.3, 2026-09-15)
- **0.8.x** → OpenGL line, still maintained: MC 1.21.1 (0.8.13), 1.21.11 (0.8.14/0.8.15-beta.1)
- Sodium 0.9.2 adds FRAPI support on NeoForge

### Iris — supports 26.2/26.3, but **NOT under Vulkan**

**Confirmed from the official Iris 1.11.0 changelog for MC 26.2 (2026-06-16):**

> "Adds support for Minecraft 26.2.
> **Note that Vulkan is not supported.**"

Iris releases exist for 26.2 and 26.3 (Fabric + NeoForge), so Iris **does** run on the Vulkan-era Minecraft versions — but only with the **OpenGL backend selected**.

Release history: Iris 1.11.0 (26.2, 2026-06-16) → 1.11.1 (2026-06-16) → 1.11.2 (2026-07-08, "Finally added NeoForge", Sodium 0.9.1) → 1.11.4 (2026-09-13, "Updates to Sodium 0.9.2", adds a new 'depth transformer') → 1.11.5/1.11.6 (26.3, 2026-09-15).

A relevant transient state: Sodium 0.9.2's release note warned "**Iris is not yet compatible.** If you want to use Iris, don't update Sodium until Iris receives an update." — resolved by Iris 1.11.4 two days later. Expect continued Sodium↔Iris coupling churn on this line.

### CaffeineMC / Iris developer statements

- Sodium issue [#1927 "Compatibility with Vulkan"](https://github.com/CaffeineMC/sodium/issues/1927) (opened 2023): CaffeineMC member `pajicadvance` — "No. Both mods replace Minecraft's renderer but use different backends. Asking to make them 'compatible' is not even a valid request."
- **Updated 2026-06-14:** a user asked "what about 0.9.0 sodium....?" and CaffeineMC contributor **douira** replied: **"Yes 0.9 has support for vulkan."**
- Iris issue [#1515 "Support for Vulkan Mod"](https://github.com/IrisShaders/Iris/issues/1515): Iris lead `IMS212` (2022) — "We do not have any plans to support Vulkan **replacement mods** as of now, as it would require a major rewrite of the Iris pipeline." *(Note: this predates Mojang's own Vulkan backend and refers to third-party Vulkan replacement mods like VulkanMod, so it should not be read as a statement about the current situation.)*

### "Aperture" — an announced Vulkan shader loader (NOT released)

A public Iris issue, [#3279](https://github.com/IrisShaders/Iris/issues/3279) (opened **2026-08-06**), asks:

> "I just wanted to know if you already have an estimate of when the new shaders loader **aperture for vulkan** will be released"

**Status: open, no official answer, no release date.** The only replies are community "+1"s. A community comment in issue #1515 claims (2026-02-19) that, based on the Iris Discord, "Iris development will be halted in favor of their new shader loader called **Aperture**, which will be Vulkan-based".

> ⚠️ **Flagging clearly:** the *existence* of an Iris-ecosystem Vulkan shader loader called Aperture is corroborated by the issue title and community discussion, but **I could NOT confirm any official CaffeineMC/Iris announcement, timeline, feature set, or open-source status**. The "Iris development will be halted" claim is an **unverified third-party Discord summary**, not a developer statement. Treat all Aperture specifics as **speculation**.

---

## 6. Are OpenGL rendering mods portable? Documented obstacles

### The official guidance

Fabric's instruction to mod authors, repeated verbatim across the 26.1 and 26.2 blog posts (i.e. pre-announced in March 2026, three months before 26.2 shipped):

> "**Developers making use of raw OpenGL calls rather than going through the Blaze3D API will need to migrate for their mods to work.**"

Plus the docs warning: "Use of raw OpenGL is not supported on Minecraft 26.2."

### Concrete technical obstacles (all sourced from primers/docs)

| Obstacle | Detail |
|---|---|
| **No raw GL state manager** | `GlStateManager` internals changed; GL constants replaced by object types. Raw GL is unsupported |
| **GL state → objects** | Blend factors, depth tests, cull, polygon mode, logic op are now `BlendFunction`/`DepthStencilState`/`PolygonMode`/`LogicOp` objects, not GL enums |
| **GPU formats ≠ GL codes** | `GpuFormat` "do[es] not have one-to-one parity for the GL codes previously used" |
| **No direct framebuffer/texture IDs** | `RenderTarget#framebufferId` removed; `colorTextureId`/`depthBufferId` → `GpuTexture` objects. Texture override via `RenderSystem#outputColorTextureOverride`/`#outputDepthTextureOverride` **fully removed** in 26.3 — must use try-with-resources `RenderPass` |
| **Command buffers** | Rendering must go through `CommandEncoder`/`RenderPass`; `GlCommandEncoder#executeDrawMultiple` removed; `presentTexture` now needs swapchain width/height |
| **No direct buffer manipulation** | `BufferStorage#mapBuffer` → `GpuBuffer#map`/`GpuBufferSlice#map`; `GlBuffer` reworked; `GpuBuffer$Usage` flag restrictions |
| **Bind groups** | `BindGroupLayout` required; duplicates throw at shader compile |
| **Shader source changes** | `#moj_import` → `#include`; mandatory `layout(location = N)`; must declare `GL_ARB_separate_shader_objects` |
| **Extension/feature gating** | Vulkan requires **1.2 + dynamic rendering + push descriptors**; some hardware/drivers are excluded and fall back to OpenGL. Minecraft enforces these as startup feature checks |
| **Package relocation** | Completely relocated to `com.mojang.renderpearl.*` in 26.3 — any `com.mojang.blaze3d.*` render import must migrate again |
| **Window/input layer** | GLFW → SDL in 26.3 |

### Real-world porting evidence

**Sodium succeeded** — by moving all rendering to Blaze3D (see §5).

**Voxy (distant-horizon-style renderer) has not.** A contributor attempting a 26.2 port ([voxy#548](https://github.com/MCRcortex/voxy/issues/548), 2026-06-17) reported:

> "I played around with Voxy with Minecraft 26.2, Fabric 0.19.3 and Sodium mc26.2-0.9.0-fabric. After fixing all of the incompatible APIs, I eventually got stuck on this bit of code ... crashing the game upon world join: `int oldFB = GL11.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);` ... it does seem logical that now that Minecraft officially supports multiple graphical APIs in the Blaze3D abstraction layer, **OpenGL-specific code fails, even if OpenGL is the actual renderer being used.**"

That last sentence is the single most useful datapoint for your assessment: **raw-GL mods can break on 26.2 even while the OpenGL backend is active**, because the abstraction removed assumptions they relied on.

Voxy's owner MCRcortex, on Vulkan ([voxy#369](https://github.com/MCRcortex/voxy/issues/369), 2026-02-21):

> "know about this, and will have to see what happens in the future, **am not going to make guarantees or promises that may be kept**"

A collaborator in July 2026: "right now there is no confirmation if voxy will support vulkan in the future it might but cortex isnt making any promises."

### Shader-pack obstacle

**Vulkan PostFX** (VPFX, 2026-04-11) is an experimental post-processing framework explicitly targeting "Minecraft's modern Vulkan-based rendering path". Its README independently confirms the architecture ("The current stable execution path is still based on Minecraft's post-chain system") and lists what it does **not** support:

> `gbuffers_*`, full `shadow.*` compatibility, full `composite.*` compatibility, screen-space reflections, volumetric fog, natural cloud rendering, material-aware lighting, dimension-specific shader routing (`world1/`, `world-1/`), direct Iris/OptiFine shader pack compatibility

> "**Vulkan PostFX is not yet a mature drop-in replacement for Iris or OptiFine.** Most Iris / OptiFine shader packs cannot be used directly without porting."

So: as of 2026-09-29, **there is no working way to run existing Iris/OptiFine shader packs on the Vulkan backend.** Shader-pack mods are the least portable category.

---

## 7. Mod loader support

### Fabric — fully current

| MC version | Fabric API | Date | Type |
|---|---|---|---|
| 26.4-snapshot-1 | 0.161.1+26.4 | 2026-09-22 | beta |
| **26.3** | **0.161.0+26.3** | 2026-09-18 | release |
| **26.2** | **0.161.0+26.2** | 2026-09-18 | release |
| 26.2 (first) | 0.158.0+26.2 | 2026-08-18 | release |
| 26.1/26.1.1/26.1.2 | 0.155.3+26.1.2 | 2026-09-07 | release |
| 1.21.11 | 0.141.4+1.21.11 | 2026-05-11 | release |
| 1.21.1 | 0.116.17+1.21.1 | 2026-09-01 | release |

- **Fabric Loader 0.19.5** (2026-08-28) is latest; 0.19.0 (2026-04-07) added enum extensions.
- Fabric API **0.161.0 shipped simultaneously for 26.2 and 26.3** — 26.2 was not abandoned.
- Fabric hardened CI for Vulkan: changelog entry *"Fix client game tests not running with vulkan (#5414)"*.
- Barely any Vulkan-related delay.
- Loom 1.17 / Gradle 9.5.1 (26.2), Gradle 9.6.0 (26.3); **Java 25 minimum since 26.1**.

### NeoForge — stable on 26.2, **lagging on 26.3**

| MC line | Newest NeoForge | Stable? |
|---|---|---|
| **26.3** | `26.3.0.33-beta` | ❌ **NO — every 26.3 build is `-beta`** |
| **26.2** | `26.2.0.88` | ✅ Stable from `26.2.0.57` (32 stable builds) |
| 26.1 | `26.1.2.112` | ✅ Yes |
| 1.21.11 | `21.11.45` | ✅ Yes |
| 1.21.1 | `21.1.252` | ✅ Yes, actively maintained |

A trailing `-beta` means breaking changes are still permitted. **14 days after 26.3's release, NeoForge 26.3 is still entirely beta with no announcement** (Fabric shipped stable 26.3 API in ~3 days). NeoForge publishes **no GitHub Releases** — only git tags; use the [Maven versions API](https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge).

**Confirmed NeoForge + Vulkan defect:** a third-party mod, [NeoLoading-VulkanFix](https://github.com/Lonmo0208/NeoLoading-VulkanFix), exists solely to fix this:

> "When Minecraft uses the Vulkan rendering backend (via `GpuBackend`), NeoForge's Early Loading Screen conflicts with Vulkan's window creation flow, causing the game to **freeze or crash during startup**."

It mixins `Window.createGlfwWindow` to detect Vulkan and skip `EarlyLoadingScreenController`; requires NeoForge `26.2.0.14-beta`+. Fabric does not share this defect. *(The fix is third-party, so this is evidence of a real bug, not a NeoForge statement.)*

No NeoForge-specific Vulkan API was found. **"No evidence found" rather than proven absence** — NeoForge's source was not exhaustively audited.

### Which version should a mod target?

- **1.21.1 remains the mainstream modding version.** Fabric API and NeoForge both actively backport to it as of 2026-09. 1.20.1 also still receives updates.
- **26.1** was positioned as its intended successor — NeoForge's Technici4n (2026-03-24): "strong indications that 26.1 will be the next stable modding version, replacing 1.21.1" — but he explicitly flagged this as "a personal educated guess, not an official statement from the NeoForge team as a whole."
- **26.2** is arguably stable on both loaders (NeoForge has a non-beta release; Fabric API 0.161.0+26.2 is a release build).
- **26.3 is not yet modpack-ready on NeoForge** (all beta); Fabric-side 26.3 is stable.

---

## 8. Difficulty assessment for porting an OpenGL client rendering mod

**Confirmed constraints, ordered by impact:**

1. **You are not forced to port today.** OpenGL remains the default backend in 26.2/26.3 with automatic fallback. A mod can ship on 26.2/26.3 with the user on OpenGL.
2. **But raw OpenGL is already unsupported, and already breaks.** Fabric says so explicitly, and Voxy's port attempt broke on 26.2 *while OpenGL was the active backend* because the abstraction removed raw-GL assumptions. If your mod calls `GL11.*`/`GlStateManager` directly, you must migrate to Blaze3D/Renderpearl **regardless** of which backend you target.
3. **The target is ONE abstraction, not two backends.** Writing against `RenderPipeline`/`GpuDevice`/`CommandEncoder`/`RenderPass`/`GpuBuffer`/`GpuTexture` gives you OpenGL **and** Vulkan. Do not target Vulkan directly — there is no supported Vulkan-specific mod API, and doing so would forfeit the fallback path.
4. **There is an existing proof of concept at your scale.** Sodium 0.9.0 shipped experimental Vulkan support on 2026-06-16 by "moved all rendering to Mojang's Blaze3D API" — the same size and category of mod. CaffeineMC reported the Vulkan backend was "even faster than before on many devices".
5. **Budget for repeated relocation.** The API moved in 1.21.5 (introduced), 1.21.6–1.21.8 (RenderStates), 1.21.9 (command queue + world/entity/particle rework), 26.2 (generalisation + Vulkan), and 26.3 (relocated to `com.mojang.renderpearl`, Shaderc, mandatory shader layouts, SDL). This is not a one-time port.
6. **If you touch shaders or post-processing, cost is much higher.** Core shader JSON died in 1.21.5; 26.3 requires Shaderc-compatible GLSL with explicit `layout(location=N)` and `GL_ARB_separate_shader_objects`. Post-effect JSON survives (reformatted in 1.21.5) and `PostChain` is still the supported execution path under Vulkan — that part is more favourable than often assumed.
7. **If you are a shader-PACK mod (Iris/OptiFine-style), Vulkan is currently a dead end.** Iris does not support Vulkan; the announced "Aperture" Vulkan shader loader has no release date; Vulkan PostFX is experimental and cannot run existing packs.
8. **Loader choice matters for timing.** Fabric is fully current. NeoForge 26.3 remains all-beta, so a NeoForge-targeted port should pin to stable 26.2.0.x for now. Also budget for the NeoForge+Vulkan early-loading-screen crash.
9. **Hardware floor.** Vulkan requires 1.2 + dynamic rendering + push descriptors; unsupported hardware falls back to OpenGL (which Mojang intends to remove) — a long-term risk for old-hardware users.

---

## 9. Explicitly unconfirmed / flagged

**Could not confirm — do not treat as fact:**
- Whether any 26.4 or later version **removes OpenGL**. No date announced.
- **`PostChainConfig`** and **`EffectInstance`** — zero sources found. Unknown whether they exist.
- **`ShaderProgram` / `ProgramManager` / `ShaderInstance`** — not found in any 26.x source; deletion vs rename **unconfirmed**. `GlProgram` is the closest confirmed candidate but not asserted as the successor.
- Whether `RenderPipeline` can still be defined in JSON by a resource pack — no source either way; primers present it as in-code only.
- Official **Aperture** details (timeline, features, licensing, whether Iris development is halting) — **unverified**. The claim that "Iris development will be halted in favor of Aperture" is a **third-party Discord summary**, not a developer statement.
- **No NeoForge-specific Vulkan API** — "no evidence found", not proven absence.
- Exact NeoForge 26.2 stable **release date** (Maven API has no timestamps; no blog post; no GitHub Releases).
- Whether a NeoForge 26.3 stable release is imminent.
- The **26.1 primer** was not retrieved (fetch timed out / path 404), so a documentation gap may exist between 1.21.11 and 26.2.
- Modpack-platform adoption statistics by version (CurseForge/Modrinth pack counts).
- ⚠️ **minecraft.net article pages could not be fetched** (the site blocked the fetch tool). Mojang's official wording was therefore reproduced via the Minecraft Wiki (which quotes the in-game changelog verbatim) and news coverage, not read from the primary URL directly.

**Labelled speculation (not sourced):**
- That `PostChainConfig`/`EffectInstance` were pre-1.21.5 internal types inlined into `PostChain` when the JSON schema was reworked.
- That the 26.3 Renderpearl split is preparation for shipping the renderer as a standalone reusable library.
- That the ~6–8 week release cadence (26.1 → 26.2 → 26.3 → 26.4 snapshots) will continue.

---

## 10. Source index

**Mojang / official**
- [Another step towards Vibrant Visuals for Java Edition](https://www.minecraft.net/en-us/article/another-step-towards-vibrant-visuals-for-java-edition) (2026-02-24)
- [Minecraft 26.2 Snapshot 1](https://www.minecraft.net/en-us/article/minecraft-26-2-snapshot-1) · [26.2 Snapshot 8](https://www.minecraft.net/en-us/article/minecraft-26-2-snapshot-8)
- [Minecraft Wiki: Java Edition 26.2](https://minecraft.wiki/w/Java_Edition_26.2) · [26.3](https://minecraft.wiki/w/Java_Edition_26.3)
- Bug tracker: [MC-307311](https://mojira.dev/MC-307311) (core shaders only load after restart on Vulkan), [MC-307296](https://mojira.dev/MC-307296), [MC-307320](https://mojira.dev/MC-307320), [MC-307383](https://mojira.dev/MC-307383)

**Fabric**
- [Fabric for Minecraft 26.2](https://fabricmc.net/2026/06/15/262.html) (2026-06-15) · [26.3](https://fabricmc.net/2026/09/15/263.html) (2026-09-15) · [26.1](https://fabricmc.net/2026/03/14/261.html) (2026-03-14) · [1.21.9 & 1.21.10](https://fabricmc.net/2025/09/23/1219.html)
- [Fabric Docs — Basic Rendering Concepts](https://docs.fabricmc.net/develop/rendering/basic-concepts) ← decisive cross-API statement
- [FabricMC Discussion #4566](https://github.com/orgs/FabricMC/discussions/4566)

**NeoForged**
- Primers: [26.2](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md) · [26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md) · [1.21.5](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/1215/primers/1.21.5/index.md) · [rendered 26.3](https://docs.neoforged.net/primer/docs/26.3/)
- [NeoForge news](https://neoforged.net/news/) · [NeoForge for Minecraft 26.1](https://neoforged.net/news/26.1release/) (2026-03-24)
- [Maven versions API](https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge)

**Sodium / Iris (CaffeineMC)**
- [Sodium 0.9.0 for MC 26.2 release](https://github.com/CaffeineMC/sodium/releases/tag/mc26.2-0.9.0) (2026-06-16) ← "Moved all rendering to Mojang's Blaze3D API"
- [Sodium releases](https://github.com/CaffeineMC/sodium/releases) · [Sodium on Modrinth](https://modrinth.com/mod/sodium) · [Sodium issue #1927](https://github.com/CaffeineMC/sodium/issues/1927)
- [Iris releases](https://github.com/IrisShaders/Iris/releases) · [Iris on Modrinth](https://modrinth.com/mod/iris) · [Iris issue #1515](https://github.com/IrisShaders/Iris/issues/1515) · [Iris issue #3279 (Aperture)](https://github.com/IrisShaders/Iris/issues/3279)

**Community / third-party**
- [Voxy issue #548 (MC Vulkan API Update Impact)](https://github.com/MCRcortex/voxy/issues/548) · [Voxy issue #369](https://github.com/MCRcortex/voxy/issues/369)
- [Vulkan PostFX (VPFX) on Modrinth](https://modrinth.com/mod/vulkan-postfx) · [NeoLoading-VulkanFix](https://github.com/Lonmo0208/NeoLoading-VulkanFix) · [HMCL VulkanBypass](https://github.com/HMCL-dev/VulkanBypass)

**News coverage**
- [Tom's Hardware](https://www.tomshardware.com/video-games/after-17-years-minecraft-java-edition-starts-replacing-opengl-with-a-vulkan-multi-threaded-renderer-update-will-prove-a-boon-for-performance-and-modding) · [GIGAZINE](https://gigazine.net/gsc_news/en/20260224-minecraft-opengl-vulkan/) · [Tech Times on 26.2 Chaos Cubed](https://www.techtimes.com/articles/318457/20260616/minecraft-chaos-cubed-live-sulfur-caves-vulkan-renderer-hardcore-fix.htm)
