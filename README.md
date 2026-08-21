# HomeForge AR

**Premium Android AR Home Remodeling App**  
Scan any room → place real products → view in interactive room plan → save projects.

**Repository**: https://github.com/jay117king/HomeForge-AR

## What works right now (complete core loop)

1. **AR Scan** – live ARCore camera, plane detection (floor = blue grids, walls = orange), tape measure (tap or button), Lock / Unlock / Reset
2. **Finalize Room** – stores measured dimensions into `ScanResult`
3. **3D Room View** – interactive top-down room plan (pinch zoom, drag pan), shows real dimensions
4. **Product catalog** – search + length/width filters, seed products with photos & prices
5. **Place products** – tap a product to add/remove it in the room
6. **Save Project** – persists room + placed items to local Room database
7. **My Projects** – list, open, delete saved projects

## How to get the APK

### Requirements
- Android Studio Ladybug or newer
- JDK 17
- Physical phone with **ARCore + Depth API** (Pixel 6+, Samsung S21+, etc.)

### Steps
```bash
git clone https://github.com/jay117king/HomeForge-AR.git
cd HomeForge-AR
```

1. Open the folder in Android Studio.
2. Wait for Gradle sync to finish.
3. Connect your ARCore phone via USB (enable USB debugging).
4. Click the green **Run** button – this installs a debug APK on the device.

**To produce a shareable APK file:**
- Menu → **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- When finished click **locate**
- APK path: `app/build/outputs/apk/debug/app-debug.apk`

**Release APK (optional):**
- Create a keystore (Build → Generate Signed Bundle / APK)
- Select release build type
- Output: `app/build/outputs/apk/release/app-release.apk`

## App flow

1. Open app → **Scan a Room**
2. Grant camera permission
3. Move phone slowly – colored grids appear on surfaces
4. Tap screen or press **Measure** to set two points → distance appears
5. Press **Lock** when satisfied
6. Press **Finalize Room**
7. In the room view: search products, adjust dimension filters, tap products to place them
8. Press the **Save** icon → name the project
9. From Home → **My Projects** to see saved rooms

## Tech stack

- Kotlin + Jetpack Compose + Material 3
- ARCore 1.46 (Depth + planes)
- Custom OpenGL ES plane renderer
- Room (SQLite) for projects & products
- Coil for product images
- Filament included for future full 3D / GLB support

## Notes

- Emulators **cannot** run ARCore Depth – use a real device.
- First launch seeds 7 sample products automatically.
- Filament full PBR materials require `.filamat` assets (skeleton is present; production 3D can be added later without changing the rest of the app).

## License

Apache 2.0
