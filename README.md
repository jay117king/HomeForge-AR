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

## Tech Stack

| Layer              | Choice                          | Why |
|--------------------|---------------------------------|-----|
| Language           | Kotlin 100%                     | Modern, coroutines, ARCore first-class |
| UI                 | Jetpack Compose                 | Declarative, 60fps, dark/light easy |
| AR                 | ARCore + Depth API              | Plane + depth, occlusion |
| 3D Engine          | Filament (Google)               | PBR, native Android, Apache 2.0, small |
| Local DB           | Room (SQLite)                   | Projects, product cache |
| Networking         | Ktor Client                     | Lightweight |
| Cloud              | Supabase free tier              | Auth, Postgres, Storage |
| Image-to-3D        | Hugging Face Spaces (TripoSR)   | Free ZeroGPU / CPU |
| Product Data       | Seed DB + Schema.org parse      | No scraping |

## Current Status (Fully Scaffolded)

### Completed
- ✅ Full Gradle project (Compose, ARCore 1.46, Filament 1.56, Room, Ktor, ML Kit, Coil)
- ✅ AndroidManifest with ARCore + camera requirements
- ✅ MainActivity with permission + ARCore availability handling
- ✅ Material 3 theme (dark / light / dynamic)
- ✅ Complete Compose navigation (Home → Scan → 3D Room)
- ✅ ScanScreen with measure / lock / finalize controls
- ✅ RoomViewScreen with live product search + RangeSliders for L/W filters + product cards
- ✅ ArSessionManager (depth + horizontal/vertical planes)
- ✅ TapeMeasure class with lock / unlock / formatted distance
- ✅ FilamentRenderer skeleton
- ✅ Room database + DAOs + ProductRepository with seed loading
- ✅ Seed catalog (7 products) in assets + cloud folder
- ✅ Image-to-3D free pipeline documentation
- ✅ ProGuard rules, strings, themes, gitignore

### Still to implement (next coding sessions)
1. Real ARCore SurfaceView / GLSurfaceView inside ScanScreen + plane rendering
2. Full Filament engine init, room mesh loading, orbit camera, product GLB instancing
3. AR overlay mode (shared camera texture)
4. Schema.org URL parser for user-pasted product links
5. Hugging Face Space FastAPI wrapper + on-demand GLB download
6. Project save / load + screenshot export

## How to Run

```bash
git clone https://github.com/jay117king/HomeForge-AR.git
cd HomeForge-AR
# Open folder in Android Studio (Ladybug+)
# Sync Gradle
# Connect a physical ARCore Depth device (Pixel 6+, S21+, etc.)
# Run
```

The UI already works end-to-end with simulated data. Real AR + 3D rendering is the next layer.

## License

Apache 2.0
