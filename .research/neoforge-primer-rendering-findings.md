# NeoForge Migration Primer — RENDERING sections, verbatim extract

## 0. Where the primer source actually lives (IMPORTANT — the task's assumed paths were wrong)

The task guessed `neoforged/documentation` at `primer/versioned_docs/version-1.21.5/...`. **That is not where the primer text lives.**

- `neoforged/documentation` only contains a *stub* `primer/` directory. `primer/.gitignore` is literally `*` + `!.gitignore`. The real content is cloned at build time by `scripts/initialize.js` from a **different repo**:
  - `const PRIMERS_GIT = 'https://github.com/neoforged/.github';`
  - mapper: `directories: { 'primers': 'docs' }`
  - Source: https://raw.githubusercontent.com/neoforged/documentation/main/scripts/initialize.js
- **The actual primer markdown files are at:**
  `https://raw.githubusercontent.com/neoforged/.github/main/primers/<version>/index.md`
- Confirmed file sizes (bytes) from the GitHub contents API:
  - 1.21.5 — 194,956
  - 1.21.6 — 141,467
  - 1.21.9 — 210,631
  - 1.21.10 — tiny (only "Minor Migrations")
  - 1.21.11 — large
  - 26.1 — 307,202
  - 26.2 — 182,451
  - 26.3 — present
- The `web_fetch` tool truncates responses at ~100 KB, so the **tails of these files are not directly retrievable** by a plain fetch. Workarounds used and their outcomes are listed in §9.

---

## 1. Primer 1.21.5 (1.21.4 -> 1.21.5) — "Render Pipeline Rework"

Source URL: https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.5/index.md
Rendered: https://docs.neoforged.net/primer/docs/1.21.5/

### 1.1 Section intro (verbatim)

> ## Render Pipeline Rework
>
> Rendering an object to the screen, whether through a shader or a `RenderType`, has been fully or partially reworked, depending on what systems you were using previously. As such, a lot of things need to be reexplained, which a more in-depth look will be below. However, for the people who don't care, here's the TL;DR.
>
> First, shader JSONs no longer exist. This is replaced by a `RenderPipeline`, which is effectively an in-code substitute. Second, the `RenderPipeline`s forcibly make most abtrarily values into objects. For example, instead of storing the blend function mode id, you store a `BlendFunction` object. Similarly, you no longer store or setup the direct texture objects, but instead manage it through a `GpuTexture`. Finally, the `VertexBuffer` can either draw to the framebuffer by directly passing in the `RenderPipeline`, updating any necessary uniforms in the consumer, or by passing in the `RenderType`.
>
> Now, for those who need the details, let's jump into them.

### 1.2 `### Abstracting Open GL` (verbatim)

> As many are aware, Minecraft has been abstracting away their OpenGL calls and constants, and this release is no different. All of the calls to GL codes, except `BufferUsage`, have been moved out of object references, to be obtained typically by calling `GlConst$toGl`. However, with all of the other rendering reworks, there are numerous changes and complexities that require learning an entirely new system, assuming you're not using `RenderType`s.
>
> Starting from the top, all calls to the underlying render system goes through `GpuDevice`, an interface that acts like a general implementation of a render library like OpenGL or Vulkan. The device is responsible for creating buffers and textures, executing whatever commands are desired. Getting the current `GpuDevice` can be accessed through the `RenderSystem` via `getDevice` like so:
>
> ```java
> GpuDevice device = RenderSystem.getDevice();
> ```
>
> The `GpuDevice` can the create either buffers with the desired data or a texture containing information on what to render using `createBuffer` and `createTexture`, respectively. Just for redundancy, buffers hold the vertex data while textures hold the texture (color and depth) data. You should generally cache the buffer or texture object for later use with any additional data updated as needed. For reference, buffers are typically created by using a `BufferBuilder` with a `ByteBufferBuilder` to build the `MeshData` first, before passing that into `createBuffer`.
>
> With the desired buffers and textures set up, how do we actually modify render them to the screen? Well, this is handled through the `CommandEncoder`, which can also be optained from the device via `GpuDevice#createCommandEncoder`. The encoder contain the familiar read and write methods along with a few extra to clear texture to a given color or simply blit the the texture immediately to the screen (`presentTexture`). However, the most important method here is `createRenderPass`. This takes in the `GpuTexture` to draw to the screen along with a default ARGB color for the background. Additionally, it can take in a depth texture as well. This should be created using a try with resources block like so:
>
> ```java
> // We will assume you have constructed a `GpuTexture` texture for the color data
> try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(this.texture, OptionalInt.of(0xFFFFFFFF))) {
>     // Setup things here
>
> }
> ```
>
> Within the `RenderPass`, you can set the `RenderPipeline` to use, which defines the associated shaders, bind any samplers from other targets or set uniforms, scissor a part of a screen to render, and set the vertex and index buffers used to define the vertices to render. Finally, everything can be drawn to the screen using one of the `draw` methods, providing the starting index and the vertex count.
>
> ```java
> // If the buffers/textures are not created or cached, create them here
> // Any methods ran from `CommandEncoder` cannot be run while a render pass is open
> RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
> GpuBuffer vertexBuffer = RenderSystem.getQuadVertexBuffer();
> GpuBuffer indexBuffer = indices.getBuffer(6);
>
> // We will assume you have constructed a `GpuTexture` texture for the color data
> try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(this.texture, OptionalInt.of(0xFFFFFFFF))) {
>
>     // Set pipeline information along with any samplers and uniforms
>     pass.setPipeline(EXAMPLE_PIPELINE);
>     pass.setVertexBuffer(0, vertexBuffer);
>     pass.setIndexBuffer(indexBuffer, indices.type());
>     pass.bindSampler("Sampler0", RenderSystem.getShaderTexture(0));
>
>     // Then, draw everything to the screen
>     // In this example, the buffer just contains a single quad
>     // For those unaware, the vertex count is 6 as a quad is made up of 2 triangles, so 2 vertices overlap
>     pass.drawIndexed(0, 6);
> }
> ```
>
> However, unless you need such fine control, it is recommended to use a `RenderType` with the `MultiBufferSource` when necessary as that sets up most things for you.

**KEY OBSERVATION for the task's Vulkan question:** the *only* mention of Vulkan anywhere in the 1.21.5 primer is the phrase "an interface that acts like a general implementation of a render library like OpenGL or Vulkan". There is **no Vulkan backend in 1.21.5** — it is aspirational wording.

### 1.3 `### Object References` (verbatim)

> Most raw references to GL codes used for determining the mode and handling the texture have been replaced with objects. As the TL;DR previously mentioned, these are stored typically as some sort of enum or object that can then be resolved to their GL counterparts. Some objects contain their reference identifier directly, like `BlendFunction`. Others are simply placeholders that are resolved in their appropriate location, like `DepthTestFunction` whose enum values are converted via `RenderPipeline#toGl`.
>
> However, the biggest change is the addition of the `GpuTexture`. This is responsible for managing anything to do with creating, writing, and releasing a texture written to some buffer. At initialization, the texture is created and bound, with any necessary parameters set for mipmaps and texture formats. These `GpuTexture`s are stored and referenced everywhere, from the depth and color targets for a `RenderTarget` to the texture backing a `TextureAtlas`. Then, once no longer need, the texture is released by calling `#close`. Note that although technically `#bind` can be called again, the texture is already considered deleted and should not be used.
>
> If, for some reason, you need to use a `GpuTexture`, it's actually quite simple to use. First, you just construct the `GpuTexture` via `GpuDevice#createTexture`. Then, if you need to change any of the addressing or texture mipmap filters, you can apply them whenever before writing.
>
> (java example `MyTextureManager` with `RenderSystem.getDevice().createTexture(...)`, `setAddressMode`, `setTextureFilter`)
>
> Then, whenever you want to upload something to the texture, you call `CommandEncoder#writeToTexture` or `CommandEncoder#copyTextureToTexture`. This either takes in the `NativeImage` to write from or an `IntBuffer` with the texture data and a `NativeImage$Format` to use.
>
> Finally, once you're done with the texture, don't forget to release it via `#close` if it's not already handled for you.

### 1.4 `### Render Pipelines` (verbatim, prose)

> Previously, a pipeline was constructed using a JSON that contained all metadata from the vertex and fragement shader to their defined values, samplers, and uniforms. However, this has all been replaced with an in-code solution that more localizes some parts of the JSON and some parts that were relegated to the `RenderType`. This is known as a `RenderPipeline`.
>
> A `RenderPipeline` can be constructed using its builder via `RenderPipeline#builder`. A pipeline can then be built by calling `build`. If you want the shader to be pre-compiled without any additional work, the final pipeline can be passed to `RenderPipeline#register`. However, you can also handle the compilation yourself if more graceful fail states are desired. If you have snippets that are used across multiple pipelines, then a partial pipeline can be built via `$Builder#buildSnippet` and then passed to the constructing pipelines in the `builder` method.

The accompanying builder example is the authoritative statement of the **new shader resource layout**:

```java
public static final RenderPipeline EXAMPLE_PIPELINE = RenderPipelines.register(
    RenderPipeline.builder()
    // The name of the pipeline (required)
    .withLocation(ResourceLocation.fromNamespaceAndPath("examplemod", "pipeline/example"))
    // The location of the vertex shader, relative to 'shaders' (required)
    // Points to 'assets/examplemod/shaders/example.vsh'
    .withVertexShader(ResourceLocation.fromNamespaceAndPath("examplemod", "example"))
    // The location of the fragment shader, relative to 'shaders' (required)
    // Points to 'assets/examplemod/shaders/example.fsh'
    .withFragmentShader(ResourceLocation.fromNamespaceAndPath("examplemod", "example"))
    // The format of the vertices within the shader (required)
    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
    // Adds constants that can be referenced within the shaders
    .withShaderDefines("ALPHA_CUTOUT", 0.5)
    // Adds the texture sampler2Ds that can be referenced within the shaders
    // Typically, the shader textures stored in the `RenderSystem` is referenced via `Sampler0` - `Sampler11`
    .withSampler("Sampler0")
    // Adds uniforms that can be referenced within the shaders
    // Defaults can be found in `CompiledShaderProgram#setupUniforms`
    .withUniform("ModelOffset", UniformType.VEC3)
    .withUniform("CustomUniform", UniformType.INT)
    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
    .withPolygonMode(PolygonMode.FILL)
    .withCull(false)
    // Made up of the `GlStateManager$SourceFactor` and `GlStateManager$DestFactor`
    .withBlend(BlendFunction.TRANSLUCENT)
    .withColorWrite(false, false)
    .withDepthWrite(false)
    .withColorLogic(LogicOp.NONE)
    .withDepthBias(0f, 0f)
    .build()
);
```

**Shader file layout statement (1.21.5):** vertex/fragment shader locations are *relative to `shaders`* → `assets/<ns>/shaders/<name>.vsh` and `assets/<ns>/shaders/<name>.fsh`. The pipeline JSON (`assets/<ns>/shaders/core/*.json`) is **gone**; it is `RenderPipeline` in code.

### 1.5 `### Post Effects` (verbatim, prose)

> Given that the pipeline JSONs have been stripped, this also effects the post effects. The `program` is replaced with directly specifying the `vertex_shader` and the `fragment_shader`. Additionally, uniforms must specify their `type`.
>
> ```json5
> // Before 1.21.5 (for some pass in 'passes')
> {
>     "inputs": [ /*...*/ ],
>     "output": "swap",
>     // Replaced by 'vertex_shader', 'fragement_shader'
>     "program": "minecraft:post/box_blur",
>     "uniforms": [ { "name": "BlurDir", "values": [ 1.0, 0.0 ] }, { "name": "Radius", "values": [ 0.0 ] } ]
> }
>
> // 1.21.1 (for some pass in 'passes')
> {
>     "inputs": [ /*...*/ ],
>     "output": "swap",
>     // Relative to 'shaders'
>     // Points to 'assets/minecraft/shaders/post/blur.vsh'
>     "vertex_shader": "minecraft:post/blur",
>     // Points to 'assets/minecraft/shaders/post/box_blur.fsh'
>     "fragment_shader": "minecraft:post/box_blur",
>     "uniforms": [
>         { "name": "BlurDir", "type": "vec2", "values": [ 1.0, 0.0 ] },
>         { "name": "Radius", "type": "float" /* Values are no longer required */ }
>     ]
> }
> ```
>
> Note that if you do not define a value for a uniform, they still must be specified before processing the `PostChain` by calling `#setUniform` within the `RenderPass` consumer of `PostChain#process`.
>
> ```java
> // Assume we already got the `PostChain` post
> post.process(Minecraft.getInstance().getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED, pass -> {
>     pass.setUniform("Radius", 0.4f);
> });
> ```

