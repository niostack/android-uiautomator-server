./gradlew build
./gradlew packageDebugAndroidTest
Copy-Item -Path app\build\outputs\apk\debug\app-debug.apk -Destination C:\Users\Administrator\AppData\Roaming\com.tikmatrix\bin\com.github.tikmatrix.apk -Force
Copy-Item -Path app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk -Destination C:\Users\Administrator\AppData\Roaming\com.tikmatrix\bin\com.github.tikmatrix.test.apk -Force
