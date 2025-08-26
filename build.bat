@echo off
echo Building and installing SamsaraServer...
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
./gradlew.bat clean installDebug -x lint --daemon
echo.
echo SamsaraServer installed successfully!
pause