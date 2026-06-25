# Google Play release

Google Play accepts new Android apps as Android App Bundle files (`.aab`).

## Current release artifact

- Local bundle: `artifacts/FallOuch-release.aab`
- Build output: `app/build/outputs/bundle/release/app-release.aab`
- Version: `versionCode=1`, `versionName=1.0.0`

## Upload key

The upload key is intentionally not committed to git.

- Keystore: `artifacts/fall-ouch-upload-key.jks`
- Local signing config: `release-keystore.properties`
- Alias: `fall-ouch-upload`
- Certificate SHA-256: `56:AD:3A:82:BA:C7:C7:2C:6B:D9:E3:D5:88:EB:C6:97:AC:70:BB:6A:3D:FE:8E:78:93:CE:EB:C5:1D:7D:1A:4B`
- Keystore SHA-256: `f84eef488c295413dcc3facf96b99c158abd95413e0be187c192719aa0f3acb0`

Do not push `release-keystore.properties` or `*.jks` to GitHub. Keep a private backup in a password manager, encrypted disk, or another safe private location.

## Build

```bash
./gradlew clean test bundleRelease lintRelease --console=plain
cp app/build/outputs/bundle/release/app-release.aab artifacts/FallOuch-release.aab
```

## Verify

```bash
jarsigner -verify -verbose -certs artifacts/FallOuch-release.aab
```

Expected result includes `jar verified`.
