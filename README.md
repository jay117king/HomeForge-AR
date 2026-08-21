# HomeForge AR

**Premium Android AR Home Remodeling App**  
Scan any room → high-fidelity interactive 3D twin → place real store products as accurate to-scale 3D models (live AR overlay supported).

Built with free / near-zero-cost stack only.

**Live Repository**: https://github.com/jay117king/HomeForge-AR

## Core Features (Max MVP)

- **ARCore + Depth API** room scanning with visual tape measure & lock
- Multi-plane reconstruction → editable polygonal 3D room mesh
- High-fidelity 3D environment (Filament PBR, soft shadows, AO)
- Live AR overlay of placed products
- Product search + live dimension filters (L × W × H)
- Seed catalog of real furniture + user-pasted URL support (Schema.org)
- Free image-to-3D pipeline (TripoSR / InstantMesh on Hugging Face Spaces)
- Photo billboard fallback for instant placement
- Local Room DB + optional free-tier cloud backup

## Current Status

### Working now
- ✅ Full project + Gradle (Compose, ARCore, Filament, Room, Ktor, ML Kit, Coil)
- ✅ **Real ARCore camera feed** via GLSurfaceView + AndroidView
- ✅ **Plane detection & visualization** (blue grids = floor, orange = walls)
- ✅ **Live tape measure** – tap screen or press Measure, Lock / Unlock, Reset
- ✅ Plane count indicator + status chip
- ✅ Product search UI with dimension RangeSliders + product cards
- ✅ Room database + seed catalog (7 items)
- ✅ Material 3 theme, navigation, permissions handling

### Next up
1. Capture finalized room mesh from detected planes
2. Filament 3D room view + orbit camera
3. Product GLB / billboard placement in the 3D room
4. AR overlay mode (products in live camera)
5. Schema.org URL paste + image-to-3D pipeline

## How to Run

```bash
git clone https://github.com/jay117king/HomeForge-AR.git
```

1. Open in **Android Studio** (Ladybug or newer)
2. Sync Gradle
3. Connect a physical device with **ARCore + Depth API** support  
   (Pixel 6+, Samsung Galaxy S21+, etc.)
4. Run the app
5. Grant camera permission → tap **Scan a Room**
6. Move the phone slowly to detect floor & walls (colored grids appear)
7. Tap the screen or press **Measure** to set points → **Lock** when happy
8. Press **Finalize Room** when you have good coverage

## Tech Highlights

| Component        | Implementation                                      |
|------------------|-----------------------------------------------------|
| AR Session       | `ArSessionManager` + Depth + Horizontal/Vertical    |
| Camera + Planes  | Custom `GLSurfaceView` renderer (Background + Grid) |
| Measure          | `TapeMeasure` with hit-test on Plane / DepthPoint   |
| UI               | Jetpack Compose + Material 3                        |
| 3D Engine        | Filament (ready for next stage)                     |
| Data             | Room + seed JSON + ProductRepository                |

## License

Apache 2.0
