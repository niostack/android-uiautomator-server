./gradlew build
./gradlew packageDebugAndroidTest
Move-Item -Path app\build\outputs\apk\debug\app-debug.apk -Destination ..\tiktok-matrix\src-tauri\bin\apk\com.github.tikmatrix.apk -Force
Move-Item -Path app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk -Destination ..\tiktok-matrix\src-tauri\bin\apk\com.github.tikmatrix.test.apk -Force