**Post shader layout (1.21.5):** `assets/<ns>/shaders/post/<name>.vsh` / `.fsh` (the `program` JSON key is replaced by explicit `vertex_shader` / `fragment_shader`).

### 1.6 1.21.5 bullet list — GlStateManager / RenderSystem / GL removal (verbatim, from the Algolia index of the rendered page; the raw markdown tail is beyond the fetch cap)

Retrieved from the docs site's own Algolia search index (appId `05RJFT798Z`, index `neoforged`), record `objectID` `74-https://docs.neoforged.net/primer/docs/1.21.5/`, anchor `post-effects` — this record contains the *full* tail bullet list of the "Render Pipeline Rework" section.

**`com.mojang.blaze3d.GpuOutOfMemoryException`** — An exception thrown when a texture could not be allocated on the GPU.

**`com.mojang.blaze3d.buffers`**
- `BufferType` no longer stores the GL codes, now in `GlConst#toGl`
- `BufferUsage` no longer stores the GL codes, now in `GlConst#toGl`
  - `isReadable`, `isWritable` - Returns whether the buffer can be read from or written to.
- `GpuBuffer` is now abstract
  - Constructor with `ByteBuffer` is removed
  - `size` - Returns the size of the buffer.
  - `type` - Returns the type of the buffer.
  - `resize`, `write`, `read`, `bind` is removed
  - `usage` - Returns the usage of the buffer.
  - `close` is now abstract
  - `isClosed` - Returns whether the buffer has been closed.
  - `$ReadView` is now an interface that defines the buffer data and how to close the view

**`com.mojang.blaze3d.font.SheetGlyphInfo#upload`** now takes in a `GpuTexture`

**`com.mojang.blaze3d.opengl`**
- `DirectStateAccess` - An interface that creates and binds data to some framebuffer.
  - `$Core` - An implementation of DSA that modifies the framebuffer without binding them to the context.
  - `$Emulated` - An abstraction over DSA that still binds the context.
- `GlBuffer` - An implementation of the `GpuBuffer` for OpenGL.
- `GlCommandEncoder` - An implementation of the `CommandEncoder` for OpenGL.
- `GlDebugLabel` - A labeler for handling debug references to GL-specified data structures.
- `GlDevice` - An implementation of `GpuDevice` for OpenGL.
- `GlRenderPass` - An implementation of `RenderPass` for OpenGL.
- `GlRenderPipeline` - An implementation of `CompiledRenderPipeline` for OpenGL.
- `GlTexture` - An implementation of `GpuTexture` for OpenGL.
- `VertexArrayCache` - A cache for binding and uploading a vertex array to the OpenGL pipeline.

**`com.mojang.blaze3d.pipeline`**
- `BlendFunction` - A class that holds the source and destination colors and alphas to apply when overlaying pixels in a target. This also holds all vanilla blend functions.
- `CompiledRenderPipeline` - An interface that holds the pipeline with all necessary information to render to the screen.
- `RenderPipeline` - A class that contains everything required to render some object to the screen. It acts similarly to a render state before being applied.
- `RenderTarget` now takes in a string representing the name of the target
  - `colorTextureId` -> `colorTexture`, now a `GpuTexture` / Same with `getColorTextureId` -> `getColorTexture`
  - `depthBufferId` -> `depthTexture`, now a `GpuTexture` / Same with `getDepthTextureId` -> `getDepthTexture`
  - `filterMode` is now a `FilterMode` / Same with `setFilterMode` for the int parameter
  - `blitAndBlendToScreen` no longer takes in the viewport size parameters
  - `framebufferId` is removed
  - `checkStatus` is removed
  - `bindWrite`, `unbindWrite`, `setClearColor` is removed
  - `blitToScreen` no longer takes in any parameters
  - `blitAndBlendToScreen` -> `blitAndBlendToTexture`, not one-to-one
  - `clear` is removed
  - `unbindRead` is removed

**`com.mojang.blaze3d.platform`**
- `DepthTestFunction` - An enum representing the supported depth tests to apply when rendering a sample to the framebuffer.
- `DisplayData` is now a record
  - `withSize` - Creates a new instance with the specified width/height.
  - `withFullscreen` - Creates a new instance with the specified fullscreen flag.
- `FramerateLimitTracker`
  - `getThrottleReason` - Returns the reason that the framerate of the game was throttled.
  - `isHeavilyThrottled` - Returns whether the current throttle significantly impacts the game speed.
  - `$FramerateThrottleReason` - The reason the framerate is throttled.
- `GlConst` -> `com.mojang.blaze3d.opengl.GlConst`
  - `#toGl` - Maps some reference object to its associated OpenGL code.
- `GlDebug` -> `com.mojang.blaze3d.opengl.GlDebug`
  - `enableDebugCallback` now takes in a set of the enabled extensions.
- **`GlStateManager` -> `com.mojang.blaze3d.opengl.GlStateManager`**
  - `_blendFunc`, `_blendEquation` is removed
  - `_glUniform2(int, IntBuffer)`, `_glUniform4(int, IntBuffer)` is removed
  - `_glUniformMatrix2(int, boolean, FloatBuffer)`, `_glUniformMatrix3(int, boolean, FloatBuffer)` is removed
  - `_glUniformMatrix4(int, boolean, FloatBuffer)` -> `_glUniformMatrix4(int, FloatBuffer)`, transpose is now always false
  - `_glGetAttribLocation` is removed
  - `_glMapBuffer` is removed
  - `_glCopyTexSubImage2D` is removed
  - `_glBindRenderbuffer`, `_glDeleteRenderbuffers` is removed
  - `glGenRenderbuffers`, `_glRenderbufferStorage`, `_glFramebufferRenderbuffer` is removed
  - `_texParameter(int, int, float)` is removed
  - `_genTextures`, `_deleteTextures` is removed
  - `_texSubImage2D` now has an overload that takes in an `IntBuffer` instead of a long for the pixel data
  - `upload` is removed
  - `_stencilFunc`, `_stencilMask`, `_stencilOp`, `_clearStencil` is removed
  - `_getTexImage` is removed
  - `_glDrawPixels`, `_readPixels` is removed
  - `$CullState#mode` is removed
  - `$DestFactor` -> `DestFactor`, codes are removed to be called through `GlConst#toGl`
  - `$FramebufferState` enum is removed
  - `$LogicOp` -> `LogicOp`, codes are removed to be called through `GlConst#toGl` / All but `OR_REVERSE` is removed / `NONE` - Performs no logic operation.
  - `$PolygonOffsetState#line` is removed
  - `$SourceFactor` -> `SourceFactor`, codes are removed to be called through `GlConst#toGl`
  - `$StencilFunc`, `$StencilState` class is removed
  - `$Viewport` enum is removed
- `GlUtil` class is removed
  - `getVendor`, `getRenderer`, `getOpenGlVersion` (now `getVersion`) have been moved to instance abstract methods on `GpuDevice`
  - `getCpuInfo` -> `GLX#_getCpuInfo`
- `GLX`
  - `getOpenGLVersionString` is removed
  - `_init` -> `_getCpuInfo`, not one-to-one
  - `_renderCrosshair`, `com.mojang.blaze3d.systems.RenderSystem#renderCrosshair` -> `net.minecraft.client.gui.components.DebugScreenOverlay#render3dCrosshair`, not one-to-one
- `PolygonMode` - A enum that defines how the polygons will render in the buffer.
- `NativeImage`
  - constructor is now public
  - `upload` is removed
  - `getPointer` - Returns the pointer to the image data.
  - `setPixelABGR` is now public
  - `applyToAllPixels` is removed
  - `downloadTexture`, `downloadDepthBuffer` is removed
  - `flipY` is removed
  - `setPackPixelStoreState`, `setUnpackPixelStoreState` is removed
  - `$InternalGlFormat` enum is removed
  - `$Format` no longer contains the GL codes, now in `GlConst#toGl`
- `TextureUtil`
  - `generateTextureId`, `releaseTextureId` is removed
  - `prepareImage` is removed
  - `writeAsPNG` now takes in a `GpuTexture` instead of the direct three integers / The overload without the `IntUnaryOperator` is removed

**`com.mojang.blaze3d.resource`**
- `RenderTargetDescriptor` now takes in an integer representing the color to clear to
- `ResourceDescriptor`
  - `prepare` - Prepares the resource for use after allocation.
  - `canUsePhysicalResource` - Typically returns whether a descriptor is already allocated with the same information.

**`com.mojang.blaze3d.shaders`**
- `AbstractUniform` -> `com.mojang.blaze3d.opengl.AbstractUniform`
  - `setSafe` methods are removed
  - `setMat*` methods are removed
  - `set(Matrix3f)` is removed
- `CompiledShader` -> `com.mojang.blaze3d.opengl.GlShaderModule`
  - `$Type` -> `com.mojang.blaze3d.shaders.ShaderType`
