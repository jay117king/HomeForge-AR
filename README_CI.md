CI build badge will appear here once the workflow runs. Download the debug APK from the Actions artifacts: Actions -> Android CI - build debug APK -> app-debug-apk

Original README content: see full README in repo. The project produces a debug APK at app/build/outputs/apk/debug/app-debug.apk when the workflow runs.

How to install the APK on a device:
1. Connect your Android device via USB with USB debugging enabled.
2. In the GitHub Actions run, download the "app-debug-apk" artifact.
3. Install with adb:
   adb install -r app-debug.apk
