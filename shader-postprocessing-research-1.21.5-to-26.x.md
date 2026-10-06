# Minecraft: Java Edition — Shader & Post-Processing System Across the 1.21.5 → 26.x Renderer Rewrite

**Research report** · compiled 2026-09-29 · all findings sourced; speculation explicitly labelled.

---

## 0. Executive summary

| Question | Verdict |
|---|---|
| 1. Core shader JSON pipeline (`shaders/core/*.json`, `ShaderProgram`, `ProgramManager`, `ShaderInstance`) | **REMOVED in 1.21.5.** Replaced by in-code `RenderPipeline`. `.vsh`/`.fsh` GLSL files **survive** (still under `assets/<ns>/shaders/core/`). |
| 2. `PostChain`, `PostChainConfig`, `RenderTarget`, `TextureTarget`, `EffectInstance` | **`PostChain`, `RenderTarget`, `TextureTarget` STILL EXIST in 26.2/26.3.** Post-effect JSON survives under `assets/minecraft/post_effect/`. `PostChainConfig` / `EffectInstance` — could not confirm. |
| 3. New abstraction classes | Introduced **as a set in 1.21.5** (`RenderPipeline`, `GpuBuffer`, `GpuTexture`, `GpuDevice`, `CommandEncoder`, `RenderPass`). **Moved to `com.mojang.renderpearl` in 26.3.** |
| 4. Single cross-API abstraction? | **YES — confirmed.** `Blaze3D`/`Renderpearl` is the shared front-end; OpenGL and Vulkan are two backends. Mods must **not** target Vulkan directly. |
| 5. Vulkan-specific shader APIs | **No Vulkan-specific mod-facing shader API.** Vulkan added 26.2-snapshot-1 (2026-04-07); shader-language changes (Shaderc, `#include`) came in 26.3 and are **cross-API**, not Vulkan-only. |

**Headline correction for the report:** the shader-JSON removal and the `RenderPipeline`/`Gpu*` API introduction both happened in **1.21.5**, not in the 26.x line. The 26.x line *generalised* that API and then *relocated* it (26.3 → Renderpearl).

---

## (a) CONFIRMED FACTS, WITH SOURCES

### A1. Core shader JSON pipeline was removed in **1.21.5**

The NeoForged 1.21.4 → 1.21.5 primer, section **"Render Pipeline Rework"**, states plainly:

> "First, shader JSONs no longer exist. This is replaced by a `RenderPipeline`, which is effectively an in-code substitute."

And under **"Render Pipelines"**:

> "Previously, a pipeline was constructed using a JSON that contained all metadata from the vertex and fragement shader to their defined values, samplers, and uniforms. However, this has all been replaced with an in-code solution... This is known as a `RenderPipeline`."