- `Uniform` -> `com.mojang.blaze3d.opengl.Uniform`
  - Constructor now takes in a `$Type` instead of the count and an integer representing the type
  - `UT_*` fields are removed
  - `setFromConfig(ShaderProgramConfig.Uniform)` is removed
  - `getTypeFromString` is removed
  - `getType` now returns a `$Type`
  - `set(int, float)` is removed
  - `setSafe` is now private
  - `$Type` - Holds the type name as well as how many values it holds.
  - `getLocation` is removed
  - `getCount` -> `$Type#count`
  - `getIntBuffer`, `getFloatBuffer` is removed
  - `$Type` -> `com.mojang.blaze3d.shaders.UniformType`

**`com.mojang.blaze3d.systems`**
- `CommandEncoder` - An interface that defines how to encode various commands to the underlying render system, such as creating a pass, clearing and writing textures, or reading from the buffer.
- `GpuDevice` - An interface that defines the device or underlying render system used to draw to the screen. This is responsible for creating the buffers and textures while compiling any pipelines.
- `RenderPass` - An interface that defines how a given pass is rendered to some buffer using the underlying render system. This allows binding any samplers and setting the required uniforms.
- **`RenderSystem`**
  - `isOnRenderThreadOrInit`, `assertOnRenderThreadOrInit` is removed
  - `recordRenderCall`, `replayQueue` is removed
  - **`blendFunc`, `blendFuncSeparate`, `blendEquation` is removed**
  - `texParameter`, `deleteTexture`, `bindTextureForSetup` is removed
  - `stencilFunc`, `stencilMask`, `stencilOp` is removed
  - `clearDepth` is removed
  - `glBindBuffer`, `glBindVertexArray`, `glBufferData`, `glDeleteBuffers` is removed
  - `glUniform1i` is removed
  - `glUniform1`, `glUniform2`, `glUniform3`, `glUniform4` is removed
  - `glUniformMatrix2`, `glUniformMatrix3`, `glUniformMatrix4` is removed
  - `setupOverlayColor` now takes in a `GpuTexture` instead of two ints
  - `beginInitialization`, `finishInitialization` is removed
  - `renderThreadTesselator` is removed
  - **`setShader`, `clearShader`, `getShader` is removed**
  - **`setShaderTexture` now takes in a `GpuTexture` instead of a bind address**
  - **`getShaderTexture` now returns a `GpuTexture` or null if not present**
  - `pixelStore`, `readPixels` is removed
  - `queueFencedTask`, `executePendingTasks` - Handles sending tasks that run on the GPU asyncronously.
  - `SCISSOR_STATE` - Holds the main scissor state.
  - `disableDepthTest`, `enableDepthTest` is removed
  - **`depthFunc`, `depthMask` is removed**
  - `enableBlend`, `disableBlend` is removed
  - `neableCull`, `disableCull` is removed  *(sic — typo in the primer)*
  - `polygonMode`, `enablePolygonOffset`, `disablePolygonOffset`, `polygonOffset` is removed
  - `enableColorLogicOp`, `disableColorLogicOp`, `logicOp` is removed
  - `bindTexture`, `viewport` is removed
  - `colorMask`, `clearColor`, `clear` is removed
  - `setupShaderLights(CompiledShaderProgram)` is removed
  - `getShaderLights` - Returns the vectors representing the block and sky lights.
  - `drawElements`, `getString` is removed
  - `initRenderer` now takes in the window pointer, the default shader source, and a boolean of whether to use debug labels
  - `setupDefaultState` no longer takes in any parameters
  - `maxSupportTextureSize` is removed
  - `glDeleteVertexArrays` is removed
  - `defaultBlendFunc` is removed
  - **`setShaderTexture` is removed**  *(listed a second time in the primer — appears both as "now takes in a GpuTexture" and "is removed"; the "is removed" line likely refers to a specific overload)*
  - `getQuadVertexBuffer` - Returns a vertex buffer with a quad bound to it.
  - `getDevice`, `tryGetDevice` - Returns the `GpuDevice` representing the underlying render system to use.
  - `getCapsString` is removed
  - `activeTexture` is removed
  - `setModelOffset`, `resetModelOffset`, `getModelOffset` - Handles the offset to apply to a model when rendering for the uniform `ModelOffset`. Typically for clouds and world borders.
  - `$AutoStorageIndexBuffer#bind` -> `getBuffer`, not one-to-one
  - `$GpuAsyncTask` - A record that holds the callback and fence object used to sync information to the GPU.
- `ScissorState` - A class which holds the part of the screen to render.

**`com.mojang.blaze3d.textures`**
- `AddressMode` - The mode set for how to render a texture to a specific location.
- `FilterMode` - The mode set for how to render a texture whenever the level-of-detail function determines how the texture should be maximized or minimized.
- `GpuTexture` - A texture that is bound and written to the GPU as required.
- `TextureFormat` - Specifies the format that the texture should be allocated with.

**`com.mojang.blaze3d.vertex`**
- `PoseStack`
  - `mulPose(Quaternionf)`, `rotateAround` now takes in a `Quaternionfc` instead of a `Quaternionf`
  - `clear` -> `isEmpty`
  - `mulPose(Matrix4f)` -> `mulPose(Matrix4fc)`
  - `$Pose`
    - `computeNormalMatrix` is now private
    - `transformNormal` now takes in a `Vector3fc` as its first parameter
    - `translate`, `scale`, `rotate`, `rotateAround`, `setIdentity`, `mulPose` are now available on the pose itself in addition to the stack
- **`VertexBuffer` -> `com.mojang.blaze3d.buffers.GpuBuffer`, not one-to-one** / Some logic is also moved to `VertexFormat`
- `VertexFormat`
  - `bindAttributes` is removed
  - `setupBufferState`, `clearBufferState`, `getImmediateDrawVertexBuffer` -> `uploadImmediateVertexBuffer`, `uploadImmediateIndexBuffer`; not one-to-one
  - `$IndexType` no longer stores the GL codes, now in `GlConst#toGl`
  - `$Mode` no longer stores the GL codes, now in `GlConst#toGl`
- `VertexFormatElement`
  - `setupBufferState` is removed
  - `$Type` no longer stores the GL codes, now in `GlConst#toGl`
  - `$Usage` no longer stores the GL function calls, now in `VertexArrayCache#setupCombinedAttributes`

**`com.mojang.math`**
- `MatrixUtil`
  - `isIdentity`, `isPureTranslation`, `isOrthonormal` now take in a `Matrix4fc`
  - `checkProperty` - Checks if the provided property is represented within the matrix.
- `OctahedralGroup`
  - `transformation` now returns a `Matrix3fc`
  - `fromAnges` -> `fromXYAngles`, not one-to-one
- `Quadrant` - An enum that contains rotations in 90 degree increments.
- `SymmetricGroup3#transformation` now returns a `Matrix3fc`
- `Transformation`
  - now takes in a `Matrix4fc`
  - `getMatrix` now returns a `Matrix4fc`
  - `getMatrixCopy` - Returns a deep copy of the current matrix.

**`net.minecraft.client.gui.font.FontTexture`** now takes in a supplied label string

**`net.minecraft.client.main.GameConfig`** now takes in a boolean representing whether to render debug labels

**`net.minecraft.client.renderer`**
- `CloudRenderer#render` no longer takes in the `Matrix4f`s used for projection or posing
- **`CompiledShaderProgram` -> `com.mojang.blaze3d.opengl.GlProgram`**
  - `link` now takes in a string for the shader name
  - `setupUniforms` now take in the list of `$UniformDescription`s along with a list of names used by the samplers
  - `getUniformConfig` is removed
  - `bindSampler` now takes in a `GpuTexture` instead of the integer bind identifier
  - `parseUniformNode` is removed
- **`CoreShaders` -> `RenderPipelines`, not one-to-one**
- `LightTexture#getTarget` - Returns the `GpuTexture` that contains the light texture for the current level based on the player.
- **`PostChain`**
  - `load` no longer takes in the `ShaderManager`, now taking in a `ResourceLocation` representing the name of the chain
  - `addToFrame`, `process` now takes in a `RenderPass` consumer to apply any additional settings to the pass to render
  - `setUniform` is removed
  - `setOnRenderPass` - Sets the uniform within the post chain on the `RenderPass` for use in the shaders.
- **`PostChainConfig`**
  - `$Pass` now takes in the ids of the vertex and fragment shader instead of the program id
  - `referencedTargets` - Returns the targets referenced in the pass to apply.
  - `program` is removed
  - `$Uniform` now takes in the type of the uniform along with an optional list of floats if the value does not need to be overridden
- **`PostPass`**
  - no longer takes in the `CompiledShaderProgram`, now taking in the `RenderPipeline` instead of a string representing the name of the pass
  - `addToFrame` now takes in a `RenderPass` consumer to apply any additional settings to the pass to render
  - `getShader` is removed
  - `$Input#bindTo` now takes in a `RenderPass` instead of the `CompiledShaderProgram`
- `RenderStateShard`
  - `$LayerStateShards` using polygon offsets have been removed
  - `getName` - Returns the name of the shard.
  - `$TransparencyStateShard` class is removed / Now handled through `BlendFunction`
  - `$ShaderStateShard` class is removed / Directly referred to by the `VertexBuffer`
  - `$CullStateShard` class is removed / Now handled as a setting on the `RenderPipeline`
  - `$DepthTestStateShard` class is removed / Now handled through `DepthTestFunction`
  - `$WriteMaskStateShard` class is removed / Now handled as a setting on the `RenderPipeline`
  - `$ColorLogicStateShard` class is removed / Now handled as a setting on the `RenderPipeline`
  - `$OutputStateShard` now takes in a supplied `RenderTarget` instead of the runnables for the startup and teardown states
- **`RenderType`**
  - no longer takes in the `VertexFormat` or `VertexFormat$Mode`
  - `SKY`, `END_SKY`, `sky`, `endSky`, `stars` is removed
  - `ENTITY_OUTLINE_BLIT`, `entityOutlineBlit` is removed
  - `PANORAMA`, `panorama` is removed
  - `CREATE_LIGHTMAP`, `createLightmap` is removed
  - `createClouds`, `flatClouds`, `clouds`, `cloudsDepthOnly` is removed
  - `worldBorder` is removed
  - `debugLine` - Returns the `RenderType` associated with the debug line.
  - `entityOutlineBlit` - Returns the `RenderType` used for rendering an entity outline.
  - `panorama` - Returns the `RenderType` used for rendering panorama mode.
  - `createLightmap` - Returns the `RenderType` used for rendering the lightmap texture.
  - `create` no longer takes in the `VertexFormat` or `VertexFormat$Mode`, instead the `RenderPipeline`
  - `getRenderTarget`, `getRenderPipeline` - Returns the target and pipeline used for rendering.
  - `format`, `mode`, `draw` are now abstract
  - `$CompositeStateBuilder` methods are now protected
  - `$OutlineProperty` is now protected
