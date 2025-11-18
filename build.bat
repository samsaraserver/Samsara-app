@echo off
echo Building Samsara Server
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
./gradlew.bat build -x lint
echo.
echo SamsaraServer built