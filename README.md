# HomeForge AR

**Premium Android AR Home Remodeling App**  
Scan any room → high-fidelity interactive 3D twin → place real store products as accurate to-scale 3D models (live AR overlay supported).

Built with free / near-zero-cost stack only.

**Repository**: https://github.com/jay117king/HomeForge-AR

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

## Current Project Status (Day 1)

✅ Repository created  
✅ Full Gradle setup (Compose, ARCore, Filament, Room, Ktor, ML Kit)  
✅ AndroidManifest with ARCore requirements  
✅ MainActivity + camera/ARCore availability checks  
✅ Material3 theme (dark/light + dynamic)  
✅ Compose navigation skeleton (Home → Scan → Room View)  
✅ ArSessionManager (depth + plane detection ready)  
✅ FilamentRenderer skeleton  
✅ Core data models (Project, Product, PlacedObject, RoomMesh)  
✅ Initial product seed catalog (tables, cupboards, taps, sofa)  

**Next immediate tasks**  
1. Wire ArSessionManager into a real SurfaceView / AndroidView in ScanScreen  
2. Implement basic plane visualization + tape measure UI  
3. Flesh out Filament room mesh loading + orbit camera  
4. Product search UI + dimension sliders  

## Setup Instructions

### Prerequisites
- Android Studio Ladybug or newer
- Android SDK 34+
- Physical device with ARCore + Depth API (Pixel 6+, Samsung S21+, etc.)
- JDK 17

### Local Build
```bash
git clone https://github.com/jay117king/HomeForge-AR.git
cd HomeForge-AR
# Open in Android Studio → Sync Gradle → Run on physical ARCore device
```

### Free Cloud Setup (later)
1. Create free Supabase project → copy URL + anon key into `local.properties`
2. Deploy TripoSR wrapper to Hugging Face Spaces
3. Pre-generate seed product GLBs on Colab free GPU and upload to Supabase Storage

## License

Apache 2.0
