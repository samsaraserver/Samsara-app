@echo off
setlocal enabledelayedexpansion

set "GRADLE_PROPS=gradle.properties"
set "JDK11_FOUND="
set "JDK17_FOUND="
set "NDK_FOUND="

echo Detecting JDK installations...

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        for /f "tokens=*" %%i in ('"%JAVA_HOME%\bin\java" -version 2^>^&1 ^| findstr /C:"version"') do (
            echo %%i | findstr /C:"\"11." >nul && (
                set "JDK11_FOUND=%JAVA_HOME%"
                echo Found JDK 11 at: !JDK11_FOUND! ^(JAVA_HOME^)
            )
            echo %%i | findstr /C:"\"17." >nul && (
                set "JDK17_FOUND=%JAVA_HOME%"
                echo Found JDK 17 at: !JDK17_FOUND! ^(JAVA_HOME^)
            )
        )
    )
)

for %%p in (
    "C:\Program Files\Microsoft\jdk-11*"
    "C:\Program Files\Eclipse Adoptium\jdk-11*"
    "C:\Program Files\Java\jdk-11*"
    "C:\Program Files\OpenJDK\jdk-11*"
    "C:\Program Files\Oracle\jdk-11*"
) do (
    if exist "%%~p\bin\javac.exe" (
        if not defined JDK11_FOUND (
            set "JDK11_FOUND=%%~p"
            echo Found JDK 11 at: !JDK11_FOUND!
        )
    )
)

for %%p in (
    "C:\Program Files\Microsoft\jdk-17*"
    "C:\Program Files\Eclipse Adoptium\jdk-17*"
    "C:\Program Files\Java\jdk-17*"
    "C:\Program Files\OpenJDK\jdk-17*"
    "C:\Program Files\Oracle\jdk-17*"
) do (
    if exist "%%~p\bin\javac.exe" (
        if not defined JDK17_FOUND (
            set "JDK17_FOUND=%%~p"
            echo Found JDK 17 at: !JDK17_FOUND!
        )
    )
)

if not defined JDK11_FOUND (
    where java >nul 2>&1 && (
        for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /C:"version"') do (
            echo %%i | findstr /C:"\"11." >nul && (
                for /f "tokens=*" %%j in ('where java') do (
                    set "JAVA_PATH=%%j"
                    set "JDK11_FOUND=!JAVA_PATH:\bin\java.exe=!"
                    echo Found JDK 11 at: !JDK11_FOUND! ^(PATH^)
                )
            )
        )
    )
)

if not defined JDK11_FOUND (
    echo Missing JDK 11 required for Gradle
    echo Download from: https://adoptium.net/temurin/releases/
    set "MISSING_JDK=true"
)

if not defined JDK17_FOUND (
    echo Missing JDK 17 required for project compilation
    echo Download from: https://adoptium.net/temurin/releases/
    set "MISSING_JDK=true"
)

if defined MISSING_JDK (
    echo.
    echo Please install both JDK 11 and JDK 17
    exit /b 1
)

:check_ndk
echo Checking NDK installations...

if defined ANDROID_HOME (
    set "NDK_ROOT=%ANDROID_HOME%\ndk"
) else if defined ANDROID_SDK_ROOT (
    set "NDK_ROOT=%ANDROID_SDK_ROOT%\ndk"
) else (
    set "NDK_ROOT=%LOCALAPPDATA%\Android\Sdk\ndk"
)

for %%v in (20.1.5968771 21.0.6113669) do (
    if exist "!NDK_ROOT!\%%v\ndk-build.cmd" (
        echo Found NDK %%v
        set "NDK_FOUND=true"
    ) else (
        echo Missing NDK %%v
    )
)

if not defined NDK_FOUND (
    echo.
    echo Required NDK versions not found. Please install:
    echo - NDK 20.1.5968771
    echo - NDK 21.0.6113669
    echo.
    echo In Android Studio: Tools ^> SDK Manager ^> SDK Tools ^> NDK
    echo Or download from: https://developer.android.com/ndk/downloads
    echo.
    echo Expected location: !NDK_ROOT!\^<version^>
    exit /b 1
)

:update_gradle
echo Updating %GRADLE_PROPS%...

powershell -Command "(Get-Content '%GRADLE_PROPS%') -replace '^org\.gradle\.java\.home=.*', 'org.gradle.java.home=%JDK11_FOUND:\=\\%' | Set-Content '%GRADLE_PROPS%'"

echo JDK 11 path set for Gradle in %GRADLE_PROPS%
echo JDK 17 available for project compilation
echo All requirements satisfied
echo Setup complete
