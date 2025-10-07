@echo off
setlocal
set "REQUESTED_JAVA_HOME=%~1"
if not "%REQUESTED_JAVA_HOME%"=="" (
    set "JAVA_HOME=%REQUESTED_JAVA_HOME%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
) else if exist "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
) else (
    echo JDK 17 installation not found. Pass the JAVA_HOME path as the first argument.
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0build.bat" %*
endlocal
