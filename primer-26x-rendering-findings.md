# NeoForge 26.x Primer — Rendering/Shader Findings (cited)

## Retrieval status

| Source | URL | Result |
|---|---|---|
| 26.1 raw | https://raw.githubusercontent.com/neoforged/.github/main/primers/26.1/index.md | HTTP 200, truncated at ~100KB (rendering tail NOT retrieved) |
| 26.2 raw | https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md | HTTP 200, truncated, but the **entire "Too Many Rendering Changes" + blaze3d/blaze3d.vulkan package listing WAS retrieved** |
| 26.3 raw | https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md | HTTP 200, truncated, but **"There's Always a Client Rewrite" (Renderpearl, Shader Extensions and Layouts, OIT) WAS retrieved** |
| docs site 26.1 | https://docs.neoforged.net/primer/docs/26.1/ | same 100KB truncation |
| jsdelivr | https://cdn.jsdelivr.net/gh/neoforged/.github@main/primers/26.1/index.md | FAILED (cross-origin redirect to raw.githubusercontent) |
| raw.githack | https://raw.githack.com/neoforged/.github/main/primers/26.1/index.md | FAILED (fetch failed) |
| statically.io | https://cdn.statically.io/gh/neoforged/.github/main/primers/26.1/index.md | FAILED (fetch failed) |
| allorigins | https://api.allorigins.win/raw?url=... | FAILED (408 timeout) |
| codetabs | https://api.codetabs.com/v1/proxy?quest=... | FAILED (522) |
| corsproxy.io | https://corsproxy.io/?... | FAILED (403 keyless legacy URL unsupported) |
| r.jina.ai | https://r.jina.ai/https://raw.githubusercontent.com/... | FAILED (non-public IP) |
| Algolia settings | https://05RJFT798Z-dsn.algolia.net/1/indexes/neoforged/settings?... | HTTP 403 "Method not allowed with this API key" |
| Algolia query (GET) | .../1/indexes/neoforged/query?... | HTTP 404 "ObjectID does not exist" (Algolia requires POST; web_fetch is GET-only) |
| zh mirror | https://zh-neoforge.netlify.app/primer/docs/26.1/ | HTTP 404 |
| GitHub contents API | https://api.github.com/repos/neoforged/.github/contents/primers/26.1/index.md | 200, base64, still ~100KB truncated |
| GitHub git blobs API | https://api.github.com/repos/neoforged/.github/git/blobs/db861efadb2d70b9dea491ccde80bc913c8fe4ee | 200, base64, still ~100KB truncated, stops mid-file |
| GitHub blob HTML | https://github.com/neoforged/.github/blob/main/primers/26.1/index.md?plain=1 | 200 but only nav chrome |
| ChampionAsh5357 fork | https://raw.githubusercontent.com/ChampionAsh5357/neoforged-github/port/26.2/primers/26.1/index.md | 200, same truncation |
| Shell download | Invoke-WebRequest | FAILED — no outbound network from pwsh (even https://example.com) |
| grep.app API | https://grep.app/api/search?q=GlStateManager&filter[repo][0]=neoforged/.github | 429 Vercel checkpoint |
| GitHub code search API | https://api.github.com/search/code?q=... | 401 requires authentication |

**Bottom line: no byte-range / offset fetch is possible. All sources cap at ~100KB and every fetch restarts at byte 0.** The 26.1 rendering tail was NOT retrieved.

---

# 26.1 — 1.21.11 -> 26.1

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/26.1/index.md

**NOT RETRIEVED.** The fetched text ends at the section "Splitting the Primary Level Data into Saved Data" -> subsection "Additional Saved Data" (mid-bullet, `- `net.minecraft.world.level.storage` / `PrimaryLevelData` ... `writeVers`). File is 307,202 bytes; fetch cap is ~100KB of formatted output.

Section headings that WERE visible (in document order, from the retrieved head):
- `## Pack Changes`
- `## Java 25 and Deobfuscation`
- `## Loot Type Unrolling`
- `## Validation Overhaul`
- `## Datapack Villager Trades`
  - `### Understanding the Trade Format`
  - `### The Trades of a Trader`
  - `### Item Listing Conversions`
- `## `Level#random` field now protected`
- `## Data Component Initializers`
  - `### Items`
  - `### Recipes`
- `## Item Instances and Stack Templates`
  - `### Recipe Builders`
- `## Serializer Records and Recipe Info`
- `## Dye Component`
  - `### Entities and Signs`
  - `### Dye Recipes`
- `## World Clocks and Time Markers`
  - `### Marking Timelines`
- `## Splitting the Primary Level Data into Saved Data`
  - `### Saved Data Changes`
  - `### Additional Saved Data`

**Rendering-relevant content retrieved from 26.1: NONE.** The only rendering-adjacent item seen is:
> `net.minecraft.client.renderer.EndFlashState#tick` now takes in the end clock time instead of the game time

No occurrence of `GlStateManager`, `RenderSystem`, `RenderTarget`, `RenderPipeline`, `GpuBuffer`, `GpuTexture`, `RenderPass`, `PostChain`, `PostPass`, `EffectInstance`, `ShaderInstance`, `GlslPreprocessor`, `ShaderDefines`, `GpuDevice`, `CommandEncoder`, `BindGroup`, `Vulkan`, `shaders/core`, `post`, or `program` could be confirmed anywhere in the retrieved 26.1 text. **I cannot state what 26.1 says about any of the requested topics.**

NOTE: the primer's 26.2 (below) opens with "Too Many Rendering Changes" describing the state *as of 26.2 relative to 26.1.x*, so the 26.1 baseline can be partly inferred, but that is inference, not a 26.1 quote.

---

# 26.2 — 26.1.x -> 26.2

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/26.2/index.md
(Fetched text spill: `C:\Users\HP\AppData\Local\Temp\dsh-spill-jk9fcS\session-09dd4567f8a5\75e2bf15f985-web_fetch.txt`)

## Vulkan (verbatim)

> ### Vulkan
>
> Vanilla has added support for the Vulkan graphics API, which can be set in the options menu. As such, for all classes in `com.mojang.blaze3d.opengl`, there is generally a parallel found in `com.mojang.blaze3d.vulkan`. With this change, more of the Blaze3d components has been generalized further.

Also (under `net.minecraft.client.main`):
> - `net.minecraft.client.main.GameConfig$GameData` now takes in `boolean`s for whether to use the validation layers for Vulkan, and the graphics api forced by launch argument
>     - `vulkanValidation` - Whether to use the validation layers for Vulkan.

`com.mojang.blaze3d.shaders`:
> - `GpuDebugOptions` now takes in a `boolean` of whether to use validation layers; only used by the Vulkan backend

### `com.mojang.blaze3d.vulkan` (new package, verbatim)

> - `com.mojang.blaze3d.vulkan`
>     - `Destroyable` - An interface for destroying the object, typically through `vkDestroy*`.
>     - `DestructionQueue` - A queue for managing the destruction of objects at a specific time.
>     - `VulkanBackend` - The Vulkan implementation of the `GpuBackend`.
>     - `VulkanBindGroupLayout` - A list of layout bindings for the handle.
>     - `VulkanCommandEncoder` - The Vulkan implementation of the `CommandEncoderBackend`.
>     - `VulkanCommandPool` - A pool for allocating the command buffers.
>     - `VulkanConst` - The constant mappings for Blaze3d to Vulkan.
>     - `VulkanDebug` - A utility for debugging Vulkan objects.
>     - `VulkanDevice` - The Vulkan implementation of the `GpuDeviceBackend`.
>     - `VulkanGpuBuffer` - The Vulkan implementation of a `GpuBuffer`.
>     - `VulkanGpuSampler` - The Vulkan implementation of a `GpuSampler`.
>     - `VulkanGpuSurface` - The Vulkan implementation of the `GpuSurfaceBackend`.
>     - `VulkanGpuTexture` - The Vulkan implementation of a `GpuTexture`.
>     - `VulkanGpuTextureView` - The Vulkan implementation of a `GpuTextureView`.
>     - `VulkanInstance` - The connection between the application and the Vulkan library, maps almost directly to [VkInstance](https://docs.vulkan.org/refpages/latest/refpages/source/VkInstance.html).
>     - `VulkanPhysicalDevice` - The installed device with Vulkan support available on the system, maps almost directly to [VkPhysicalDevice](https://docs.vulkan.org/refpages/latest/refpages/source/VkPhysicalDevice.html).
>     - `VulkanQueryPool` - The Vulkan implementation of a `GpuQueryPool`.
>     - `VulkanQueue` - A wrapped queue on a device to submit elements.
>     - `VulkanRenderPass` - The Vulkan implementation of the `RenderPassBackend`.
>     - `VulkanRenderPipeline` - The Vulkan implementation of a `CompiledRenderPipeline`.
>     - `VulkanTransientMemory` - The Vulkan implementation of the `TransientMemory`.
>     - `VulkanUtils` - A utility for common operations within the Vulkan implementation.
> - `com.mojang.blaze3d.vulkan.checkpoints`
>     - `AbstractCheckpointStorage` - An abstract implementation of the storage that handles the markers.
>     - `AmdCheckpointExtension` - A `CheckpointExtension` for AMD GPUs.
>     - `CheckpointExtension` - An extension that inserts markers into a command stream to help determine what caused a device lost failure.
>     - `NoopCheckpointExtension` - A `CheckpointExtension` that does nothing.
>     - `NvidiaCheckpointExtension` - A `CheckpointExtension` for Nvidia GPUs.
> - `com.mojang.blaze3d.vulkan.glsl`
>     - `GlslCompiler` - Compiles the GLSL shaders into SPIR-V bytecode.
>     - `IntermediaryShaderModule` - An intermediary module representing the shader after initial compilation but before binding.
>     - `ShaderCompileException` - An exception thrown during the Vulkan shader compilation process.
>     - `SpvcUtil` - A utility for converting a dimension to its string representation in an SPV shader.
>     - `SpvSampler` - A sampler for an SPV shader.
>     - `SpvUniformBuffer` - A uniform buffer for an SPV shader.
>     - `SpvVariable` - A variable for an SPV shader.
> - `com.mojang.blaze3d.vulkan.init`
>     - `VulkanFeature` - A Vulkan feature on the device.
>     - `VulkanPNextStruct` - A representation of a Vulkan struct on the `pNext` chain.

## `com.mojang.blaze3d.platform` — GlStateManager (verbatim, 26.2)

> `com.mojang.blaze3d.platform`
>     - `BackendOptions` record is removed
>     - `BlendOp` - The operation performed when blending the source and destination outputs.
>     - `ClientShutdownWatchdog#startShutdownWatchdog` now takes in a `boolean` of whether to force shutdown with a `-8` exit call
>     - `DestFactor`, `SourceFactor` -> `BlendFactor`
>     - `GLX`
>         - `_initGlfw` no longer takes in the `BackendOptions`
>         - `getGlfwPlatform` - Returns the currently selected platform.
>     - `InputConstants$Value` can now only target `ElementType#TYPE_USE`
>     - `MacosUtil#setWindowColorSpaceForOpenGLBecauseGLFWDoesnt` - Sets the window color space.
>     - `Monitor` is now a record, taking in the `String` monitor name
>         - Original constructor behavior is handled through `Monitor#tryCreate`
>         - `refreshVideoModes` is removed
>         - `getVideoModeIndex` -> `indexOfMode`
>         - `getCurrentMode` is removed
>         - `getX`, `getY` -> `x`, `y`
>         - `getMode` -> `mode`
>         - `getModeCount` -> `modeCount`
>         - `getMonitor` is removed
>     - `MonitorCreator` interface is removed
>         - Use `Monitor#tryCreate` instead
>     - `NativeImage#getPixelBytes` - Gets a buffer of the pixels in the image.
>     - `NativeLibrariesBootstrap` - A bootstrap for validating and loading the required native libraries.
>     - `ScreenManager` -> `MonitorManager`, not one-to-one
>     - `TextureUtil#writeAsPNG` now only allows `RGBA8_UNORM` formatted textures to be written to disk.
>     - `Window` now takes in a `boolean` for whether to use exclusive fullscreen and the `MonitorManager`
>         - `updateVsync` is removed
>             - Usage replaced by `Window#setMode` and `WindowEventHandler#framebufferSizeChanged`
>         - `isResized`, `resetIsResized` are removed
>     - `WindowEventHandler#framebufferSizeChanged` - Handles when the window buffer size has changed.

**Note:** In 26.2, `GlStateManager` is NOT listed under `com.mojang.blaze3d.platform`. It appears only under `com.mojang.blaze3d.opengl` (below).

## `com.mojang.blaze3d.opengl.GlStateManager` (verbatim, 26.2)

>     - `GlStateManager`
>         - `_blendEquationSeparate`, `glBlendEquationSeparate` - Sets the blend mode of the RGB and alpha channels.
>         - `_disableBlend`, `_enableBlend` now take in an `int` index for the buffer the blend is applied to
>         - `_colorMask` now has an overload that specifies the index of the buffer to apply the write mask to
>         - `_clearBuffer` - Clears the give buffer using the associated value.
>         - `$BlendState`
>             - `modeRgb` - The blend mode of the RGB color channels.
>             - `modeAlpha` - The blend mode of the alpha channel.

(Note: this is inside the `com.mojang.blaze3d.opengl` bullet list; the next line in the source is `- `GlSurface` - The OpenGL implementation of the surface backend.`)

## `com.mojang.blaze3d.systems.RenderSystem` (verbatim, 26.2)

>     - `RenderSystem`
>         - `DEFAULT_DEPTH_CLEAR_VALUE` - The default clear value for the depth buffer.
>         - `flipFrame` is removed
>         - `getApiDescription` is removed
>         - `shutdownRenderer` - Shuts down the renderer and GPU device.
>         - `getModelViewMatrix` -> `getModelViewMatrixCopy`, not one-to-one
>         - `initBackendSystem` no longer takes in the `BackendOptions`
>         - `$AutoStorageIndexBuffer` now implements `AutoCloseable`

Related new types in the same package:
>     - `DeviceFeatures` - A record containing the features that the GPU device supports.
>     - `DeviceInfo` - A record containing information about the GPU device.
>     - `DeviceLimits` - A record containing information about the limits of GPU features.
>     - `DeviceType` - The type of the device performing the rendering (e.g., cpu, discrete graphics, integrated graphics).
>     - `GpuBackend#createDevice` now takes in a `Runnable` for the shaders that must be loaded for the client to start up, and can throw a `BackendCreationException`
>     - `GpuDevice` now takes in a `Runnable` for the shaders that must be loaded for the client to start up
>         - `createSurface` - Creates the surface for a window to write data to.
>         - `getImplementationInformation` -> `getDeviceInfo`, not one-to-one
>         - `getVendor` -> `DeviceInfo#vendorName`
>         - `getBackendName` -> `DeviceInfo#backendName`
>         - `getVersion` -> `DeviceInfo#driverInfo`
>         - `getRenderer` -> `DeviceInfo#name`
>         - `getMaxTextureSize` -> `DeviceLimits#maxTextureSize`
>         - `getUniformOffsetAlignment` -> `DeviceLimits#minUniformOffsetAlignment`
>         - `getEnabledExtensions` -> `DeviceInfo#underlyingExtensions`
>         - `getMaxSupportedAnisotropy` -> `DeviceLimits#maxAnisotropy`
>         - `setVsync` -> `GpuSurface#configure`, not one-to-one
>         - `presentFrame` -> `GpuSurface#present`
>         - `isZZeroToOne` -> `DeviceInfo#isZZeroToOne`
>         - `createTimestampQueryPool` - Creates the query pool for getting data from the GPU.
>         - `getTimestampNow` - Returns the current epoch timestamp.
>         - `loadCriticalShaders` - Loads the shaders required for the client to start up.
>     - `GpuQueryPool` - A pool for getting query objects and their associated values from the GPU.
>     - `GpuSurface` - The surface wrapper, swapchain, and validator.
>     - `GpuSurfaceBackend` - An API for modifying and writing data to linked window using its handle.
>     - `HintsAndWorkarounds` - A record containing issues with the supported GPU device and what workarounds are required.

### `CommandEncoder` (verbatim, 26.2)

>     - `CommandEncoder` now takes in the `TracyGpuProfiler`
>         - `backend` - Returns the encoder backend.
>         - `submit` - Submits the elements to the GPU and ends the frame.
>         - `submitRenderPass` - Submits the current render pass, handled as part of the try-with-resources.
>         - `presentTexture` -> `GpuSurface#blitFromTexture`, not one-to-one
>         - `timerQueryBegin`, `timerQueryEnd` replaced by `writeTimestamp`
>         - `createRenderPass`
>             - Now takes in an optional `Vector4fc` instead of an `int` for the clear color
>             - Now has an overload that takes in a `$RenderArea` for where to draw to
>             - Now has an overload that only takes in a `RenderPassDescriptor`
>         - `clearColorTexture`, `clearColorAndDepthTextures` now take in an optional `Vector4fc` instead of an `int` for the clear color
>         - `mapBuffer` replaced by `GpuBuffer#map`, `GpuBufferSlice#map`
>         - `transientMemory` - Gets the transient memory of the encoder.
>         - `writeToTexture` overload
>             - No longer takes in the width, height, and source XY of the image
>             - No longer takes in the `NativeImage$Format`
>         - `copyBufferToTexture` - Copies the buffer slice with the given parameters to the destination texture.
>     - `CommandEncoderBackend`
>         - `isInRenderPass` -> `CommandEncoder#isInRenderPass`, now `protected` from `public`
>         - `submitRenderPass` - Submits the current render pass, handled as part of the try-with-resources.
>         - `presentTexture` -> `GpuSurface#blitFromTexture`, not one-to-one
>         - `timerQueryBegin`, `timerQueryEnd` replaced by `writeTimestamp`
>         - `createRenderPass` now only takes in the `RenderPassDescriptor`
>         - `clearColorTexture`, `clearColorAndDepthTextures` now take in an optional `Vector4fc` instead of an `int` for the clear color
>         - `mapBuffer` replaced by `GpuBuffer#map`, `GpuBufferSlice#map`
>         - `transientMemory` - Gets the transient memory of the encoder.
>         - `writeToTexture(GpuTexture, ByteBuffer, NativeImage.Format, int, int, int, int, int, int)` is removed
>             - Original method no longer takes in the source XY and swaps the `NativeImage` for a `ByteBuffer`
>         - `copyBufferToTexture` - Copies the buffer slice with the given parameters to the destination texture.

### `RenderPass` / `RenderPassDescriptor` (verbatim, 26.2)

>     - `RenderPass` now takes in a `Runnable` for what to do on close, typically from a try-with-resources; a list of `RenderPassDescriptor$Attachment` color textures; and a `$RenderArea` of where to draw to
>         - `writeTimestamp` - Writes the timestamp to the pool at the given index.
>         - `setVertexBuffer` now takes in a `GpuBufferSlice` instead of a `GpuBuffer`
>         - `drawIndexed` parameter order for the five `int`s are as follows: the index count, instance count, first index, vertex offset / base vertex, and the first instance for fetching vertex attributes (new)
>         - `drawIndexedIndirect` - Draws the indexed data to the buffer using the indirect API.
>         - `draw` parameter order for the four `int`s are as follows: the vertex count, instance count, first vertex, and the first instance for fetching vertex attributes (new)
>         - `drawIndirect` - Draws the vertex data to the buffer using the indirect API.
>         - `multiDrawIndexed` - Draws the indexed data using an interleaved multi-draw call.
>         - `multiDraw` - Draws the vertex data using an interleaved multi-draw call.
>         - `$RenderArea` - A rectangle defining where the pass can write data to.
>     - `RenderPassBackend` is no longer `AutoCloseable`
>         - `isClosed` is removed
>         - `writeTimestamp` - Writes the timestamp to the pool at the given index.
>         - `setVertexBuffer` now takes in a `GpuBufferSlice` instead of a `GpuBuffer`
>         - `drawIndexed` parameter order for the five `int`s are as follows: the index count, instance count, first index, vertex offset / base vertex, and the first instance for fetching vertex attributes (new)
>         - `drawIndexedIndirect` - Draws the indexed data to the buffer using the indirect API.
>         - `draw` parameter order for the four `int`s are as follows: the vertex count, instance count, first vertex, and the first instance for fetching vertex attributes (new)
>         - `drawIndirect` - Draws the vertex data to the buffer using the indirect API.
>         - `multiDrawIndexed` - Draws the indexed data using an interleaved multi-draw call.
>         - `multiDraw` - Draws the vertex data using an interleaved multi-draw call.
>     - `RenderPassDescriptor` - A class containing the configuration of the `RenderPass` to use during drawing.

### `com.mojang.blaze3d.pipeline` (verbatim, 26.2)

> `com.mojang.blaze3d.pipeline`
>     - `BindGroupLayout` - The samplers and uniforms a shader will use.
>     - `BlendEquation` - An equation that blends the source and destination factors using the provided operation.
>     - `BlendFunction` now takes in `BlendEquation`s, `BlendFactor`s, or `BlendOp`s instead of `SourceFactor`s and `DestFactor`s
>     - `ColorTargetState` now takes in the `GpuFormat`
>         - `MAX_COLOR_TARGETS` - The maximum number of color states a buffer can use.
>         - `$WriteMask` can now only target `ElementType#TYPE_USE`
>     - `RenderPipeline` now takes in a list of `BindGroupLayout`s instead of uniforms and samplers directly, and an array of `ColorTargetState`s and `VertexFormat`s instead of just one
>         - `getSamplers`, `getUniforms` -> `getBindGroupLayouts`
>             - Can get the samplers and uniforms via `BindGroupLayout#flattenSamplers`, `flattenUniforms`
>         - `getColorTargetStates` - Returns the array of color target states.
>             - The original now just returns the first one
>         - `getVertexFormat` replaced by `getVertexFormatBinding`, taking in the bound index
>         - `getVertexFormatBindings` - Returns the array of vertex formats.
>         - `getVertexFormatMode` -> `getPrimitiveTopology`
>         - `$Builder`
>             - `withUniform`, `withSampler` -> `withBindGroupLayout`
>                 - Can add samplers and uniforms via `BindGroupLayout$Builder#withSampler`, `withUniform`
>             - `withColorTargetState` now takes in the `int` index to bind
>             - `withUnusedColorTargetState` - Unbinds the specified `int` index.
>             - `withVertexFormat` -> `withVertexBinding`, now taking in the `int` index to bind, and no longer taking in the `VertexFormat$Mode`
>             - `withPrimitiveTopology` - Sets the topology the pipeline uses.
>         - `$Snippet`
>             - `samplers`, `uniforms` -> `bindGroupLayouts`, not one-to-one
>             - `colorTargetState` -> `colorTargetStates`, now a nullable array of `ColorTargetState`s instead of an optional single state
>             - `activeColorTargetStateCount` - The number of active color states used by the pipeline.
>             - `vertexFormat` -> `vertexFormatPerBuffer`, now a nullable array of `VertexFormat`s instead of an optional single state
>         - `$UniformDescription` -> `BindGroupLayout$UniformDescription`
>     - `RenderTarget` now takes in the `GpuFormat`
>         - `blitToScreen` is removed
>             - Usage replaced by `GpuSurface#blitFromTexture`
>         - `blitAndBlendToTexture` now takes in a `GpuTextureView` for the output depth texture
>     - `TextureTarget` now takes in the `GpuFormat`

## `com.mojang.blaze3d.buffers` / `.textures` / `.vertex` (verbatim, 26.2)

> `com.mojang.blaze3d.buffers`
>     - `GpuBuffer`
>         - `USAGE_INDIRECT_PARAMETERS` - A flag that indicates the buffer can be used in indirect draw and compute dispatch calls.
>         - `$MappedView` -> `GpuBufferSlice$MappedView`, not one-to-one
>         - `$Usage` can now only target `ElementType#TYPE_USE`
>     - `GpuFence#awaitCompletion` now takes in a nanosecond timeout instead of a millisecond timeout

> `com.mojang.blaze3d.textures`
>     - `GpuTexture$Usage` now only targets `ElementType#TYPE_USE`
>     - `TextureFormat` replaced with `GpuFormat`
>         - `RGBA8` -> `RGBA8_UNORM`, not one-to-one
>         - `RED8` -> `R8_UNORM`, not one-to-one
>             - This should be taken with a grain of salt as it does not exactly map to the previous version's GL type
>         - `RED8I` -> `R8_SINT`, not one-to-one
>             - This should be taken with a grain of salt as it does not exactly map to the previous version's GL type
>         - `DEPTH32` -> `D32_FLOAT`, not one-to-one
>             - This should be taken with a grain of salt as it does not exactly map to the previous version's GL type

> `com.mojang.blaze3d.vertex`
>     - `ByteBufferBuilder$Result#size` - The size of the resulting buffer.
>     - `DefaultVertexFormat`
>         - `*_SEMANTIC_NAME` - The name of the attributes used by the default vertex formats.
>         - `EMPTY` is removed
>             - This is typically replaced with either a `null` value or an empty array
>     - `MeshData`
>         - `unpackQuadCentroids` -> `decodeQuadCentroids`, now `public` from `private`, not one-to-one
>         - `vertexBufferSlice` - The resulting vertex buffer containing the `MeshData`
>         - `writeSortedIndexBuffer` - Writes the sorted vertices to an index buffer.
>     - `StagingBuffer` - A buffer for staging changes to write at a later point in time.
>         - Many of the methods or logic were originally part of `UberGpuBuffer`.
>     - `Tesselator` class is removed
>     - `TlsfAllocator`
>         - `$Block` instance fields are now `public` from package-private
>         - `$Heap` constructor is now `public` from package-private
>     - `UberGpuBuffer` now takes in the `StagingBuffer` instead of the `GpuDevice`, buffer size `int`, and `GraphicsWorkarounds`
>         - `uploadStagedAllocations` now takes in the `StagingBuffer$Uploader` instead of the `CommandEncoder`
>         - `$UberGpuBufferHeap` constructor is now `public` from package-private
>     - `VertexFormat`
>         - `UNKNOWN_ELEMENT` is removed
>         - `MAX_VERTEX_ELEMENTS` - The maximum number of elements or attributes that can be defined on the format.
>         - `uploadImmediateVertexBuffer`, `uploadImmediateIndexBuffer` are removed
>         - `builder` now takes in an `int` for how many instances pass between updates to the attribute
>         - `getStepRate` - How many instances pass between updates to the attribute.
>             - OpenGL is 0 per vertex.
>         - `getElementAttributeNames` is removed
>         - `getOffsetsByElement` is removed
>         - `getOffset` replaced by `getElement`
>         - `contains` now takes in the `String` attribute name instead of the `VertexFormatElement`
>         - `getElementsMask`, `getElementName` are removed
>         - `$Builder`
>             - `add` -> `addAttribute`, not one-to-one
>             - `padding` is removed
>         - `$IndexType` -> `com.mojang.blaze3d.IndexType`
>         - `$Mode` -> `PrimitiveTopology`
>     - `VertexFormatElement` now takes in the `GpuFormat` instead of the `$Type`, normalized `boolean`, and count `int`; the `String` attribute name instead of the `int` id, and the `int` offset` instead of the index
>         -  Constant `VertexFormatElement` fields are now removed
>             - They are constructed when creating the `VertexFormat`
>         - `MAX_COUNT` is removed
>         - `register` is removed
>         - `mask`, `byteSize` are removed
>         - `byId`, `elementsFromMask` are removed
>         - `type`, `$Type` -> `GpuFormat` following the `_` in the entries
>         - `normalized` -> `GpuFormat` if `NORM` is in the entry name
>         - `count` -> `GpuFormat` counting `R`, `G`, `B`, `A` in the initial characters before the size
>     - `VertexMultiConsumer` class is removed
>         - Usage replaced by `SheetedDecalTextureGenerator` for the foil buffer

## 26.2 prose sections on rendering (verbatim)

> ### Blend Factors
>
> `DestFactor` and `SourceFactor` have been combined into a single `BlendFactor`, which is used within `BlendEquation`s and `BlendFunction`s. How these `BlendFactor`s are applied is through `BlendOp`, which defines the operation to merge the source and destination. `BlendOp#ADD` is used as a default when not specified in a `BlendFunction`.

> ### GPU Formats
>
> `TextureFormat` and the buffer formats of `VertexFormatElement` have been replaced by the `GpuFormat` enum. The names of each enum entry can be broken down into three components: a channel with their bit size, an underscore, and finally the data type. Here are some examples:
>
> - `R8_UNORM`: An unsigned normalized value with a single 8-bit red channel.
> - `RGBA32_SINT`: A signed integer with 32-bit red, green, blue, and alpha channels.
> - `D32_FLOAT_S8_UINT`: A float with a 32-bit depth channel, and a unsigned integer with an 8-bit stencil channel.
>
> The previous data types of `RGBA8`, `RED8`, `RED8I`, `DEPTH32` can be represented using one of these `GpuFormat`s, though their mappings do not have one-to-one parity for the GL codes previously used.

> ### Rewriting Vertex Formats
>
> `VertexFormat` along with `VertexFormatElement` has been partially rewritten into a more dynamic framework. There is no longer a set of existing `VertexFormatElement`s. Instead, the elements are constructed when building the `VertexFormat` by adding attributes via `$Builder#addAttribute`. A `VertexFormat` can define at most sixteen elements or attributes, each providing at least the name and `GpuFormat`.
>
> `VertexFormat$IndexType` and `VertexFormat$Mode` have also been moved into their own `IndexType` and `PrimitiveTopology` enums, respectively. This is because `RenderPipeline`s can now define up to sixteen vertex buffers with different `VertexFormat`s, but must be applied to the same `PrimitiveTopology`.

> ### Multiple Color Target States
>
> With this expansion, `RenderPipeline`s can also specify up to eight `ColorTargetState`s. This must match the number of color attachments specified when creating a `RenderPass` (typically 1 in vanilla usecases).

> ### Bind Group Layouts
>
> `BindGroupLayout` is a collection of sampler names and `$UniformDescription`s, replacing the single list of sampler names and `RenderPipeline#UniformDescription`. Due to this separate object, `RenderPipeline$Snippet`s that were made up of only samplers and uniforms are now decoupled, allowing for a modular, non-repeatable implementation when dealing with many pipelines and layouts. All vanilla layouts are stored in `BindGroupLayouts` and are attached to a `RenderPipeline` via `RenderPipeline$Builder#withBindGroupLayout`.
>
> ```java
> public static final BindGroupLayout EXAMPLE_LAYOUT = BindGroupLayout.builder()
>     // Specifies that the shaders have a 'Sampler0' sampler
>     .withSampler("Sampler0")
>     // Specifies that the shaders have access to the 'Globals' uniform
>     .withUniform("Globals", UniformType.UNIFORM_BUFFER)
>     .build();
>
> public static final RenderPipeline EXAMPLE_PIPELINE = RenderPipeline.builder()
>     .withLocation(Identifier.fromNamespaceAndPath("examplemod", "pipeline/example"))
>     .withVertexShader(Identifier.fromNamespaceAndPath("examplemod", "example_shader"))
>     .withFragmentShader(Identifier.fromNamespaceAndPath("examplemod", "example_shader"))
>     .withVertexBinding(0, DefaultVertexFormat.ENTITY)
>     .withPrimitiveTopology(PrimitiveTopology.QUADS)
>     // Specify the layouts to use
>     .withBindGroupLayout(EXAMPLE_LAYOUT)
>     // Can use multiple layouts
>     .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
>     .build();
> ```
>
> Note that all `BindGroupLayout`s added to a `RenderPipeline` must not contain any duplicate entries (e.g. two shaders named `Sampler0`, two uniforms named `Globals`) and by extension the same `BindGroupLayout` multiple times, or an error will be thrown during the shader compilation process.

> ### Render Areas
>
> `RenderPass`es can now define a rectangle within a texture to draw its data to via `CommandEncoder#createRenderPass`. If this not specified, it defaults to the size of the color `GpuTextureView`.

## 26.2 — shader resource layout (verbatim)

> - `assets/minecraft/shaders/core`
>     - `rendertype_text` -> `text`
>     - `rendertype_text_background` -> `text_background`
>     - `rendertype_text_background_see_through` -> `text_background` with `IS_SEE_THROUGH` shader define
>     - `rendertype_text_intensity` -> `text` with `IS_GRAYSCALE` shader define
>     - `rendertype_text_intensity_see_through` -> `text` with `IS_GRAYSCALE` and `IS_SEE_THROUGH` shader defines
>     - `rendertype_text_see_through` -> `text` with `IS_SEE_THROUGH` shader define

(This is a **shader define** mechanism reference, but the primer does not name a `ShaderDefines` class in 26.2 text I retrieved.)

## 26.2 — Vulkan-related citation from Fabric (non-primer)

Source: https://fabricmc.net/2026/06/15/262.html
> 26.2 introduces the ability for the backend to be changed between the default OpenGL backend and an experimental Vulkan backend, with OpenGL planned to be removed once the Vulkan backend is stable. Developers making use of raw OpenGL calls rather than going through the Blaze3D API will need to migrate for their mods to work.

Note this page also links directly to the primer source at the fork: https://github.com/ChampionAsh5357/neoforged-github/blob/port%2F26.2/primers/26.2/index.md

## 26.2 — NOT retrieved

- Anything about `net.minecraft.client.renderer.PostChain`, `PostPass`, `EffectInstance`, `ShaderInstance` — no matches found in the retrieved 26.2 text.
- `GlslPreprocessor` — no matches in retrieved 26.2 text.
- `assets/<ns>/shaders/post/*.json` and `assets/<ns>/shaders/program/*.json` layout — no matches in retrieved 26.2 text.
- `MainTarget` — no matches in retrieved 26.2 text.
- The tail of the 26.2 file (from roughly `net.minecraft.client` onward, including the client-renderer package listing beyond line ~719 of the spill) was truncated. So any 26.2 statements about `net.minecraft.client.renderer.PostChain` / `PostPass` / `EffectInstance` / `ShaderInstance` that live in the `net.minecraft.client.*` bullet region were NOT retrieved.

---

# 26.3 — 26.2 -> 26.3

Source: https://raw.githubusercontent.com/neoforged/.github/main/primers/26.3/index.md
(Fetched text spill: `C:\Users\HP\AppData\Local\Temp\dsh-spill-jk9fcS\session-09dd4567f8a5\879ac4d12d2e-web_fetch.txt`)

Headings visible: `## Pack Changes`, `## There's Always a Client Rewrite` with `### LWJGL with SDL`, `### Renderpearl`, `### No More Texture Override Shenanigans`, `### Shader Extensions and Layouts`, `### Order Independent Transparency (OIT)`, `### Feature Phase Changes`, `### Palette Metadata`, `### Item Quads`, `### Submitting Crumbling Overlays`.

## Renderpearl — Blaze3d split (verbatim)

> ### Renderpearl
>
> The render API that previously lived in Blaze3d has been split into a separate project called Renderpearl. All rendering classes that originally lived in Blaze3d have been migrated to `com.mojang.renderpearl`.
>
> Packages in Renderpearl are roughly structured like so:
>
> - `com.mojang.renderpearl.api.*` - APIs for the elements the vanilla game interacts with.
> - `com.mojang.renderpearl.backend.*` - The objects that manage the actual logic to communicate with a graphics library.
> - `com.mojang.renderpearl.frontend.*` - The implemented APIs that the vanilla game interacts with. These classes handle most of the validation.

## RenderSystem in 26.3 (verbatim)

> ### No More Texture Override Shenanigans
>
> `RenderSystem#outputColorTextureOverride` and `outputDepthTextureOverride` have been fully removed, closing one of the shenanigans then it came to writing to the GPU texture, especially for `FeatureRenderDispatcher`. Now, each try-with-resources creates the `RenderPass` to specify the textures to output to, along with any other information, like the `FeatureRenderDispatcher$PreparedFrame`:
>
> ```java
> // Setup everything possible before the render frame
>
> // Create the texture to write to
> GpuDevice device = RenderSystem.getDevice();
> GpuTexture texture = device.createTexture(/*...*/);
> GpuTexture textureView = device.createTextureView(texture);
> GpuTexture depthTexture = device.createTexture(/*...*/);
> GpuTexture depthTextureView = device.createTextureView(depthTexture);
> device.createCommandEncoder().clearColorAndDepthTextures(texture, new Vector4f(0f), depthTexture, 0);
>
> // ...
>
> // Write data for rendering
> SubmitNodeStorage storage = new SubmitNodeStorage();
> storage.submitCustomGeometry(/*...*/);
>
> // ...
>
> // Render to the texture.
> // This should only contain those critical to modify the render pass with
> // the correct info. Everything else should've been done prior.
> try (
>     FeatureRenderDispatcher.PreparedFrame frame = featureRenderDispatcher.prepareFrame(storage);
>     RenderPass pass = RenderSystem.getDevice()
>         .createCommandEncoder()
>         .createRenderPass(() -> "Example", textureView, Optional.empty(), depthTextureView, OptionalDouble.empty());
> ) {
>     // Setup anything required by the pass.
>     RenderSystem.bindDefaultUniforms(pass);
>
>     //...
>
>     // Render to the textures.
>     FeatureRenderDispatcher.renderAllFeatures(pass, frame);
> }
> ```

## Shader GLSL / GlslPreprocessor replacement (verbatim, 26.3)

> ### Shader Extensions and Layouts
>
> The Shader GLSL now has some additional features and changes regarding the the underlying syntax as Shaderc is now used for compilation on both OpenGL and Vulkan via the `GlslCompiler`.
>
> As such, `#moj_import` was renamed to `#include` to import functions from other shaders. Additionally, both the `in` and `out` uniforms must specify their location in the layout to maintain a fixed read/write location when passing from vertex to fragment. This is especially valuable when some are included based on shader defines.
>
> Speaking of defines, `GlslCompiler` provides three macros: `RENDERPEARL_DEPTH_IS_ZERO_TO_ONE` for if the device coordinates are between 0-1 (always defined for Vulkan, if the machine has the `GL_ARB_clip_control` extension for OpenGL), `RENDERPEARL_EXPLICIT_DEPTH_INVARIANCE` for if the depth must be explicitly set (currently only Apple silicon machines), and `RENDERPEARL_INSTANCE_INDEX_INCLUDES_BASE_INSTANCE` for if the `gl_InstanceIndex` parameter either represents the instance index or `gl_InstanceID` (always defined for Vulkan, if the machine has the `GL_ARB_shader_draw_parameters` extension for OpenGL).
>
> Finally, most vanilla shaders require the machine to have the `GL_ARB_separate_shader_objects` extension as a requirement of Vulkan, failing if not present. This is functionally the same as requiring a higher GLSL version.
>
> ```glsl
> // In some shader file.
>
> // Required extensions for the shader.
> #extension GL_ARB_separate_shader_objects : require
>
> // Includes for functions.
> #include <minecraft:light.glsl>
> #include <minecraft:fog.glsl>
> #include <minecraft:dynamictransforms.glsl>
> #include <minecraft:projection.glsl>
> #include <minecraft:sample_lightmap.glsl>
>
> // Layout followed by the location.
> layout(location = 0) in vec3 Position;
> layout(location = 1) in vec4 Color;
> layout(location = 2) in vec2 UV0;
> layout(location = 3) in ivec2 UV1;
> layout(location = 4) in ivec2 UV2;
>
> // The location specified by the out uniforms in
> // the vertex shader must match the `in`s in the
> // fragment shader.
> // Since it matches by location now, the variable names
> // are ignored, but it is recommended to keep them consistent.
> layout(location = 0) out float sphericalVertexDistance;
> layout(location = 1) out float cylindricalVertexDistance;
> ```

## OIT shader defines (verbatim, 26.3) — key excerpt

> Minecraf now supports the use of Order-Independent Transparency (OIT) through its improved transparency option. [...] Vanilla specifically uses [Wavelet OIT](https://arxiv.org/pdf/2201.00094) [...]
>
> For our understanding, OIT runs through three stages: depth bounds, transmittance, and accumulate. In both depth bounds and transmittance, only the alpha is updated. It is only during the accumulate phase that the pixel color is set. As such, OIT requires updating both the vertex and the fragment shader. For both depth bounds and transmittance, alpha only OIT is denoted by the shader define `OIT_ALPHA_ONLY`, which you will see scattered across the supported shaders.

(Full vertex/fragment GLSL examples and the `OitPipelineSet` / `RenderSetup#setOitPipelines` / `PreparedRenderType#drawFromBufferOit` samples were retrieved and are in the spill file at the path above, lines ~160-355.)

## 26.3 package renames — the definitive mapping table (verbatim)

These are quoted from the 26.3 primer's 26.2->26.3 bullet list (spill lines 513-838):

>     - `GpuDeviceLossException` -> `renderpearl.api.device.GpuDeviceLossException`
>     - `GpuFormat` -> `renderpearl.api.GpuFormat`
>     - `GpuOutOfMemoryException` -> `renderpearl.api.device.GpuOutOfMemoryException`
>     - `IndexType` -> `renderpearl.api.pipeline.IndexType`
>     - `PrimitiveTopology` -> `renderpearl.api.pipeline.PrimitiveTopology`
>     - `GpuBuffer` -> `renderpearl.api.buffers.GpuBuffer`, now an interface from a class
>         - The class portion has been moved into `renderpearl.backend.common.BaseGpuBuffer`
>     - `GpuBufferSlice` -> `renderpearl.api.buffers.GpuBufferSlice`
>     - `GpuFence` -> `renderpearl.api.commands.GpuFence`
> - `com.mojang.blaze3d.opengl.*` -> `renderpearl.backend.opengl.*`
>     - `BindGroupLayout` -> `renderpearl.api.pipeline.BindGroupLayout`
>     - `BlendEquation` -> `renderpearl.api.pipeline.BlendEquation`
>     - `BlendFunction` -> `renderpearl.api.pipeline.BlendFunction`
>     - `CompiledRenderPipeline` -> `renderpearl.api.pipeline.CompiledRenderPipeline`
>     - `DepthStencilState` -> `renderpearl.api.pipeline.DepthStencilState`
>     - `RenderPipeline` -> `renderpearl.api.pipeline.RenderPipeline`
>     - `BlendFactor` -> `renderpearl.api.pipeline.BlendFactor`
>     - `BlendOp` -> `renderpearl.api.pipeline.BlendOp`
>     - `CompareOp` -> `renderpearl.api.pipeline.CompareOp`
>     - `PolygonMode` -> `renderpearl.api.pipeline.PolygonMode`
>     - `GpuDebugOptions` -> `renderpearl.api.device.GpuDebugOptions`
>     - `ShaderSource` -> `renderpearl.api.pipeline.ShaderSource`, now implements `AutoCloseable`
>     - `ShaderType` -> `renderpearl.api.pipeline.ShaderType`
>     - `UniformType` -> `renderpearl.api.pipeline.UniformType`
>     - `BackendCreationException` -> `renderpearl.api.device.BackendCreationException`
>     - `CommandEncoder` -> `renderpearl.frontend.FrontendCommandEncoder`
>         - Definition interface in `renderpearl.api.commands.CommandEncoder`
>     - `CommandEncoderBackend` -> `renderpearl.backend.api.CommandEncoderBackend`
>     - `DeviceFeatures` -> `renderpearl.api.device.DeviceFeatures`
>     - `DeviceInfo` -> `renderpearl.api.device.DeviceInfo`
>     - `DeviceLimits` -> `renderpearl.api.device.DeviceLimits`
>     - `DeviceType` -> `renderpearl.api.device.DeviceType`
>     - `GpuBackend` -> `renderpearl.api.device.GpuBackend`
>     - `GpuDevice` -> `renderpearl.frontend.FrontendGpuDevice`, no longer taking in the `Runnable` for the critical shader loader
>         - Definition interface in `renderpearl.api.device.GpuDevice`
>     - `GpuDeviceBackend` -> `renderpearl.backend.api.GpuDeviceBackend`
>     - `GpuQuery` -> `renderpearl.api.commands.GpuQuery` now implements `UncheckedAutoCloseable`
>     - `GpuQueryPool` -> `renderpearl.api.commands.GpuQueryPool` now implements `UncheckedAutoCloseable`
>     - `GpuSurface` -> `renderpearl.frontend.FrontendGpuSurface`
>         - Definition interface in `renderpearl.api.device.GpuSurface`
>     - `GpuSurfaceBackend` -> `renderpearl.backend.api.GpuSurfaceBackend`
>     - `HintsAndWorkarounds` -> `renderpearl.api.device.HintsAndWorkarounds`
>     - `RenderPass` -> `renderpearl.frontend.FrontendRenderPass`
>         - Definition interface in `renderpearl.api.commands.RenderPass`
>     - `RenderPassBackend` -> `renderpearl.backend.api.RenderPassBackend`
>     - `RenderPassDescriptor` -> `renderpearl.api.commands.RenderPassDescriptor`, now a record
>     - `RenderSystem`  [continued below]
>     - `SurfaceException` -> `renderpearl.api.device.SurfaceException`
>     - `TracyGpuProfiler` -> `renderpearl.frontend.TracyGpuProfiler`
> - `com.mojang.blaze3d.textures.*` -> `renderpearl.api.textures.*`
>         - The class portion has been moved into `renderpearl.backend.common.BaseGpuTexture`
>         - The class portion has been moved into `renderpearl.backend.common.BaseGpuTextureView`
> - `com.mojang.blaze3d.util.TransientBlockAllocator` -> `renderpearl.backend.util.TransientBlockAllocator`
>     - `VertexFormat` -> `renderpearl.api.vertex.VertexFormat`
>     - `VertexFormatElement` -> `renderpearl.api.vertex.VertexFormatElement`
> - `com.mojang.blaze3d.vulkan.*` -> `renderpearl.backend.vulkan.*`
>     - `VulkanBackend`
>         - Constants moved to `VulkanFeatureSets`
>             - `REQUIRED_DEVICE_EXTENSIONS`, `REQUIRED_DEVICE_FEATURES` merged into `VulkanFeatureSets#REQUIRED_FEATURESET`
>     - `VulkanBindGroupLayout` record is removed
>     - `VulkanConst#toVk` now has an overload that takes in the `ShaderType`
>     - `VulkanDevice` no longer takes in the `ShaderSource`
>     - `VulkanGpuTextureView` now extends `BaseGpuTextureView` instead of `GpuTextureView`
>     - `VulkanRenderPass#VALIDATION` -> `FrontendGpuDevice#STRICT_VALIDATION`
>         - `$TextureViewAndSampler` -> `renderpearl.util.TextureViewAndSampler`
>     - `VulkanRenderPipeline` is now a final class instead of a record
>         - The constructor no longer takes in a `RenderPipeline` and takes in a `LongList` shader modules instead of `long` modules for the vertex and fragment, the `long` descriptor set, and a list of `BindGroupLayout$UniformDescription`s instead of the `VulkanBindGroupLayout`
>         - `compile` now takes in the `BackendRenderPipeline$CreateInfo` instead of the `VulkanBindGroupLayout`, `RenderPipeline`, and `long` modules
>     - `VulkanUtils`
> - `com.mojang.blaze3d.vulkan.checkpoints.*` -> `renderpearl.backend.vulkan.checkpoints.*`
>     - `GlslCompiler` -> `renderpearl.frontend.shaders.GlslCompiler`
>     - `ShaderCompileException` -> `renderpearl.util.ShaderCompileException`
> - `com.mojang.blaze3d.vulkan.init.*` -> `renderpearl.backend.vulkan.init.*`
>     - `VulkanPNextStruct` now takes in the next struct `Class`
> - `com.mojang.renderpearl.backend.api.SpvModule` - An intermediate module representing the compiled shader in SPV.
> - `com.mojang.renderpearl.backend.opengl.GlPipelineRecompiler` - Handles recompiling the SPV shader module into GLSL.
> - `com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets` - The feature sets for vulkan used by vanilla.
> - `com.mojang.renderpearl.frontend.shaders`
> - `com.mojang.renderpearl.util.UncheckedAutoCloseable` - An `AutoCloseable` that removes the `Exception` throws from `close`.

**IMPORTANT GAP:** the `- `RenderSystem`` bullet's sub-items (line 739 of the spill) were **cut off by an in-page elision** in the fetched text — the `RenderSystem` entry appears with no children and then the list jumps to `SurfaceException`. The actual `RenderSystem` sub-bullets for 26.3 are **NOT retrieved**. Same for `com.mojang.renderpearl.frontend.shaders` (line 834), which has no children in the retrieved text.

## 26.3 — NOT retrieved

- `RenderSystem` sub-bullets (elided in the fetched text).
- `com.mojang.renderpearl.frontend.shaders` sub-bullets (elided).
- Anything about `net.minecraft.client.renderer.PostChain`, `PostPass`, `EffectInstance`, `ShaderInstance` in 26.3 — no matches found in the retrieved text.
- `assets/<ns>/shaders/post/*.json` and `program/*.json` layout in 26.3 — the retrieved text only shows `assets/minecraft/post_effect/transparency.json` is removed and part of the `assets/minecraft/shaders/core` listing (which itself is cut mid-word: "`blit_depth.fsh` - A frget` is removed").
- The tail of 26.3 (from `## Registries and Data Components` onward is present; content after line ~1579 was truncated).

---

# Explicit list of what could NOT be retrieved and why

1. **All of 26.1's rendering/Blaze3d/shader content.** The file is 307,202 bytes; `web_fetch` truncates at ~100KB of formatted text at byte 0. No mirror, proxy, CDN, GitHub API variant, or shell download could bypass the cap (shell has no outbound network at all; every proxy either 4xx/5xx'd or redirected back to the capped raw URL). **I have zero verbatim 26.1 rendering text.**
2. **26.1 `GlStateManager` removal/deprecation statements.** Not retrieved (see #1).
3. **26.1 `RenderSystem#setShader`/`blendFunc`/`depthFunc`/`setShaderTexture` removals.** Not retrieved. I can only say that by 26.2 the primer's `RenderSystem` bullet lists `flipFrame` and `getApiDescription` as removed and `shutdownRenderer` as new — which implies these methods were handled in 26.1, but **that is inference and I will not assert it as a quote**.
4. **The Algolia DocSearch index.** Could not be used: `/settings` returns 403 "Method not allowed with this API key"; `GET /1/indexes/neoforged/query` returns 404 "ObjectID does not exist" (Algolia requires an HTTP POST body, which `web_fetch` cannot issue). Per the parent's independent check, the index also does not contain the 26.x primer pages.
5. **26.2 `net.minecraft.client.renderer.PostChain` / `PostPass` / `EffectInstance` / `ShaderInstance`.** No matches in the retrieved 26.2 span, and the region where a `net.minecraft.client.renderer.*` bullet list would appear was past the truncation point.
6. **26.2 `MainTarget`.** No matches in retrieved 26.2 text.
7. **26.2/26.3 `GlslPreprocessor` by name.** No matches. (26.3 documents the replacement mechanism — `#moj_import` -> `#include`, compiled via `GlslCompiler`/Shaderc — without naming a `GlslPreprocessor` class.)
8. **`assets/<ns>/shaders/post/*.json` and `program/*.json` resource-layout documentation.** Not found in any retrieved text. Only `assets/minecraft/shaders/core/*` renames (26.2) and `assets/minecraft/post_effect/transparency.json` removal (26.3) were retrieved.
9. **26.3 `RenderSystem` sub-bullets and `com.mojang.renderpearl.frontend.shaders` sub-bullets** — present as headings but with their children elided by the fetch formatting.
10. **Sidebar TOC/anchor lists for the 26.1/26.2/26.3 docs pages.** The docs site sidebar returned by `web_fetch` shows only the top-level page links (`/primer/docs/26.1/`, `/26.2/`, `/26.3/`, plus `/26.1/neo`), not the per-heading anchors, so it could not be used to enumerate rendering section names for 26.1.
