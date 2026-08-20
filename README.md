# HomeForge AR

**Premium Android AR Home Remodeling App**  
Scan any room → high-fidelity interactive 3D twin → place real store products as accurate to-scale 3D models (live AR overlay supported).

Built with free / near-zero-cost stack only.

## Core Features (Max MVP)

- **ARCore + Depth API** room scanning with visual tape measure & lock
- Multi-plane reconstruction → editable polygonal 3D room mesh
- High-fidelity 3D environment (Filament PBR, soft shadows, AO)
- Live AR overlay of placed products
- Product search + dimension filters (L×W×H)
- Seed catalog of real furniture + user-pasted URL support (Schema.org parsing)
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

## Project Structure

```
HomeForge-AR/
├── app/
│   ├── src/main/
│   │   ├── java/com/homeforge/ar/
│   │   │   ├── ar/           # ARCore session, depth, plane
│   │   │   ├── renderer/     # Filament setup, materials, lighting
│   │   │   ├── room/         # Scanning, mesh generation, editing
│   │   │   ├── product/      # Search, placement, 3D loading
│   │   │   ├── data/         # Room DB, repositories
│   │   │   ├── ui/           # Compose screens
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── cloud/
│   ├── model-gen/               # HF Space FastAPI wrapper
│   └── product-seed/            # JSON seed data
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Setup Instructions

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 34+
- Device with ARCore + Depth API support (Pixel 6+, Samsung S21+, etc.)
- JDK 17

### Local Build
```bash
git clone https://github.com/jay117king/HomeForge-AR.git
cd HomeForge-AR
# Open in Android Studio → Sync Gradle → Run on physical ARCore device
```

### Free Cloud Setup (later)
1. Create free Supabase project → copy URL + anon key into `local.properties`
2. Deploy TripoSR wrapper to Hugging Face Spaces (see `cloud/model-gen/`)
3. Pre-generate seed product GLBs on Colab free GPU and upload to Supabase Storage

## Development Phases

1. **Week 1-2**: Filament + ARCore basic session + depth
2. **Week 3-4**: Room mesh generation + tape measure
3. **Week 5**: 3D placement, orbit camera, collision
4. **Week 6**: Product seed + search + dimension filters
5. **Week 7**: Image-to-3D + GLB loading + billboard fallback
6. **Week 8**: AR overlay mode + polish + onboarding

## License

Apache 2.0 (same as Filament / ARCore samples)

---

**Status**: Project skeleton + architecture locked. Core modules being implemented.