Source: [NeoForged primer 1.21.5](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/1215/primers/1.21.5/index.md) · rendered: [docs.neoforged.net/primer/docs/1.21.5](https://docs.neoforged.net/primer/docs/1.21.5/)

**Revision:** the primer once says "1.21.1" in a code comment for the post-effect JSON example — that is a **typo in the primer**; the surrounding text and the branch name (`port/1215`) make clear the target is 1.21.5.

**What survived:** the `.vsh` / `.fsh` GLSL files. The same primer's `RenderPipeline` builder example still points at real files:

```java
.withVertexShader(ResourceLocation.fromNamespaceAndPath("examplemod", "example"))
// Points to 'assets/examplemod/shaders/example.vsh'
.withFragmentShader(ResourceLocation.fromNamespaceAndPath("examplemod", "example"))
// Points to 'assets/examplemod/shaders/example.fsh'
```

The 26.3 primer likewise still shows `assets/examplemod/shaders/core/example_oit_supported.vsh` / `.fsh`.

**So:** JSON *program definitions* → gone (1.21.5). GLSL *shader source files* → still present and still in `assets/<namespace>/shaders/core/`.

**`ShaderProgram` / `ProgramManager` / `ShaderInstance`:** these are the pre-1.21.5 loader-side class names (Yarn/Mojang mappings era-dependent). I did **not** find them in any 26.x source, and the JSON files they loaded no longer exist, so they cannot survive in their old form. I could **not** confirm their exact removal version or whether a same-named successor exists in 26.x — see section (b).

The nearest confirmed 26.3 equivalent is **`ShaderManager`** and **`ShaderSource`**:
- `ShaderManager` now implements `PreparableReloadListener` instead of extending `SimplePreparableReloadListener`
- `ShaderManager#SHADER_INCLUDE_PATH` is now `public`; new `SHADER_INCLUDE_EXTENSION`, `SHADER_INCLUDE_CONVERTER`, `listAllIncludes`, `isPostEffectValid`, `getAvailablePostEffects`
- `ShaderManager#getShader` → `ShaderSource#getShader`
- `ShaderSource` → `renderpearl.api.pipeline.ShaderSource`, now implements `AutoCloseable`

Source: [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md)

### A2. `PostChain`, `RenderTarget`, `TextureTarget` **survive into 26.3**

This is the most important de-confliction in this report, and it is confirmed from the 26.3 primer's own class listing, under the `net.minecraft.client.renderer` section:

> ```
> - `PostChain`
>     - `id` - The identifier of the post effect.
>     - `getReferencedExternalTargets` - Gets any referenced targets in the post effect.
>     - `closePersistentTargets` - Destroys the buffers for any persistent targets.
> ```

And in the `com.mojang.blaze3d.pipeline` block:

> ```
> - `RenderTarget` now takes in a nullable `GpuFormat` for the depth instead of just a `boolean` to determine whether to use depth
>     - `useDepth` replaced by `depthFormat`
>         - The original `boolean` can be queried via `hasDepth`
>     - `format `-> `colorFormat`
>     - `copyColorFrom` - Copies the color texture from another `RenderTarget`.
> - `TextureTarget` now takes in a nullable `GpuFormat` for the depth instead of just a `boolean` to determine whether to use depth
> ```

Sources: [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md) · [ChampionAsh5357 port/26.3 mirror](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.3/primers/26.3/index.md)

**Therefore:** `PostChain`, `RenderTarget` and `TextureTarget` were **NOT removed** in the 1.21.5 → 26.x rewrite. They were **retained and adapted** to the new GPU-abstraction types: `RenderTarget`'s raw GL integer IDs (`colorTextureId`, `depthBufferId`) became `GpuTexture` objects back in 1.21.5:

> `colorTextureId` -> `colorTexture`, now a `GpuTexture` … `depthBufferId` -> `depthTexture`, now a `GpuTexture` … `framebufferId` is removed … `checkStatus` is removed … `bindWrite`, `unbindWrite`, `setClearColor` is removed

Source: [NeoForged primer 1.21.5](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/1215/primers/1.21.5/index.md)

**Post-effect JSON survives too** — it was *reformatted*, not deleted. The 1.21.5 primer documents the exact schema change:

```json5
// Before 1.21.5 (for some pass in 'passes')
{
    "inputs": [ /*...*/ ],
    "output": "swap",
    "program": "minecraft:post/box_blur",   // <-- replaced
    "uniforms": [ { "name": "BlurDir", "values": [ 1.0, 0.0 ] } ]  // <-- no "type"
}

// 1.21.5
{
    "inputs": [ /*...*/ ],
    "output": "swap",
    "vertex_shader": "minecraft:post/blur",        // relative to 'shaders'
    "fragment_shader": "minecraft:post/box_blur",  // relative to 'shaders'
    "uniforms": [
        { "name": "BlurDir", "type": "vec2", "values": [ 1.0, 0.0 ] },  // "type" now REQUIRED
        { "name": "Radius",  "type": "float" }                          // "values" no longer required
    ]
}
```

`Uniform$Type` values: `int`, `ivec3`, `float`, `vec2`, `vec3`, `vec4`, `matrix4x4`.

Deferred uniform values are set at process time:
```java
post.process(Minecraft.getInstance().getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED, pass -> {
    pass.setUniform("Radius", 0.4f);
});
```

**26.3 changes to post effects (confirmed):**
- `assets/minecraft/post_effect/transparency.json` is **removed**
- `ServerPlayer#sendPostEffects`, `addPostEffect`, `clearPostEffects`, `getPostEffects`, `removePostEffect` — handles post effects applied on the player
- `Player#postEffects` — the post effects currently applied to the player
- `GameRenderState#requestedPostEffects` — "Post effects used"; `useShaderTransparency` is removed
- `DebugEntryPostEffect` → `DebugEntryPostEffects`, now listing all active post effects
- `ShaderManager#isPostEffectValid` — "Checks if a post effect is only targetting valid render targets"
- `ShaderManager#getAvailablePostEffects` — "Returns all known post effects"
- `ShaderManager#SHADER_INCLUDE_EXTENSION` / `SHADER_INCLUDE_CONVERTER` — references to include shaders
- Screen/spectator post-effect handling: `clearPostEffect` → `clearSpectatedEntityPostEffect`; `togglePostEffect` → `toggleSpectatorPostEffect`

Source: [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md)

Note `PostChain#closePersistentTargets` — "Destroys the buffers for any **persistent targets**" — confirms the post-chain still owns long-lived render targets in 26.3.

### A3. The newer abstraction classes — introduced in **1.21.5**, relocated in **26.3**

**Introduced in 1.21.5** (NeoForged primer 1.21.5, "Abstracting Open GL" section):

| Class | What it abstracts | Source quote |
|---|---|---|
| `GpuDevice` | "an interface that acts like a general implementation of a render library like OpenGL or Vulkan" | obtained via `RenderSystem.getDevice()` |
| `GpuBuffer` | vertex/index/uniform buffer storage | `GpuDevice#createBuffer` |
| `GpuTexture` | "responsible for managing anything to do with creating, writing, and releasing a texture" | `GpuDevice#createTexture`; also `setAddressMode`, `setTextureFilter` |
| `CommandEncoder` | command submission | `GpuDevice#createCommandEncoder`; `writeToTexture`, `copyTextureToTexture`, `clearTexture`, `presentTexture`, `createRenderPass` |
| `RenderPass` | a single render pass | `CommandEncoder#createRenderPass`; `setPipeline`, `bindSampler`, `setUniform`, `setVertexBuffer`, `setIndexBuffer`, `draw*` |
| `RenderPipeline` | the whole former shader-JSON + RenderType state | `RenderPipeline.builder()` |
| `RenderPipelines` | vanilla pipeline registry/holder | used as `RenderPipelines.register(...)` in the primer example |
| `BlendFunction`, `DepthTestFunction`, `PolygonMode`, `LogicOp`, `TextureFormat`, `FilterMode`, `AddressMode` | GL state enums/objects replacing raw GL constants | 1.21.5 primer, "Object References" |
| `CompiledRenderPipeline`, `GlRenderPipeline`, `GlDevice`, `GlCommandEncoder`, `GlRenderPass`, `GlTexture`, `GlBuffer` | the **OpenGL** implementation side (`com.mojang.blaze3d.opengl`) | 1.21.5 primer class list |

The 1.21.5 primer explicitly frames this as **"Abstracting Open GL"**, and the header example is:

```java
GpuDevice device = RenderSystem.getDevice();
try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
        .createRenderPass(this.texture, OptionalInt.of(0xFFFFFFFF))) {
    pass.setPipeline(EXAMPLE_PIPELINE);
    pass.setVertexBuffer(0, vertexBuffer);
    pass.setIndexBuffer(indexBuffer, indices.type());
    pass.bindSampler("Sampler0", RenderSystem.getShaderTexture(0));
    pass.drawIndexed(0, 6);
}
```

**1.21.5 → 1.21.6/1.21.7/1.21.8:** the Fabric documentation confirms continuation rather than introduction:

> "IMPORTANT UPDATE — Starting from **1.21.6**, large changes are being implemented to the rendering pipeline, such as moving towards `RenderType`s and `RenderPipeline`s and more importantly, `RenderState`s, with the ultimate goal of being able to prepare the next frame while drawing the current frame."

And: "For example, in **1.21.8** GUI rendering adopted this model, and `GuiGraphicsExtractor` methods simply add to the render state."

Source: [Fabric Docs — Basic Rendering Concepts (26.2)](https://docs.fabricmc.net/develop/rendering/basic-concepts)

**1.21.9** did not introduce `RenderPipeline` — it was already in use. The [Fabric 1.21.9 & 1.21.10 blog (2025-09-23)](https://fabricmc.net/2025/09/23/1219.html) discusses `RenderPipeline` as an existing API, e.g. Fabric adding `RenderPipeline.Builder.withUsePipelineDrawModeForGui` for GUI rendering, and reworking world rendering around `OrderedRenderCommandQueue`. The 1.21.9 work was about `RenderState` extraction and batching, not the pipeline abstraction.

**26.2 generalised the API further** (confirmed by both primers):
- `TextureFormat` + `VertexFormatElement` buffer formats → **`GpuFormat` enum** (`R8_UNORM`, `RGBA32_SINT`, `D32_FLOAT_S8_UINT`, …); note "their mappings do not have one-to-one parity for the GL codes previously used"
- `DestFactor` + `SourceFactor` → single **`BlendFactor`**, with **`BlendOp`** (`ADD` default)
- `VertexFormat`/`VertexFormatElement` → dynamic framework, elements built via `$Builder#addAttribute`, max 16 attributes; `VertexFormat$IndexType` → **`IndexType`**, `VertexFormat$Mode` → **`PrimitiveTopology`**
- `RenderPipeline` can now define **up to 16 vertex buffers** and **up to 8 `ColorTargetState`s**
- **`BindGroupLayout` introduced**, replacing the flat sampler list and `RenderPipeline#UniformDescription`; attached via `RenderPipeline$Builder#withBindGroupLayout`; vanilla layouts live in `BindGroupLayouts`
- `RenderPass`es can define a **sub-rectangle** of a texture via `CommandEncoder#createRenderPass`
- New `TelemetryEventType#GRAPHICS_CAPABILITIES` + `TelemetryProperty` `BACKEND_NAME`, `BACKEND_FAILURE_MESSAGE`, `BACKEND_FAILURE_REASON`, `BACKEND_FAILURE_MISSING_CAPABILITIES`
- `GlslCompiler` / `ShaderSource`: `getVertexShader` + `getFragmentShader` merged into `getShaders`; `pushConstantSize` added

Source: [NeoForged primer 26.2](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md)

**26.3 relocated everything to Renderpearl:**

> "The render API that previously lived in Blaze3d has been split into a separate project called Renderpearl. All rendering classes that originally lived in Blaze3d have been migrated to `com.mojang.renderpearl`."
>
> - `com.mojang.renderpearl.api.*` — APIs for the elements the vanilla game interacts with.
> - `com.mojang.renderpearl.backend.*` — the objects that manage the actual logic to communicate with a graphics library.
> - `com.mojang.renderpearl.frontend.*` — the implemented APIs that the vanilla game interacts with. These classes handle most of the validation.

Key relocations confirmed in the 26.3 primer class list:

| Old (26.2) | New (26.3) |
|---|---|
| `com.mojang.blaze3d.opengl.*` | `renderpearl.backend.opengl.*` |
| `com.mojang.blaze3d.vulkan.*` | `renderpearl.backend.vulkan.*` |
| `com.mojang.blaze3d.vulkan.checkpoints.*` | `renderpearl.backend.vulkan.checkpoints.*` |
| `com.mojang.blaze3d.vulkan.init.*` | `renderpearl.backend.vulkan.init.*` |
| `com.mojang.blaze3d.textures.*` | `renderpearl.api.textures.*` |
| `RenderPipeline` | `renderpearl.api.pipeline.RenderPipeline` |
| `BlendFactor` / `BlendOp` / `CompareOp` / `PolygonMode` | `renderpearl.api.pipeline.*` |
| `BindGroupLayout` | `renderpearl.api.pipeline.BindGroupLayout` |
| `CompiledRenderPipeline` | `renderpearl.api.pipeline.CompiledRenderPipeline` |
| `DepthStencilState` | `renderpearl.api.pipeline.DepthStencilState` |
| `GpuFormat` | `renderpearl.api.GpuFormat` |
| `GpuBuffer` | `renderpearl.api.buffers.GpuBuffer` (now an **interface**; class part → `renderpearl.backend.common.BaseGpuBuffer`) |
| `GpuBufferSlice` | `renderpearl.api.buffers.GpuBufferSlice` |
| `GpuFence` | `renderpearl.api.commands.GpuFence` |
| `CommandEncoder` | **`renderpearl.frontend.FrontendCommandEncoder`** (interface: `renderpearl.api.commands.CommandEncoder`) |
| `GpuDevice` | **`renderpearl.frontend.FrontendGpuDevice`** (interface: `renderpearl.api.device.GpuDevice`) |
| `RenderPass` | **`renderpearl.frontend.FrontendRenderPass`** (interface: `renderpearl.api.commands.RenderPass`) |
| `GpuSurface` | `renderpearl.frontend.FrontendGpuSurface` |
| `ShaderSource` | `renderpearl.api.pipeline.ShaderSource` (now `AutoCloseable`) |
| `ShaderType` / `UniformType` | `renderpearl.api.pipeline.*` |
| `VertexFormat` / `VertexFormatElement` | `renderpearl.api.vertex.*` |
| `GlslCompiler` | `renderpearl.frontend.shaders.GlslCompiler` |
| `ShaderCompileException` | `renderpearl.util.ShaderCompileException` |
| `GpuDeviceLossException` | `renderpearl.api.device.GpuDeviceLossException` |
| `GpuOutOfMemoryException` | `renderpearl.api.device.GpuOutOfMemoryException` |
| `IndexType` / `PrimitiveTopology` | `renderpearl.api.pipeline.*` |
| `GpuQuery` / `GpuQueryPool` | `renderpearl.api.commands.*` (now `UncheckedAutoCloseable`) |
| `HintsAndWorkarounds` / `DeviceFeatures` / `DeviceInfo` / `DeviceLimits` / `DeviceType` / `GpuBackend` | `renderpearl.api.device.*` |
| `RenderPassDescriptor` | `renderpearl.api.commands.RenderPassDescriptor` (now a **record**) |

New 26.3-only types: `renderpearl.backend.api.SpvModule` (SPIR-V intermediate module), `renderpearl.backend.opengl.GlPipelineRecompiler` ("Handles recompiling the SPV shader module into GLSL"), `renderpearl.backend.vulkan.VulkanFeatureSets`, `renderpearl.util.UncheckedAutoCloseable`.

Source: [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md)

### A4. **Cross-API abstraction: YES — one front-end, two backends** (the crucial question)

**Answer: a mod written against the Blaze3D/Renderpearl API works on BOTH OpenGL and Vulkan. Mods must NOT target Vulkan directly.**

The single most direct piece of evidence is the Fabric documentation banner, which is an explicit instruction to mod authors:

> **WARNING** — "Use of raw OpenGL is not supported on Minecraft 26.2 as it has released with an optional Vulkan backend. Instead, you must use the **`Blaze3D` abstraction layer, which sits between your code and the rendering backend (either OpenGL or Vulkan)**."

Source: [Fabric Docs — Basic Rendering Concepts (26.2)](https://docs.fabricmc.net/develop/rendering/basic-concepts)

Supporting evidence:

1. **Parallel backend packages, one shared API.** NeoForged 26.2 primer: "for all classes in `com.mojang.blaze3d.opengl`, there is generally a parallel found in `com.mojang.blaze3d.vulkan`." The modder-facing types (`RenderPipeline`, `GpuDevice`, `CommandEncoder`, `RenderPass`, `GpuBuffer`, `GpuTexture`) live in the **shared** `com.mojang.blaze3d.*` namespace, *not* in either backend package. Only the implementations (`GlDevice`, `VulkanDevice`, …) are backend-specific.

2. **26.3 formalised this into a three-layer architecture** with the split into Renderpearl:
   - `api.*` — the interface definitions the game/mod interacts with
   - `frontend.*` — the implemented APIs that "handle most of the validation" (backend-agnostic)
   - `backend.*` — "the objects that manage the actual logic to communicate with a graphics library" (opengl / vulkan)
   
   `CommandEncoder`, `GpuDevice` and `RenderPass` became **interface + frontend implementation pairs** (e.g. `renderpearl.api.commands.CommandEncoder` + `renderpearl.frontend.FrontendCommandEncoder`). That is the textbook shape of a shared front-end over pluggable backends.

3. **All modding guidance targets the shared layer.** Every code example in the 1.21.5, 26.2 and 26.3 primers uses `RenderPipeline`, `RenderSystem.getDevice()`, `RenderPass`, `GpuTexture` — never a Vulkan-specific type. The 26.3 OIT example, the `BindGroupLayout` example and the `RenderSetup` example are all backend-agnostic.

4. **Shader compilation is unified, and the GL backend recompiles Vulkan SPIR-V.** In 26.3, "Shaderc is now used for compilation on both OpenGL and Vulkan via the `GlslCompiler`", and `renderpearl.backend.opengl.GlPipelineRecompiler` "handles recompiling the SPV shader module into GLSL". A single authoring path feeds both backends.

5. **Device selection is a runtime preference, not a compile-time choice.** 26.2 added a video-setting "Graphics API" with three values — `Default`, `Prefer Vulkan (Experimental)`, `Prefer OpenGL` — each with automatic fallback, and crash-triggered setting changes. Mods are not involved in this decision.

Source: [Minecraft Wiki — Java Edition 26.2](https://minecraft.wiki/w/Java_Edition_26.2) · [Java Edition 26.2/Development versions](https://minecraft.wiki/w/Java_Edition_26.2/Development_versions)

**Where exactly the boundary sits (specifically requested):**

```
mod / vanilla game code
      │  uses only backend-agnostic types
      ▼
RenderPipeline · GpuDevice · CommandEncoder · RenderPass · GpuBuffer · GpuTexture
BindGroupLayout · GpuFormat · BlendFunction · DepthStencilState · PrimitiveTopology
      │            (26.2: com.mojang.blaze3d.*   →   26.3: com.mojang.renderpearl.api.* / .frontend.*)
      ▼
 ┌─────────────────────────┴─────────────────────────┐
 ▼                                                   ▼
backend.opengl.*                                backend.vulkan.*
GlDevice, GlCommandEncoder,                     VulkanDevice, VulkanCommandEncoder,
GlRenderPass, GlTexture, GlBuffer,              VulkanRenderPass, ..., VulkanFeatureSets
GlPipelineRecompiler  (SPIR-V → GLSL)
```

**The boundary is: mods live above `RenderPipeline`/`GpuDevice`; `com.mojang.renderpearl.backend.*` (formerly `com.mojang.blaze3d.opengl.*` / `.vulkan.*`) is Mojang-internal backend territory.** There is **no supported Vulkan-specific mod-facing shader API** — targeting `backend.vulkan.*` would be reaching through the abstraction.

The one caveat worth flagging for mod authors: because both backends must be satisfied, the wording "most vanilla shaders **require** the machine to have the `GL_ARB_separate_shader_objects` extension **as a requirement of Vulkan**" (26.3 primer) shows that supporting Vulkan *raises the minimum GL capability floor* even when running on the OpenGL backend. Portability is achieved by constraining GL, not by branching per-API.

### A5. Vulkan-specific shader APIs and snapshot changelog entries

**Timeline (confirmed):**

| Date | Version | Event |
|---|---|---|
| 2026-04-07 | **26.2-snapshot-1** | Vulkan renderer added (experimental); "Graphics API" video option added with `Default` / `Prefer Vulkan` / `Prefer OpenGL` |
| 2026-04-09 | 26.2 Snapshot 2 | Vulkan bug fixes (see below) |
| 2026-06-16 | **26.2 "Chaos Cubed"** | Full release; in the release build "Default" = "Prefer OpenGL" |
| 2026-09-15 | **26.3 "Wilderness Bound"** | Renderpearl split, Shaderc/`#include`, OIT, SDL |
| (26.4) | 26.4 | Per the 26.4 primer, contains no rendering/shader changes of note (placement domains, biome resolvers only) |

Sources: [Java Edition 26.2/Development versions](https://minecraft.wiki/w/Java_Edition_26.2/Development_versions) · [Java Edition 26.2](https://minecraft.wiki/w/Java_Edition_26.2) · [Fabric for Minecraft 26.3](https://fabricmc.net/2026/09/15/263.html) · [NeoForged primer 26.4](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.4/primers/26.4/index.md)

**26.2-snapshot-1 changelog — "Vulkan support" section (verbatim points):**
- "As previously announced, it is intended to switch the game from OpenGL to Vulkan."
- "Vulkan is not supported by older hardware or drivers – OpenGL will be used as a fallback on those cases."
- "The current requirement is **Vulkan 1.2 with dynamic rendering and push descriptors**, but this requirement may increase or decrease over time."
- "Currently an experimental rendering backend, and may not be as performant or stable."
- "Under Vulkan, it is preferred the player's dedicated graphics card over any integrated graphics, which is a change from OpenGL."
- "The player can see which backend is being used in the F3 debug overlay (in the `system_specs` section)."
- "On macOS, **MoltenVK** is used to translate Vulkan to Metal."

**26.2-snapshot-1 "Shaders" changelog entry (the only shader entry in that snapshot) — this is a GLSL rename, NOT a Vulkan API:**
> "The `core/rendertype_text`, `core/rendertype_text_see_through`, `core/rendertype_text_intensity`, `core/rendertype_text_intensity_see_through`, `core/rendertype_text_background`, and `core/rendertype_text_background_see_through` shaders have been replaced by `core/text` and `core/text_background`
> - Variants are now controlled by shader defines: `IS_GUI`, `IS_SEE_THROUGH`, and `IS_GRAYSCALE`"

**26.2-snapshot-1 also:** "Rendering now uses a **reversed depth buffer**, which helps with Z-fighting on most hardware." And "Profiling the game with Tracy (`--tracy`) now includes GPU timings."

**Vulkan bug fixes in 26.2 Snapshot 2** — these directly evidence that core shaders are shared across both backends:
- [MC-307311](https://mojira.dev/MC-307311) — "Core shaders only load after restart when using Vulkan."
- [MC-307296](https://mojira.dev/MC-307296) — "Launching the game with an incompatible resource pack that modifies core shaders causes the game to softlock."
- [MC-307320](https://mojira.dev/MC-307320) — "Vulkan prevents the game from opening and crashes some GPUs."
- [MC-307383](https://mojira.dev/MC-307383) — "The 'Exclusive Fullscreen' setting does not work with the Vulkan rendering backend."

MC-307311 is decisive: **the same "core shaders" resource-pack concept works under Vulkan**, confirming shared shaders rather than a Vulkan-only path.

**26.3 shader-language changes (`GlslCompiler`, cross-API — not Vulkan-specific):**
- "Shaderc is now used for compilation on **both OpenGL and Vulkan** via the `GlslCompiler`."
- `#moj_import` was **renamed to `#include`** to import functions from other shaders.
- Both `in` and `out` uniforms **must** specify their location: `layout(location = N) in ...` / `layout(location = N) out ...`
- Three macros: `RENDERPEARL_DEPTH_IS_ZERO_TO_ONE`, `RENDERPEARL_EXPLICIT_DEPTH_INVARIANCE`, `RENDERPEARL_INSTANCE_INDEX_INCLUDES_BASE_INSTANCE`
- "most vanilla shaders require the machine to have the `GL_ARB_separate_shader_objects` extension as a requirement of Vulkan"

**Other 26.3 render/shader-adjacent changes:**
- **Order-Independent Transparency (Wavelet OIT)** — three phases (depth bounds, transmittance, accumulate); shader define `OIT_ALPHA_ONLY`; new `minecraft:oit.glsl`; new `OitPipelineSet` built from a `RenderPipeline.Snippet` with `withDepthBoundsModifier` / `withTransmittanceModifier` / `withAccumulateModifier`; wired up via `RenderSetup#setOitPipelines`; drawn with `RenderType#prepare` + `PreparedRenderType#drawFromBufferOit`
- `RenderSystem#outputColorTextureOverride` and `#outputDepthTextureOverride` **fully removed**
- `assets/minecraft/shaders/core` additions include `blit_depth.fsh`; `assets/minecraft/post_effect/transparency.json` removed
- New `RenderPipelines` constants: `OIT_DEPTH_BOUNDS_SNIPPET`, `OIT_TRANSMITTANCE_SNIPPET`, `OIT_ACCUMULATE_SNIPPET`, `SOLID_TERRAIN_MULTIDRAW`, `WIREFRAME_MULTIDRAW`, `CUTOUT_TERRAIN_MULTIDRAW`, `TRANSLUCENT_TERRAIN_MULTIDRAW`, `OIT_*`, `ENTITY_SHADOW`, `GLINT_SNIPPET`, `GLINT_SPECIAL_SNIPPET`, `ARMOR_CUTOUT_NO_CULL_GLINT`, `ENTITY_SOLID_GLINT`, `ITEM_CUTOUT_GLINT`, `ITEM_CUTOUT_GLINT_SPECIAL`, `ITEM_TRANSLUCENT_GLINT`, `ITEM_TRANSLUCENT_GLINT_SPECIAL`, `LINES_TRANSLUCENT_NO_DEPTH_WRITE`, `WEATHER`, `BLIT_DEPTH_BOUNDS`, `BLIT_DEPTH_DURING_DEPTH_BOUNDS`, `BLIT_DEPTH`, `INTEGRATE_DEPTH`; `getStaticPipelines` → `requiredPipelines`; new `optionalPipelines`
- `RenderTarget#copyColorFrom` — copies the color texture from another `RenderTarget`
- SDL replaces GLFW; `com.mojang.blaze3d.platform.TextInputManager` used by Fabric docs in 26.3 (note: the 26.3 primer inconsistently writes `TextureInputManager` in prose — appears to be a typo)

Sources: [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md) · [Fabric for Minecraft 26.3](https://fabricmc.net/2026/09/15/263.html) · [Wavelet OIT paper](https://arxiv.org/pdf/2201.00094)

### A6. Version/date reference table

| Version | Name | Release date | Renderer status |
|---|---|---|---|
| 1.21.5 | — | 2025-03 (1.21.5 released March 2025) | Shader JSON removed; `RenderPipeline`/`Gpu*`/`RenderPass`/`CommandEncoder` introduced; post-effect JSON schema changed |
| 1.21.6–1.21.8 | — | 2025 | RenderState migration; GUI render states (1.21.8) |
| 1.21.9 / 1.21.10 | — | 2025-09-30 (blog dated 2025-09-23) | `OrderedRenderCommandQueue`; world/entity/particle/BE rendering reworked |
| 26.1 | — | 2025-12-02 (per wiki nav) | (not deeply covered here — see caveats) |
| 26.2 | **Chaos Cubed** | **2026-06-16** | **Vulkan added (experimental, opt-in)**; `GpuFormat`, `BlendFactor`/`BlendOp`, `BindGroupLayout`, dynamic `VertexFormat`; protocol 776; data version 4903; resource pack format 88.0; Java SE 25 |
| 26.3 | **Wilderness Bound** | **2026-09-15** | **Blaze3D → Renderpearl (`com.mojang.renderpearl`)**; Shaderc + `#include` + `layout(location)`; Wavelet OIT; SDL |
| 26.4 | — | (post-26.3) | No notable shader/renderer changes in the 26.4 primer |

Note the 26.2 primer's "Pack Changes" links `https://misode.github.io/versions/?id=26.2&tab=changelog` for the exhaustive vanilla changelog — useful for further verification.

---

## (b) COULD NOT CONFIRM / OPEN QUESTIONS

1. **`PostChainConfig`** — I found **no** mention of a class by this name in the 1.21.5, 26.2, 26.3 or 26.4 primers, nor in search results. `PostChain` is confirmed alive; `PostChainConfig` is **unconfirmed in either direction**. It is plausible this is a Yarn/Forge-era name for a JSON codec holder that was folded into `PostChain` itself, but I have **no source** for that — treat as unknown.

2. **`EffectInstance`** — likewise **not found** in any primer or search result. Note that `PostChain`'s pre-1.21.5 internals historically used `PostPass`/`EffectInstance` for individual passes. I could **not** confirm whether an equivalent exists in 26.x, or under what name. The confirmed post-chain API surface in 26.3 is only: `id`, `getReferencedExternalTargets`, `closePersistentTargets`, plus `process(...)` / `setUniform(...)` (the latter from the 1.21.5 primer example). **Unknown.**

3. **`ShaderProgram` / `ProgramManager` / `ShaderInstance`** — not found in 26.x sources. The JSON they consumed is confirmed gone (1.21.5). Whether the *classes* were deleted outright, renamed, or retained as internal GL objects bound to `RenderPipeline` is **unconfirmed**. `GlProgram` is confirmed to exist in 26.2 (`setupUniforms` → `setupBindGroupLayouts`; `link` now takes an array of `VertexFormat`s) — that is the closest confirmed successor candidate, but I cannot assert it *is* the successor to `ShaderProgram`.

4. **Exact 1.21.5 release date** — I did not independently fetch a source stating it. The primer is titled "1.21.4 -> 1.21.5" and the branch is `port/1215`; I did not verify the calendar date from a primary source.

5. **26.1 primer contents** — my fetch of the 26.1 primer timed out and the raw path (`update/26.1/primers/26.1/index.md`) was not re-attempted successfully. I therefore **have not read the 26.1 → 26.2 or 26.0 → 26.1 primer**. If 26.1 made further shader/post-effect changes, this report may have a **gap between 1.21.10/1.21.11 and 26.2**. The Fabric docs version selector listing "Minecraft 26.1.2" and "Minecraft 1.21.11" as documented branches suggests those versions exist and are documented.

6. **`net.minecraft.client.renderer.PostChain` in 26.2 specifically** — I confirmed `PostChain` in the **26.3** primer's delta list (meaning it existed in 26.2 and changed in 26.3) and confirmed its existence in 1.21.8 via a javadoc mirror ([aldak0.ru javadoc 1.21.8](https://aldak0.ru/javadoc/1.21.8-21.8.x/net/minecraft/client/renderer/PostChain.html)). I did **not** obtain a direct 26.2 class listing. The 26.3 evidence is strong but indirect for "26.2 exactly".

7. **`ProgramManager`-equivalent for `.vsh`/`.fsh` discovery** — I could not confirm which class discovers `shaders/core/*.vsh|.fsh` in 26.x. `ShaderManager` is the likely candidate (it owns `SHADER_INCLUDE_PATH`, `listAllIncludes`, `getAvailablePostEffects` in 26.3), but I have **no source** explicitly saying it loads core shaders.

8. **Whether `RenderPipeline` JSON *may* still be datapack/resource-pack definable** — the 1.21.5 primer says shader JSONs no longer exist and the pipeline is "in-code". I found no statement about a JSON serialization of `RenderPipeline`. Mods appear expected to build pipelines in Java. **Not confirmed either way** whether a JSON path was later reintroduced.

9. **Minecraft.net article fetch failures** — `https://www.minecraft.net/en-us/article/minecraft-26-2-snapshot-1` failed to fetch directly (as the task warned). I used the Minecraft Wiki mirrors instead, which reproduce the changelog text. The parent's other researcher may have obtained the primary article.

---

## (c) EXPLICIT SPECULATION (clearly labelled — not sourced)

- **Speculation:** `PostChainConfig` and `EffectInstance` most likely correspond to pre-1.21.5 Yarn-mapped internals of `PostChain` that were inlined into `PostChain` when the JSON schema was reworked in 1.21.5. I have **no source** for this and it should not be published as fact.
- **Speculation:** the reason `PostChain` survived while `ShaderInstance`/shader JSON did not is that post-effects were *already* data-driven at the pass level (explicit `vertex_shader`/`fragment_shader` file references plus typed uniforms), so only the `program` indirection needed removing — whereas core shaders had their entire render state (blend, depth, cull, samplers, defines) embedded in JSON, which is exactly what `RenderPipeline` absorbed. This is an inference from the two schemas, not a sourced claim.
- **Speculation:** the 26.3 Renderpearl split is likely preparation for shipping Blaze3D/Renderpearl as a standalone reusable library (possibly for Bedrock-adjacent or other Mojang projects), given it was moved to a `com.mojang.*` namespace outside the game's `com.mojang.blaze3d` render-specific naming. Not sourced.

---

## (d) Source list

**Primary / first-party technical**
- [NeoForged primer 26.2](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md) — also mirrored at [ChampionAsh5357/neoforged-github port/26.2](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.2/primers/26.2/index.md) and rendered at [docs.neoforged.net/primer/docs/26.2](https://docs.neoforged.net/primer/docs/26.2/)
- [NeoForged primer 26.3](https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md) — also [ChampionAsh5357 port/26.3](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.3/primers/26.3/index.md) and [docs.neoforged.net/primer/docs/26.3](https://docs.neoforged.net/primer/docs/26.3/)
- [NeoForged primer 1.21.5](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/1215/primers/1.21.5/index.md) — rendered at [docs.neoforged.net/primer/docs/1.21.5](https://docs.neoforged.net/primer/docs/1.21.5/)
- [NeoForged primer 26.4](https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.4/primers/26.4/index.md)
- [NeoForged primers index](https://docs.neoforged.net/primer/)

**Fabric**
- [Fabric for Minecraft 26.3 (2026-09-15)](https://fabricmc.net/2026/09/15/263.html)
- [Fabric for Minecraft 1.21.9 & 1.21.10 (2025-09-23)](https://fabricmc.net/2025/09/23/1219.html)
- [Fabric Docs — Basic Rendering Concepts (26.2)](https://docs.fabricmc.net/develop/rendering/basic-concepts) — contains the decisive "Blaze3D abstraction layer... between your code and the rendering backend (either OpenGL or Vulkan)" statement
- [Fabric Docs — Rendering (index)](https://docs.fabricmc.net/develop/rendering/)
- [Fabric Docs — Rendering in the World](https://docs.fabricmc.net/develop/rendering/world)
- [FabricMC Discussion #4566 — "1.21.5 BufferRenderer is completely gone?"](https://github.com/orgs/FabricMC/discussions/4566)
- [FabricMC fabric-api Issue #5253 — Missing `fabric-renderer-*`](https://github.com/FabricMC/fabric-api/issues/5253)

**Minecraft Wiki (changelog mirrors)**
- [Java Edition 26.2](https://minecraft.wiki/w/Java_Edition_26.2)
- [Java Edition 26.2/Development versions](https://minecraft.wiki/w/Java_Edition_26.2/Development_versions) — source of the 26.2-snapshot-1 date (2026-04-07) and the shaders/Vulkan changelog text
- [Java Edition 1.21.5](https://minecraft.wiki/w/Java_Edition_1.21.5)
- [Java Edition 26.1](https://minecraft.wiki/w/Java_Edition_26.1)
- [Shader](https://minecraft.wiki/w/Shader)
- [Java Edition 26.3](https://minecraft.wiki/w/Java_Edition_26.3)

**Bug tracker**
- [MC-307311 — Core shaders only load after restart when using Vulkan](https://mojira.dev/MC-307311)
- [MC-307296 — incompatible resource pack that modifies core shaders causes softlock](https://mojira.dev/MC-307296)
- [MC-307320 — Vulkan prevents the game from opening and crashes some GPUs](https://mojira.dev/MC-307320)
- [MC-307383 — Exclusive Fullscreen does not work with the Vulkan backend](https://mojira.dev/MC-307383)

**Class/mapping references**
- [mappings.dev 1.21.5 net.minecraft.client.renderer](https://mappings.dev/1.21.5/net/minecraft/client/renderer/index.html)
- [mappings.dev 1.21.9 PostChain](https://mappings.dev/1.21.9/net/minecraft/client/renderer/PostChain.html)
- [aldak0.ru javadoc 1.21.8 PostChain](https://aldak0.ru/javadoc/1.21.8-21.8.x/net/minecraft/client/renderer/PostChain.html)

**Other**
- [Misode version changelog (26.2)](https://misode.github.io/versions/?id=26.2&tab=changelog) / [(26.3)](https://misode.github.io/versions/?id=26.3&tab=changelog) / [(1.21.5)](https://misode.github.io/versions/?id=1.21.5&tab=changelog) — linked by the primers as the authoritative vanilla pack changelog
- [Wavelet OIT paper (arXiv 2201.00094)](https://arxiv.org/pdf/2201.00094) — the technique vanilla's 26.3 OIT implements
- [Stonecutter docs — Minecraft Changes](https://stonecutter.kikugie.dev/wiki/v2/guides/tools/minecraft-changes)
- [DeepWiki — Fabric Docs: Custom Render Pipelines](https://deepwiki.com/FabricMC/fabric-docs/9.3-custom-render-pipelines)
