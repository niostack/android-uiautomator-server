adb install -r -t ..\tiktok-matrix\src-tauri\bin\apk\com.github.tikmatrix.apk
adb install -r -t ..\tiktok-matrix\src-tauri\bin\apk\com.github.tikmatrix.test.apk
adb shell am instrument -w -r -e debug false -e class com.github.tikmatrix.stub.Stub com.github.tikmatrix.test/androidx.test.runner.AndroidJUnitRunner