Changes since start (2026-01-29)

- Builds
  - Debug build assembled (assembleDebug), then clean rebuild to fix startup crash.
  - Release build assembled (assembleRelease).
- Runtime verification
  - Investigated startup crash on device; resolved by clean rebuild.
- Device installs
  - Debug APK installed via adb.
  - Release APK installed via adb.
- Repository
  - Initial import committed on branch sport-engine.
  - Branch sport-engine pushed to origin.
- Build tooling
  - Updated Gradle wrapper to 8.14.4 for Java 24 compatibility.
- vMix package
  - Added `com.kvl.cyclotrack.vmix` package with TimedValue, EmaFilter, LiveDataHub, MeasurementFrame, FrameEngine, VmixMapper, and VmixHttpServer.
  - Added NanoHTTPD dependency for the HTTP server.
  - Wired vMix server + frame engine into TripInProgressService and started feeding LiveDataHub from sensors/GPS/TripProgress.

Notes
- vMix server starts with TripInProgressService and serves JSON on port 8080.
