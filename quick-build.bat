@echo off
echo Quick building SamsaraServer...
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
./gradlew.bat installDebug -x lint --daemon
echo.
echo SamsaraServer installed successfully!