- `ShaderDefines$Builder#define` now has an overload that takes in an integer
- **`ShaderManager`**
  - `SHADER_INCLUDE_PATH` is now private
  - `MAX_LOG_LENGTH` is removed
  - `preloadForStartup` is removed, replaced by `GpuDevice#precompilePipeline`
  - `getProgram`, `getProgramForLoading` -> `getShader`, not one-to-one
  - `linkProgram` now takes in a `RenderPipeline` instead of a `ShaderProgram` and `ShaderProgramConfig`
  - `$CompilationCache#getOrCompileProgram`, `getOrCompileShader` -> `getShaderSource`, not one-to-one
  - `$Configs` no longer takes in the map of programs
  - `$ShaderCompilationKey` record is removed
- **`ShaderProgram`, `ShaderProgramConfig` -> `RenderPipeline`, not one-to-one**
- `SkyRenderer#renderDarkDisc` no longer takes in the `PoseStack`

### 1.7 What 1.21.5 does NOT contain (explicit negatives, verified)

- **No** `GlslPreprocessor` anywhere. Algolia full-index query for `GlslPreprocessor` returns `{"nbHits":0}`. The primer never mentions shader-source injection or `ShaderInstance` source patching.
- **No** `MainTarget` class mentioned in 1.21.5. (Confirmed absent from the fetched 1.21.5 material.)
- **No** `BindGroup`. (That term belongs to a different engine's abstraction.)
- **No** shader `program/*.json` layout is described. The primer's only shader-layout statements are `shaders/<name>.vsh|.fsh` (pipeline) and `shaders/post/<name>.vsh|.fsh` (post).
- **No Vulkan backend.** The word "Vulkan" appears exactly once, aspirational (see §1.2).

---

## 2. Primer 1.21.6 (1.21.5 -> 1.21.6) — Blaze3d Changes

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.6/index.md
Rendered: https://docs.neoforged.net/primer/docs/1.21.6/

Headings present: `## GUI Changes`, `## Blaze3d Changes` (with `### Buffer Slices`, `### Uniform Rework`, `### Fog Environments`, `### Render Pass Scissoring now only OpenGL`), then `## Tag Providers: Appender Rewrite`, `## Generic Encoding and Decoding: Replacing Direct NBT Access`.

### 2.1 `## Blaze3d Changes` intro (verbatim)

> Just like the previous update Blaze3d has new redesigns that change how rendering is handled.

### 2.2 `### Buffer Slices` (verbatim)

> The most important change within the rendering system is the use of `GpuBufferSlice`s. As the name implies, it simply takes some slice of a `GpuBuffer`, using an `offset` to indicate the starting index, and the `length` to indicate its size. When dealing with anything rendering related, even when passing to a shader, you will almost always be dealing with a `GpuBufferSlice`. A way to think about it is that the buffer is just some arbitrary list of object where a slice represents a specific object.
>
> To create a slice, simply call `GpuBuffer#slice`, optionally providing the starting index and length. In most instances, you will not be calling this method directly, instead dealing with other methods that call it for you. Note that as you are generally writing data to these slices, they should be constructed before a try-with-resources `RenderPass` is created.

### 2.3 `### Uniform Rework` (verbatim)

> The uniform system has been completely overhauled such that, unless you are familiar with specific features, it might seem completely foreign. In a nutshell, uniforms are now stored as either interface objects, a texel buffer, or a sampler. These are populated either by `GpuBuffer`s or `GpuBufferSlice`s.
>
> #### Uniform Types
>
> A uniform is currently represented as one of two `UniformType`s: either as an `UNIFORM_BUFFER`/interface block, or a `TEXEL_BUFFER`. When setting up a pipeline and calling `withUniform`, you must specify your uniform with one of the above types. If you choose to use a texel buffer, you must also specify the format of the texture data. Samplers have not changed.
>
> ```java
> public static final RenderPipeline EXAMPLE_PIPELINE = RenderPipeline.builder(...)
>     .withUniform("ExampleUniform", UniformType.UNIFORM_BUFFER)
>     .withUniform("ExampleTexel", UniformType.TEXEL_BUFFER, TextureFormat.RED8I)
>     .build();
> ```

Interface-block GLSL example is given with the comment:

> ```glsl
> // In assets/examplemod/shaders/core/example_shader.json
> layout(std140) uniform ExampleUniform {
>     vec4 Param1;
>     float Param2;
> };
> ```

> The uniform block can either be inside the same file as the main function or, if it will be reused, as a `moj_import`, where the uniform file is within `assets/<modid>/shaders/include/<path>`. Note that any shaders used before resource packs are loaded cannot use `moj_import`.

*(Note: the primer's comment says `shaders/core/...json` but the file it is describing is a GLSL source file — the comment is a leftover from the pre-1.21.5 layout. This is a documented inconsistency in the primer itself.)*

> Post Effects define their interface blocks in the `uniform` input:
>
> ```json5
> // In assets/examplemod/post_effect/example_post_effect.json
> // Inside a 'passes' object
> {
>     "vertex_shader": "...",
>     "uniforms": {
>         "ExampleUniform": [
>             { "name": "Param1", "type": "vec4", "value": [ 0.0, 0.0, 0.0, 0.0 ] },
>             { "name": "Param2", "type": "float", "value": 0 }
>         ]
>     }
> }
> ```
>
> Supported parameter types: `int`, `ivec3`, `float`, `vec2`, `vec3`, `vec4`, `matrix4x4 (mat4)`; "Dynamic values from the codebase are no longer supported".

**Shader layout (1.21.6):** include files live at `assets/<modid>/shaders/include/<path>` and are pulled in with `moj_import`. Post effects now live at `assets/<ns>/post_effect/<name>.json` (note: **`post_effect`, not `post`**).

### 2.4 `### Render Pass Scissoring now only OpenGL` (verbatim)

> The scissoring state has been removed from the generic pipeline code, now only accessible through the OpenGL implementation. Generic region handling has been delegated to `CommandEncoder#clearColorAndDepthTextures`. Note that this does not affect the existing `ScreenRectangle` system that handles the blit facing scissoring.

### 2.5 1.21.6 bullet list — key entries (verbatim)

`com.mojang.blaze3d.buffers`
- `BufferType` enum is removed
- `BufferUsage` enum is removed
- `GpuBuffer` now takes two ints represent the size and usage, the type is no longer specified
  - `type` is removed
  - `usage` now returns an int
  - `slice` - Returns a record representing a slice of some buffer. The actual buffer is not modified in any way.
  - `$ReadView` -> `$MappedView`
- `GpuBufferSlice` - A record that represents a slice of some buffer by holding a reference to the entire buffer, an offset index of the starting point, and the length of the slice.
- `GpuFence` is now an interface, with its OpenGL implementation moved to `blaze3d.opengl.GlFence`
- `Std140Builder` - A buffer inserter for an interface block laid out in the `std140` format. This is used for the uniform blocks within shaders.
- `Std140SizeCalculator` - An object used for keeping track of the size of some interface block.

`com.mojang.blaze3d.opengl`
- `AbstractUniform` is removed, replaced by the `Std140*` classes
- `BufferStorage` - A class that create buffers given its types. Its implementations define its general usage.
- `DirectStateAccess`: `createBuffer`, `bufferData`, `bufferSubData`, `bufferStorage`, `mapBufferRange`, `unmapBuffer`
- `GlBuffer` now takes in a `DirectStateAccess`, two ints, and a `ByteBuffer` instead of a `GlDebugLabel` and its `Buffer*` enums
  - `initialized` is removed
  - `persistentBuffer` - Holds a reference to an immutable section of some buffer.
  - `$ReadView` -> `$GlMappedView`
- `GlCommandEncoder`
  - `executeDrawMultiple` now takes in a collection of strings that defines the list of required uniforms and a generic indicating the object of the draw call
  - `executeDraw` now takes in two `int`, the number of instances of the specified range of indices to be rendered and the base vertex
- `GlConst#toGl` for `BufferType` and `BufferUsage` -> `bufferUsageToGlFlag`, `bufferUsageToGlEnum`
- `GlDebugLabel#pushDebugGroup`, `popDebugGroup` - Profiler commands for grouping similar calls.
- `GlDevice`
  - `USE_GL_ARB_buffer_storage` - Sets the extension flag for `GL_ARB_buffer_storage`.
  - `getBufferStorage` - Returns the storage responsible for creating buffers.
- `GlProgram`
  - `Uniform` fields have been removed
  - `safeGetUniform`, `setDefaultUniforms` are removed
  - `bindSampler` is removed
  - `getSamplerLocations`, `getSamplers`, `getUniforms` -> `getUniforms` which returns a map of names to their `Uniform` objects
- `GlRenderPass`
  - `uniforms` now is a `HashMap<String, GpuBufferSlice>`
  - `dirtSamplers` is removed
  - `samplers` map now holds a `GpuTextureView` value
  - `pushedDebugGroups` - Returns the number of groups pushed onto the stack. No debug groups must be open for the render pass to be closed.
  - `isScissorEnabled` - Returns whether the scissoring state is enabled which crops the area that is rendered to the screen.
  - `getScissorX`, `getScissorY`, `getScissorWidth`, `getScissorHeight`
- `GlTexture` now takes in an additional integer for the usage and depth/layers
  - `flushModeChanges` now takes in an `int` which represents the texture target
  - `addViews`, `removeViews` - Manages the views looking at a texture for some mip levels.
- `GlTextureView` - A view implementation of a texture for some mip levels.
- `Uniform` is now a sealed interface which is implemented as either a buffer object, texel buffer, or a sampler

`com.mojang.blaze3d.pipeline`
- `BlendFunction#PANORAMA` is removed
- `CompiledRenderPipeline#containsUniform` is removed
- `RenderPipeline`
  - `$Builder#withUniform` now has an overload that can take in a `TextureFormat`
  - `$UniformDescription` now takes in a nullable `TextureFormat`
- `RenderTarget`
  - `colorTextureView`, `depthTextureView`, `getColorTextureView`, `getDepthTextureView` - A view of the current color and depth textures.
  - `blitAndBlendToTexture` now takes in a `GpuTextureView` instead of a `GpuTexture`

`com.mojang.blaze3d.platform.Lightning` is now `AutoCloseable`
- `setup*` -> `setupFor`, an instance method that takes in an `$Entry`
- `updateLevel` - Updates the lighting buffer, taking in whether to use the nether diffused lighting.

`com.mojang.blaze3d.shaders`
- `FogShape` -> `net.minecraft.client.renderer.fog.FogData`, not one-to-one
- `UniformType` now only holds two types: `UNIFORM_BUFFER`, or `TEXEL_BUFFER`, and no longer takes in a count

`com.mojang.blaze3d.systems`
- `CommandEncoder`
  - `clearColorAndDepthTextures` now has an overload that takes in four `int`s representing the region to clear the texture information within
  - `writeToBuffer`, `mapBuffer(GpuBuffer, int, int)` now take in a `GpuBufferSlice` instead of a `GpuBuffer`
  - `createFence` - Creates a new sync fence.
  - `createRenderPass` now takes in a `Supplier<String>`, used for determining the name of the pass to use as a debug group, and `GpuTextureView`s instead of `GpuTexture`s
  - `writeToTexture` now takes in an additional `int` representing the depth or layer to write to
  - `presentTexture` now takes in a `GpuTextureView` instead of a `GpuTexture`
- `GpuDevice`
  - `createBuffer` no longer takes in a `BufferUsage`, with `BufferType` replaced by an `int`
  - `getUniformOffsetAlignment` - Returns the uniform buffer offset alignment.
  - `createTexture` now takes in additionals `int` for the usage and the number of depth/layers, see `GpuTexture` constants
  - `createTextureView` - Creates a view of a texture for a certain range of mip levels.
- `RenderPass`
  - `enableScissor` is removed
  - `bindSampler` can now take in a nullable `GpuTextureView`
  - `setUniform` can either take in a `GpuBuffer` or `GpuBufferSlice` instead of the raw inputs
  - `drawIndexed` now takes the following `int`s: the base vertex, the start index, the number of elements, and the prim count
  - `drawMultipleIndexed` now takes in a collection of strings that defines the list of required uniforms and a generic indicating the object of the draw call
  - `pushDebugGroup`, `popDebugGroup` - Profiler commands for grouping similar calls.
  - `$UniformUploader#upload` now takes in a `GpuBufferSlice` instead of an array of `float`s
  - `$Draw` now has a generic that passed in to upload any uniforms to the buffer
- **`RenderSystem`**
  - `SCISSOR_STATE` -> `scissorStateForRenderTypeDraws`, now private
    - Accessible through `getScissorStateForRenderTypeDraws`
  - `enableScissor`, `disableScissor` -> `enableScissorForRenderTypeDraws`, `disableScissorForRenderTypeDraws`, not one to one
  - `PROJECTION_MATRIX_UBO_SIZE` - Returns the size of the projection matrix uniform
  - `setShaderFog`, `getShaderFog` now deals with a `GpuBufferSlice`
  - `setShaderGlintAlpha`, `getShaderGlintAlpha` is removed
  - `setShaderLights`, `getShaderLights` now deals with a `GpuBufferSlice`
  - `setShaderColor`, `getShaderColor`
  - `setup*Lighting` methods are removed
  - `setProjectionMatrix`, `getProjectionMatrix` (now `getProjectionMatrixBuffer`) now deals with a `GpuBufferSlice`
  - `setShaderGameTime`, `getShaderGameTime` -> `setGlobalSettingsUniform`, `getGlobalSettingsUniform`; not one-to-one
  - `getDynamicUniforms` - Returns a list of uniforms to write for the shader.
  - `bindDefaultUniforms` - Binds the default uniforms to be used within a `RenderPass`
  - `outputColorTextureOverride`, `outputDepthTextureOverride` are now `GpuTextureView`s
  - `setupOverlayColor`, `setShaderTexture`, `getShaderTexture` now operate on `GpuTextureView`s instead of `GpuTexture`s

`com.mojang.blaze3d.textures`
- `GpuTexture` now takes in an int representing the usage flags and number of depth/layers
  - `usage` - The flags that define how the texture can be used.
  - `depthOrLayers`, `getDepthOrLayers` - Defines how many layers or depths are available for a given texture. This is meant as a generic count for available texture encodings. Only cubemap support is currently available (meaning the layer count must be a multiple of 6).
- `GpuTextureView` - A view of some texture for a range of mip levels.
- `TextureFormat#RED8I` - An 8-bit signed integer handling the red color channel.

`com.mojang.blaze3d.vertex`
- `ByteBufferBuilder` now takes in a long for the maximum capacity
  - `exactlySized` - Returns a buffer that is its maximum capacity. This should be called over the public constructor.
- `DefaultVertexFormat#EMPTY` - A vertex format with no elements.

`net.minecraft.client.renderer`
- `CachedOrthoProjectionMatrixBuffer` - An object that caches the orthographic projection matrix, rebuilding if the width or height of the screen changes.
- `CachedPerspectiveProjectionMatrixBuffer` - An object that caches the perspective projection matrix, rebuilding if the width, height, or field of view changes.
- `CloudRenderer`: `FLAG_INSIDE_FACE`, `FLAG_USE_TOP_COLOR` now private; `RADIUS_BLOCKS` removed; `endFrame` - Ends the current frame being rendered to by constructing a fence.
- `CubeMap` is now `AutoCloseable` / `render` no longer takes in the float for the partial tick.
- `DynamicUniforms` - A class that writes the uniform interface blocks to the buffer for use in shaders.
- `DynamicUniformStorage` - A class that holds the uniforms within a slice of a mappable ring buffer.
- `FogParameters` record is removed, now stored in a general `GpuBufferSlice`
- `FogRenderer` now implements `AutoCloseable` -> `.fog.FogRenderer`
- `GameRenderer`: `getGlobalSettingsUniform`, `getLighting`, `setLevel`
- `GlobalSettingsUniform` - An object fod handling the uniform holding the current game settings.
- `LevelRenderer`: `endFrame`; `renderLevel` now takes in a `GpuBufferSlice`, the fog vector, and whether the current position is foggy instead of the `GameRenderer`
- `MappableRingBuffer` - An object that contains three buffers that are written to as required, then rotates to the next buffer to use on end. These are synced using fences.
- `PerspectiveProjectionMatrixBuffer` - An object that holds the projection matrix uniform.
- **`PostChain`**
  - `load` now takes in the `CachedOrthoProjectionMatrixBuffer`
  - `addToFrame` no longer takes in the `Consumer<RenderPass>`
  - `process` no longer takes in the `Consumer<RenderPass>`
- **`PostChainConfig`**
  - `$FixedSizedTarget` record is removed
  - `$FullScreenTarget` record is removed
  - `$InternalTarget` is now a record which can take in an optional width, height, whether the target is persistent, and the clear color to use
  - `$Pass#uniforms` is now a `Map<String, List<UniformValue>>`
  - `$Uniform` record is removed
- **`PostPass`** is now `AutoCloseable`, taking in a `Map<String, List<UniformValue>>` for the uniforms along with a list of `PostPass$Input`s
  - `addToFrame` no longer takes in the `Consumer<RenderPass>`, with the `Matrix4f` passed in as a `GpuBufferSlice`
  - `$Input`: `bindTo` is removed; `texture` - Constructs a `GpuTexture` for the input given the map of resources; `samplerName` - Returns the name of the sampler.
- `UniformValue` - An interface that represents a uniform stored within an interface block.

`net.minecraft.client.renderer.texture`
- `AbstractTexture`: `setUseMipmaps`; `textureView`, `getTextureView` - Represents the current texture view.
- `CubeMapTexture` - A cubemap compatible texture, textures are expected to have suffixes `_0` to `_5`.
- `ReloadableTexture#doLoad` is now protected

`net.minecraft.world.level.material.FogType`
- `DIMENSION_OR_BOSS` - Fog for dimension special effects or bosses.
- `ATMOSPHERIC` - Default fog type.

---

## 3. Primer 1.21.9 (1.21.8 -> 1.21.9)

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.9/index.md
Rendered: https://docs.neoforged.net/primer/docs/1.21.9/

Headings: `## Pack Changes`, `## The Debugging Overhaul`, `## Debug Screens`, `## Feature Submissions: The Movie` (with `### Submission and Rendering`, `### Entity Models`, `### Entity Renderer`, `### Block Entity Renderer`, `### Special Item Models`, `### Particles`, `### Atlas Handler Consolidation`), `## The Font Glyph Pipeline`.

**Note: 1.21.9 contains NO section on GlStateManager, RenderSystem setShader/blendFunc/depthFunc, PostChain, ShaderInstance, or Vulkan.** Its rendering content is the *entity/block-entity/particle render-state ("feature submission") rework* and one small shader-JSON bullet list.

### 3.1 `## Feature Submissions: The Movie` (verbatim intro)

> The entirety of the rendering pipeline, from entities to block entities to particles, has been reworked into a submission / render phase system known as features. This guide will go over the basics of the feature system itself followed by how each major type are implemented using it.
>
> ### Submission and Rendering
>
> The feature system, like GUIs, is broken into two phases: submission and rendering. The submission phase is handled by the `SubmitNodeCollector`, which basically collects the data required to abstractly render an object to the screen. This is all done through the `submit*` methods, which generally take a `PoseStack` to place the object in 3D space, and any other required data such as the model, state, render type, etc.

Method table (verbatim):

| Method | Parameters |
|---|---|
| `submitHitbox` | A pose stack, render state of the entity, and the hitboxes render state |
| `submitShadow` | A pose stack, the shadow radius, and the shadow pieces |
| `submitNameTag` | A pose stack, an optional position, the Y offset, the text component, if the text should be seethrough (like when sneaking), light coordinates, and the camera render state |
| `submitText` | A pose stack, the XY offset, the text sequence, whether to add a drop shadow, the font display mode, light coordinates, color, background color, and outline color |
| `submitFlame` | A pose stack, render state of the entity, and a rotation quaternion |
| `submitLeash` | A pose stack and the leash state |
| `submitModel` | A pose stack, entity model, render state, render type, light coordinates, overlay coordinates, tint color, an optional texture, outline color, and an optional crumbling overlay |
| `submitModelPart` | A pose stack, model part, render type, light coordinates, overlay coordinates, an optional texture, whether to use item glint over entity glint if render type is not transparent, whether to render the glint overlay, tint color, an optional crumbling overlay, and an outline color |
| `submitBlock` | A pose stack, block state, light coordinates, overlay coordinates, and outline color |
| `submitMovingBlock` | A pose stack and the moving block render state |
| `submitBlockModel` | A pose stack, the render type, block state model, RGB floats, light coordinates, overlay coordinates, and outline color |
| `submitItem` | A pose stack, item display context, light coordinates, overlay coordinates, outline color, tint layers, quads, render type, and foil type |
| `submitCustomGeometry` | A pose stack, render type, and a function that takes in the current pose and `VertexConsumer` to create the mesh |
| `submitParticleGroup` | A `SubmitNodeCollector$ParticleGroupRenderer` |

> Technically, the `submit*` methods are provided by the `OrderedSubmitNodeCollector`, of which the `SubmitNodeCollector` extends. This is because features can be submitted to different orders, which function similarly to strata in GUIs. By default, all submit calls are pushed onto order 0. Using `SubmitNodeCollector#order` with some integer and then calling the `submit*` method, you can have an object render before or after all features on a given order. This is stored as an AVL tree, where each order's data is stored in a `SubmitNodeCollection`. With the current default feature rendering order, this is only used in very specific circumstances, such as rendering a slime's outer body or equipment layers.
>
> The render phase is handled through the `FeatureRenderDispatcher`, which renders the object using their submitted feature renderers. ... Currently, for a given order, the features push their vertices like so: shadows, models, model parts, flame animations, entity name tags, arbitrary text, hitboxes, leashes, items, blocks, custom render pipelines, and finally particles. Each order, starting from the smallest number to the largest, will rerun all of the feature renders until it reaches the end of the tree. All submissions are then cleared for next use.

### 3.2 `### Atlas Handler Consolidation` (verbatim intro)

> The atlas handler has had some of its logic modified to consolidate other sprites and change how to obtain a `TextureAtlasSprite`.
>
> First, map decorations, paintings, and GUI sprites are now proper atlases with their own sheets: `Sheets#MAP_DECORATIONS_SHEET`, `PAINTINGS_SHEET`, and `GUI_SHEET` respectively.
>
> Obtaining the `TextureAtlasSprite` from said sheets are now completely routed through the `MaterialSet`: a functional interface that takes in a `Material` (basically a sheet location and texture location), and returns the associated `TextureAtlasSprite`. The `MaterialSet` handles texture grabs for item models, block entity renderers, and entity renderers

### 3.3 1.21.9 shader-JSON bullet (verbatim — this is the ONLY shader-resource mention in 1.21.9)

> - `assets/minecraft/shaders/core`
>     - `blit_screen.json` -> `screenquad.json`, using no-format triangles instead of positioned quads
>     - `position_color_lightmap.*` are removed
>     - `position_color_tex_lightmap.*` are removed

### 3.4 Other 1.21.9 rendering bullets (verbatim, selected)

`com.mojang.blaze3d.vertex`
- `CompactVectorArray` - An holder that smashes a list of float vectors into a single sequential array.
- `MeshData$SortState#centroids` now returns a `CompactVectorArray`
- `VertexSorting`
  - `byDistance` now takes in a `Vector3fc` instead of a `Vector3f`
  - `sort` now takes in a `CompactVectorArray` instead of a `Vector3f[]`

`net.minecraft.client.renderer`
- `OrderedSubmitNodeCollector` - A submission handler for holding elements to be drawn in a given order to the screen whenever the features are dispatched.
- `OutlineBufferSource` no longer takes in any parameters / `setColor` now takes in a single integer
- `ParticleGroupRenderState`, `ParticlesRenderState`, `QuadParticleRenderState`
- `RenderPipelines`
  - `GUI_TEXT` - The pipeline for text in a gui.
  - `GUI_TEXT_INTENSITY` - The pipeline for text intensity when not colored in a gui.
- `RenderStateShard#TRANSLUCENT_TARGET`, `PARTICLES_TARGET` are removed
- `RenderType`
  - **`pipeline` - The `RenderPipeline` the type uses.**
  - `opaqueParticle`, `translucentParticle` are removed
  - `sunriseSunset`, `celestial` are removed
- `SubmitNodeCollection` - An implementation of `OrderedSubmitNodeCollector` that holds the submitted features in separate lists.
- `SubmitNodeCollector` - An `OrderedSubmitNodeCollector` that provides a method to change the current order that an element will be rendered in.
- `SubmitNodeStorage` - A storage of collections held by some order.

`net.minecraft.client.renderer.feature` (new package)
- `BlockFeatureRenderer`, `CustomFeatureRenderer`, `FeatureRenderDispatcher`, `FlameFeatureRenderer`, `HitboxFeatureRenderer`, `ItemFeatureRenderer`, `LeashFeatureRenderer`, `ModelFeatureRenderer`, `ModelPartFeatureRenderer`, `NameTagFeatureRenderer`, `ParticleFeatureRenderer`, `ShadowFeatureRenderer`, `TextFeatureRenderer`

`net.minecraft.client.renderer.state` (new package)
- `BlockBreakingRenderState`, `BlockOutlineRenderState`, `CameraRenderState`, `LevelRenderState`, `ParticleGroupRenderState`, `ParticlesRenderState`, `QuadParticleRenderState`, `SkyRenderState`, `WeatherRenderState`, `WorldBorderRenderState`

`net.minecraft.client.resources`
- `MapDecorationTextureManager` class is removed
- `PaintingTextureManager` class is removed
- `PlayerSkin$Model` -> `PlayerModelType`, not one-to-one
- `TextureAtlasHolder` class is removed

`net.minecraft.client.resources.model`
- `AtlasIds` -> `net.minecraft.data.AtlasIds`
- `AtlasSet` -> `AtlasManager`, not one-to-one
- `Material`: `sprite` -> `MaterialSet#get`; `buffer` now takes in a `MaterialSet`
- `MaterialSet` - A map of material to its atlas sprite.
- `ModelManager` is no longer `AutoCloseable` / `getAtlas` -> `MaterialSet#get` / `updateMaxMipLevel` -> `AtlasManager#updateMaxMipLevel`

`net.minecraft.client.renderer.texture`
- `SkinTextureDownloader` is now an instance class rather than a static method holder
- `SpriteContents` now takes in an optional `AnimationMetadataSection` and `MetadataSectionType$WithValue` list instead of a `ResourceMetadata`
- `SpriteLoader`: `DEFAULT_METADATA_SECTIONS` is removed; `stitch` is now private; `runSpriteSuppliers` is now private; `loadAndStitch` now takes a set of `MetadataSectionType`s instead of a collection
- `TextureAtlasSprite#isAnimated` -> `SpriteContents#isAnimated`

---

## 4. Primer 1.21.10 (1.21.9 -> 1.21.10)

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.10/index.md
Rendered: https://docs.neoforged.net/primer/docs/1.21.10/

**The entire 1.21.10 primer has NO rendering section at all.** It consists only of "Minor Migrations" with a very short list:

> ## Minor Migrations
>
> ### List of Additions
>
> - `net.minecraft.world.entity.decoration.HangingEntity#getPopBox` - Returns the bounding box indicating where the entity will pop off if colliding.
>
> ### List of Changes
>
> `net.minecraft.world.level.block.state.BlockBehaviour#entityInside` now takes in a `boolean` indicating whether the entity is intersecting or inside the block

**Explicit negative:** there is nothing in the 1.21.10 primer about GlStateManager, RenderSystem, RenderPipeline, PostChain, shaders, or Vulkan.

---

## 5. Primer 1.21.11 (1.21.10 -> 1.21.11) — "Oh Hey, Another Rendering Rewrite"

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.11/index.md
Rendered: https://docs.neoforged.net/primer/docs/1.21.11/

Headings include `## The Rename Shuffle` (`### ResourceLocation to Identifier`, `### The util Package`, `### critereon to criterion`, `### Entity and Object Subpackages`), `## Oh Hey, Another Rendering Rewrite`.

### 5.1 `## Oh Hey, Another Rendering Rewrite` (verbatim intro)

> More of the rendering pipeline has been rewritten, with the majority focused on samplers, `RenderType` creation, and mipmaps.
>
> ### The Separation of Samplers
>
> Blaze3d has separated setting the `AddressMode`s and `FilterMode`s when reading texture data into `GpuSampler`. As the name implies, a `GpuSampler` defines how to sample data from a buffer, such as a texture. `GpuSampler` contains four methods: `getAddressModeU` / `getAddressModeV` for determining how the sampler should behave when reading the UV positions (either repeat or clamp), `getMinFilter` / `getMagFilter` for determining how to minify or magnify the texture respectively (either nearest neighbor or linear), `getMaxAnisotropy` for the largest anisotropic filtering level that can be used, and `getMaxLod` for the maximum level-of-detail on a texture.
>
> Samplers can be created via `GpuDevice#createSampler`, but that is not necessary unless you want to specify a different anisotropic filtering level greater than `1`, or a maximum level-of-detail that is not `0` or `1000`. If the default, as there are only 32 possible combinations, vanilla creates all `GpuSampler`s and stores them in a cache, accessible via `RenderSystem#getSamplerCache`, followed by `SamplerCache#getSampler`:

```java
// Raw call
GpuSampler sampler = RenderSystem.getDevice().createSampler(
    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
    FilterMode.LINEAR, FilterMode.NEAREST,
    4f, OptionalDouble.of(0.0)
);

// Sampler cache method
GpuSampler sampler = RenderSystem.getSamplerCache().getSampler(
    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
    FilterMode.LINEAR, FilterMode.NEAREST,
    true
);
```

> To make user of the sampler for a texture, when binding the texture in a render pass (via `RenderPass#bindTexture`), you must now specify the sampler to use in addition to the texture view:
>
> ```java
> try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(...)) {
>     pass.bindTexture("Sampler0", ..., sampler);
> }
> ```
>
> Setting up the post processor has not changed from the user perspective as only clamp to edge address modes may be selected.
>
> ### The `RenderType` Shuffle
>
> Creating a `RenderType` has been reworked to some degree. While most of the features from the previous implementation still exist, they have been changed to match the new rendering system where direct OpenGL is abstracted away and only accessed through their defined pipelines and `RenderSystem`.
>
> #### Existing Types
>
> Existing types have been moved from `RenderType` to `RenderTypes` (e.g., `RenderType#solid` -> `RenderTypes#solid`).
>
> #### Custom Types
>
> Originally, to create a `RenderType`, you would construct a `$CompositeState` using `RenderStateShard`s. ... The new system splits the render definition in two: the `RenderSetup`, and our `RenderType`. The `RenderSetup`, as the name implies, sets up the renderer to be used when drawing to a texture. Teardown is completely removed ... `RenderType`, on the other hand, is simply a named `RenderSetup`.
>
> ### Block and Terrain Split
>
> `RenderPipeline`s that were used by both a standalone block and the terrain has been split into separate pipelines: one with prefix `_BLOCK` and `_TERRAIN`, respectively. This includes the solid, cutout, translucent, and tripwire pipelines. No block variant exists for the translucent pipeline.
>
> ### Item Atlases
>
> The block atlas no longer contains textures specifically for items. Those have been moved to their own atlas named `minecraft:items`, with the id stored at `AtlasIds#ITEMS`.

### 5.2 1.21.11 bullet list — GlStateManager / RenderSystem / shaders (verbatim)

`com.mojang.blaze3d.opengl`
- `GlDevice` now takes in a `ShaderSource` instead of a `BiFunction`
  - `getOrCompileShader` now takes in a `ShaderSource` instead of a `BiFunction`
- `GlRenderPass`
  - `samplers` now is a hash map of strings to `GlRenderPass$TextureViewAndSampler`s
  - `$TextureViewAndSampler` - A record that defines a sampler with its sampled texture.
- `GlSampler` - The OpenGL implementation of a gpu sampler.
- **`GlStateManager`**
  - `_glBufferSubData` now uses in a `long` for the offset
  - `_glMapBufferRange` now uses `long`s for the length and offset
- `GlTexture#modesDirty`, `flushModeChanges` are removed
- `GlTextureView#getFbo` - Gets the framebuffer object of a texture, using the cache if present.

- `com.mojang.blaze3d.pipeline.RenderTarget#filterMode`, `setFilterMode` are removed
- `com.mojang.blaze3d.platform.TextureUtil`
  - `solidify` - Modifies the texture by packing and unpacking the pixels to better help with non-darkened interiors within mipmaps.
  - `fillEmptyAreasWithDarkColor` - Sets empty pixels to an empty pixel whose RGB value is the darkest color in the image.
- **`com.mojang.blaze3d.shaders.ShaderSource` - A functional interface that gets the shader source from its id and type as a string.**

`com.mojang.blaze3d.systems`
- `CommandEncoder#copyTextureToBuffer` now uses a `long` for the offset
- `GpuDevice`
  - `createSampler` - Creates a sampler for some source to destination with the desired address and filter modes.
  - `precompilePipeline` now takes in a `ShaderSource` instead of a `BiFunction`
  - `getMaxSupportedAnisotropy` - The maximum anisotropic filtering level supported by the hardware.
  - `createBuffer` now uses in a `long` for the size
- `RenderPass#bindTexture` now takes in a `GpuSampler`
- **`RenderSystem`**
  - now uses in a `long` for the size
  - `samplerCache` - Returns a cache of samples containing all possible combinations.
  - **`TEXTURE_COUNT` is removed**
  - **`setupOverlayColor`, `teardownOverlayColor` are removed**
  - **`setShaderTexture`, `getShaderTexture` are removed**
  - **`setTextureMatrix`, `resetTextureMatrix`, `getTextureMatrix` are removed**
  - `lineWidth` -> `VertexConsumer#setLineWidth`
  - `getShaderLineWidth` -> `Window#getAppropriateLineWidth`
  - `initRenderer` now takes in a `ShaderSource` instead of a `BiFunction`
- `SamplerCache` - A cache of all possible samplers that may be used by the renderer.

`com.mojang.blaze3d.textures`
- `GpuSampler` - A buffer sampler with the specified UV address modes and minification and magnification filters.
- `GpuTexture`
  - `addressModeU` -> `GpuSampler#getAddressModeU`
  - `addressModeV` -> `GpuSampler#getAddressModeV`
  - `minFilter` -> `GpuSampler#getMinFilter`
  - `magFilter` -> `GpuSampler#getMagFilter`
  - `setAddressMode`, `setTextureFilter` have been replaced by `SamplerCache#getSampler`, not one-to-one
  - `useMipmaps`, `setUseMipmaps` are removed

**CRITICAL for the task:** the `setShaderTexture` / `getShaderTexture` **removal happens in 1.21.11**, not 1.21.5. In 1.21.5 they merely changed signature to take a `GpuTexture`; in 1.21.6 to `GpuTextureView`; in 1.21.11 they are removed outright.

Also in 1.21.11: `com.mojang.blaze3d.buffers.GpuBuffer` size moves to `long`; `GpuBufferSlice` uses `long`s.

---

## 6. Primers 26.1 / 26.2 / 26.3

**COULD NOT RETRIEVE THE PRIMER TEXT.** See §8/§9 for the exhaustive list of failed approaches.

- **26.1** (`1.21.11 -> 26.1`): 307,202 bytes at https://raw.githubusercontent.com/neoforged/.github/main/primers/26.1/index.md — beyond the 100 KB fetch cap; tail (including any rendering-engine section) not retrieved.
  - A fork mirror with the same content exists at https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/update/26.1/primers/26.1/index.md (307,203 bytes) — same cap problem.
- **26.2** (`26.1.x -> 26.2`): 182,451 bytes at https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md — not retrieved.
  - Fork mirror (referenced by the Fabric 26.2 blog): https://github.com/ChampionAsh5357/neoforged-github/blob/port/26.2/primers/26.2/index.md (182,353 bytes) — not retrieved.
- **26.3** (`26.2 -> 26.3`): https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md — not retrieved.
  - Fork branches `port/26.3` and `port/26.4` exist on `ChampionAsh5357/neoforged-github`.

### 6.1 What IS known about 26.x rendering, from citable non-primer sources

**Vulkan (the task's specific question).** No primer text obtained, but two independent citable sources confirm it:

1. **Fabric 26.2 blog** — https://fabricmc.net/2026/06/15/262.html:
   > 26.2 introduces the ability for the backend to be changed between the default OpenGL backend and an experimental Vulkan backend, with OpenGL planned to be removed once the Vulkan backend is stable. Developers making use of raw OpenGL calls rather than going through the Blaze3D API will need to migrate for their mods to work.

2. **Stonecutter "Notable Minecraft changes"** — https://stonecutter.kikugie.dev/wiki/v2/guides/tools/minecraft-changes:
   > ## `26.2` … - Rendering backend can now be switched between OpenGL and Vulkan

**26.1 GUI/rendering rename** — **NeoForge 26.1 release post**, https://neoforged.net/news/26.1release/, section "`GuiGraphics` rename":

> As in the last few releases, rendering continues to be refactored at a fast pace. `GuiGraphics` was renamed to `GuiGraphicsExtractor`, and many GUI-related methods were renamed in a similar fashion.

| 1.21.11 | 26.1 |
| --- | --- |
| `Screen#render` | `Screen#extractRenderState` |
| `Screen#renderBackground` | `Screen#extractBackground` |
| `AbstractContainerScreen#renderBg` | `Screen#extractBackground` |
| `AbstractContainerScreen#renderLabels` | `Screen#extractLabels` |

Same post also documents `MutableQuad` (a NeoForge-added mutable `BakedQuad` representation with `setCubeFaceFromSpriteCoords`, `setCubeFace`, `bakeUvsFromPosition`, `recalculateWinding`, `setSpriteAndMoveUv`), Java 25, and the removal of obfuscation.

**26.1 rendering docs (current version)** — https://docs.neoforged.net/docs/rendering/screens/ describes the post-26.1 equivalents, e.g. `Renderable#extractRenderState`, `GuiGraphicsExtractor#nextStratum`, `GuiGraphicsExtractor#blurBeforeThisStratum`, `GuiElementRenderState` (`pipeline`, `scissorArea`, `textureSetup`, `bounds`, `buildVertices`), and NeoForge's `GuiGraphicsExtractor#submitGuiElementRenderState`.

### 6.2 1.21.11 official corroboration (NeoForge 21.11 release post)

https://neoforged.net/news/21.11release/ — sections "RenderType changes" and "Baked quad changes":

> Vanilla render types are now in the `RenderTypes` class (previously they were in `RenderType`). Additionally, the construction of `RenderType`s has been refactored a little.

> `BakedQuad` no longer stores its data encoded in an `int[]`. Instead, it now has explicit fields for vertex positions and uvs. ... In NeoForge, we add `bakedNormals` and `bakedColors` fields to it ...
>
> ```java
> BakedColors bakedColors = BakedColors.of(color);
> BakedColors bakedColors = BakedColors.of(color0, color1, color2, color3);
> ```
>
> ... use the following pattern to instantiate a `Vector3fc` for usage in a baked quad:
> ```java
> ModelBaker modelBaker = /* ... */;
> Vector3fc position0 = modelBaker.parts().vector(0.25F, 0.5F, 0.0F);
> ```
>
> ... the `neoforge_data` of a model element or face will not recognize the `block_light` and `sky_light` keys anymore, however it will now recognize the `light_emission` key which acts as a combined block+sky light setter.

---

## 7. Stonecutter version-compatibility table

URL: **https://stonecutter.kikuguri.dev** → actual: https://stonecutter.kikugie.dev/wiki/v2/guides/tools/minecraft-changes
(Source of that page: https://codeberg.org/stonecutter/docs/src/branch/main/docs/wiki/v2/guides/tools/minecraft-changes.md)

Verbatim rendering-relevant rows:

> ## `26.2`
> Wikis: [26.2](https://minecraft.wiki/w/Java_Edition_26.2)
> Blogs: [Fabric](https://fabricmc.net/2026/06/15/262.html) | [Primer](https://docs.neoforged.net/primer/docs/26.2/)
>
> - Numerous changes in rendering internals (see linked posts)
> - **Rendering backend can now be switched between OpenGL and Vulkan**
> - Item and block IDs have been split to `BlockIds`, `BlockItemIds` and `ItemIds`
> - GUI and HUD methods were moved to `Gui` and `Hud` classes respectively, including the current screen instance

> ## `26.1.0-2`
> - **Updated to Java 25**
> - **Unobfuscated game classes**
> - `ItemStack`s now require registry access; `ItemStackTemplate`s are used for data representation
> - `GuiGraphics` was renamed to `GuiGraphicsExtractor`, along with many rendering methods replacing 'render' with 'extract'
> - Recipe serializers were moved to a dedicated registry
> - The game now automatically determines block render layers

> ## `1.21.11`
> - Gamerules now use the registry system
> - Codebase now uses [Jspecify](https://jspecify.dev/docs/start-here/) nullability annotations
> - Vanilla `RenderType`s were moved to the `RenderTypes` class
> - `ResourceLocation` has been renamed to `Identifier`, as well as the relocation of a bunch of other classes
> - **Further changes to rendering pipelines**

> ## `1.21.9-10`
> - **World, block entity, entity and particle rendering were reworked to use render states**
> - Key binding categories are now defined by their own class
> - Resource reloaders now provide a shared state
> - Expanded debug renderer capabilities

> ## `1.21.6-8`
> - **The rendering process now extracts renderable data before drawing it (currently only for GUI, HUD and chunks)**
> - Block entity (de)serialization now uses input/output abstractions instead of operating on the NBT directly

> ## `1.21.5`
> - Many item interfaces were replaced with components
> - NBT queries now support `Optional`s and default values
> - The gametest framework has been reworked, exposing it to datapacks
> - **Core shader JSON definitions were replaced with `RenderPipeline`s**

---

## 8. Supporting / corroborating sources

- **NeoForge 21.5 release announcement** (official, paraphrases the primer): https://neoforged.net/news/21.5release/
  > ### Render Pipeline Rework
  > Rendering an object to the screen, whether through a shader or a RenderType, has been fully or partially reworked, depending on what systems you were using previously. ... First, core-shader JSONs no longer exist. They are replaced by a `RenderPipeline`, which is effectively an in-code substitute. Second, the `RenderPipeline`s turns most flag values into enum constants. For example, instead of storing the blend function mode id, you store a `BlendFunction`. ...
  >
  > #### Abstracting OpenGL
  > As many are aware, Minecraft has been abstracting away their OpenGL calls and constants, and this release is no different. All the calls to GL codes, except BufferUsage, have been moved out of object references, to be obtained typically by calling GlConst$toGl. ...
  >
  > :::warning
  > Please be aware that while these changes are wide sweeping, **OpenGL is still the underlying platform**, so they will probably not cause compile errors. Mods are however advised switch to the new platform.
  > :::

- **Fabric 1.21.5 blog** (no rendering-pipeline detail; confirms rendering was NOT listed as a modder-facing 1.21.5 change on the Fabric side): https://fabricmc.net/2025/03/24/1215.html
- **Fabric 1.21.9 blog**: https://fabricmc.net/2025/09/23/1219.html
- **Fabric 1.21.11 blog**: https://fabricmc.net/2025/12/05/12111.html
- **Fabric 26.1 blog**: https://fabricmc.net/2026/03/14/261.html
- **Fabric 26.2 blog**: https://fabricmc.net/2026/06/15/262.html
- **NeoForge 21.11 release**: https://neoforged.net/news/21.11release/
- **NeoForge 26.1 release**: https://neoforged.net/news/26.1release/

---

## 9. Methodology / retrieval notes (for reproducibility)

The `web_fetch` tool caps responses at ~100 KB. Raw primer files are 140–310 KB. What worked and what did not:

**Worked:**
1. **Finding the real source repo** — `neoforged/documentation` `scripts/initialize.js` reveals `PRIMERS_GIT = 'https://github.com/neoforged/.github'` with `{directories: {'primers': 'docs'}}`.
2. **Algolia DocSearch index of the docs site** — config is public in `docusaurus.config.js` (appId `05RJFT798Z`, apiKey `b198aa85c7f2ee9364d105ef0be4d81a`, index `neoforged`). GET queries of the form
   `https://05RJFT798Z-dsn.algolia.net/1/indexes/neoforged?query=<terms>&x-algolia-application-id=05RJFT798Z&x-algolia-api-key=<key>&hitsPerPage=N&attributesToRetrieve=url,anchor,hierarchy,content`
   return per-anchor `content` chunks. The record with anchor `post-effects` on the 1.21.5 page contained the **entire tail** bullet list of the Render Pipeline Rework section — this is how §1.6 was recovered despite the 100 KB cap. **Caveat:** the index only covers `docs/` and primers up to ~1.21.11; it does NOT contain 26.1/26.2/26.3 primer pages (queries for `Vulkan`, `GuiGraphicsExtractor` etc. return only `docs/` hits).
3. **Files under the cap** — 1.21.10 (tiny), 1.21.7, 1.21.8 all returned completely.

**Did NOT work:**
- URL fragments (`#abstracting-open-gl`): fragments are client-side only; Docusaurus does not slice server-side.
- `r.jina.ai` — blocked ("resolves to a non-public IP address").
- `cdn.jsdelivr.net/gh/...` — cross-origin redirect to raw.githubusercontent.com, not followed.
- `raw.gitmirror.com` — DNS failure.
- `api.codetabs.com/v1/proxy`, `api.allorigins.win` — HTTP 522.
- `https://github.com/neoforged/.github/blob/main/primers/26.2/index.md` — returns GitHub chrome; truncated before content.
- `patch-diff.githubusercontent.com/.../pull/14.diff` — works, but the diff for the initial primer commit is the same ~195 KB (still capped). Useful only for small follow-up commits.
- GitHub code search API — 401 (requires auth).
- Algolia `settings` / `browse` endpoints — 403 for a search-only key.
- PowerShell/`curl` from the shell has **no outbound network** in this sandbox (TLS handshake fails / timeout), so downloading the file locally was impossible.

**Consequence:** the 1.21.5 tail was recovered via Algolia; the **26.1, 26.2 and 26.3 primer rendering/Vulkan sections could not be retrieved in this session.**

**Two independently verified structural facts (a second agent re-confirmed these, and they rule out a whole class of workarounds):**
- The primer for each version is a **single unsplit file**. `https://api.github.com/repos/neoforged/.github/contents/primers/1.21.5` lists exactly one entry: `index.md`, 194,956 bytes, sha `963c8220f9d28f08f4d8b314fd6f19c519272a47`. The "it's split across multiple files in some fork" hypothesis is **false** for the canonical repo.
- `https://api.github.com/repos/neoforged/documentation/contents/primer` contains **only `.gitignore`**. The primer markdown is *not* stored in the documentation repo at all — it is cloned in at build time from `neoforged/.github`. Any search of `neoforged/documentation` for `primer/versioned_docs/...` is guaranteed to 404.

**Additional dead ends confirmed by a second agent (do not re-attempt):**
- GitHub Git Blobs API (`/git/blobs/<sha>`) returns base64, which is ~33% *larger*, so it truncates **earlier** (~40%) than the raw fetch — strictly worse.
- Algolia `GET /settings` and `/browse` → 403 ("Method not allowed with this API key" — it is a search-only key); `/1/indexes/*/queries` → 400 (needs a POST body, `web_fetch` cannot POST). **The working Algolia route is the plain `GET /1/indexes/neoforged?query=...` form.**
- `corsproxy.io` → 403 (API key required); `thingproxy` → DNS ENOTFOUND; `codeberg.org/stonecutter/docs` has no `primers/` path; `gitea.com/neoforged` → 404.
- Local PowerShell has **no outbound network** in this sandbox, so HTTP Range requests were never available.

---

## 10. Summary answers to the task's specific questions

| Question | Verdict | Version |
|---|---|---|
| `com.mojang.blaze3d.platform.GlStateManager` removal | **Not removed — relocated.** `com.mojang.blaze3d.platform.GlStateManager` -> `com.mojang.blaze3d.opengl.GlStateManager`. Many `_gl*`/`_tex*`/`_stencil*` methods removed; `$DestFactor` -> `DestFactor`, `$SourceFactor` -> `SourceFactor`, `$LogicOp` -> `LogicOp`, `$CullState#mode`, `$FramebufferState`, `$StencilFunc`, `$StencilState`, `$Viewport`, `$PolygonOffsetState#line` removed. | **1.21.5** |
| GlStateManager further changes | `_glBufferSubData` long offset; `_glMapBufferRange` long length/offset | **1.21.11** |
| `GlUtil` | Class removed; `getVendor`/`getRenderer`/`getOpenGlVersion`->`getVersion` moved to `GpuDevice` | 1.21.5 |
| `RenderSystem.setShader`/`clearShader`/`getShader` | **Removed** | **1.21.5** |
| `RenderSystem.blendFunc`/`blendFuncSeparate`/`blendEquation` | **Removed** | **1.21.5** |
| `RenderSystem.depthFunc`/`depthMask` | **Removed** | **1.21.5** |
| `RenderSystem.setShaderTexture`/`getShaderTexture` | Signature change to `GpuTexture` (1.21.5) -> `GpuTextureView` (1.21.6) -> **removed** (1.21.11) | 1.21.5 / 1.21.6 / **1.21.11** |
| `RenderSystem.setShaderColor`/`getShaderColor` | Kept (listed, not removed) | 1.21.6 |
| `RenderSystem.SCISSOR_STATE` | -> `scissorStateForRenderTypeDraws`, now private | 1.21.6 |
| `RenderSystem.TEXTURE_COUNT`, `setupOverlayColor`, `teardownOverlayColor`, texture matrix | **Removed** | **1.21.11** |
| `RenderSystem.getDevice()` / `GpuDevice` | **New in 1.21.5** — the central abstraction | 1.21.5 |
| `CommandEncoder` | New interface in 1.21.5 (`GpuDevice#createCommandEncoder`) | 1.21.5 |
| `RenderPass` | New interface in 1.21.5 | 1.21.5 |
| `GpuBuffer`, `GpuTexture`, `GpuTextureView` | `GpuBuffer`/`GpuTexture` in 1.21.5; `GpuTextureView` added 1.21.6; `GpuSampler` added 1.21.11 | 1.21.5 / 1.21.6 / 1.21.11 |
| `RenderPipeline` / `CompiledRenderPipeline` | New in 1.21.5, replacing shader JSON | 1.21.5 |
| `RenderTarget`/`MainTarget` | `RenderTarget` heavily changed (`colorTexture`/`depthTexture` are `GpuTexture`s). **`MainTarget` is NOT mentioned anywhere in the primer.** | 1.21.5 |
| `ShaderDefines` | `ShaderDefines$Builder#define` gains an `int` overload | 1.21.5 |
| `MultiBufferSource` | Still used/recommended for `RenderType` draws | 1.21.5 |
| `PostChain`/`PostPass`/`EffectInstance` | `PostChain`: `setUniform` removed, `load`/`process` signatures change (1.21.5), `load` takes `CachedOrthoProjectionMatrixBuffer` (1.21.6). `PostPass` takes `RenderPipeline` not `CompiledShaderProgram` (1.21.5); `AutoCloseable` (1.21.6). **`EffectInstance` is never mentioned in any primer.** | 1.21.5 / 1.21.6 |
| `ShaderInstance` | **Never named.** The 1.21.5 counterpart class changes are `ShaderProgram`/`ShaderProgramConfig` -> `RenderPipeline`, and `CompiledShaderProgram` -> `com.mojang.blaze3d.opengl.GlProgram`. `ShaderManager` loses `preloadForStartup`, `getProgram`, `linkProgram(RenderPipeline)`, `$ShaderCompilationKey`. | 1.21.5 |
| `GlslPreprocessor` / shader source injection / `ShaderInstance` patching | **Zero mentions.** Algolia full-index query `GlslPreprocessor` => `nbHits: 0`. Shader *source* abstraction appears only in 1.21.11: `com.mojang.blaze3d.shaders.ShaderSource` (functional interface) and `GpuDevice#precompilePipeline(ShaderSource)`. | none / **1.21.11** |
| `assets/<ns>/shaders/core/*.json` | **Removed in 1.21.5** — "shader JSONs no longer exist" / "Core shader JSON definitions were replaced with `RenderPipeline`s". Shaders become `assets/<ns>/shaders/<name>.vsh` and `.fsh`. | **1.21.5** |
| `assets/<ns>/shaders/post/*.json` | `program` key replaced by explicit `vertex_shader`/`fragment_shader`, relative to `shaders` -> `assets/<ns>/shaders/post/<name>.vsh|.fsh`; uniforms need `type`. 1.21.6 moves post effects to `assets/<ns>/post_effect/<name>.json` with map-of-interface-block uniforms. | **1.21.5 / 1.21.6** |
| `.../shaders/include/<path>` + `moj_import` | Documented | **1.21.6** |
| `program/*.json` | **Never mentioned in any primer.** | none |
| Vulkan | Mentioned once in 1.21.5, aspirational only. NeoForge 21.5 release explicitly warns "OpenGL is still the underlying platform". Stonecutter: 26.2 "Rendering backend can now be switched between OpenGL and Vulkan". | 1.21.5 (aspirational) / **26.2 (real)** |
| OpenGL backend removal | **Not removed.** 1.21.6 section is literally titled "Render Pass Scissoring now only OpenGL" — OpenGL-specific code exists and grew. | 1.21.6 |
| `BindGroup` | **Never mentioned in any primer.** | none |
| `Very Technical Changes` | Section exists in 1.21.5 (credited to @TelepathicGrunt), *not* a rendering section. | 1.21.5 |
