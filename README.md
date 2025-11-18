<img width="1259" height="478" alt="image" src="https://github.com/user-attachments/assets/62fd6a8e-619a-49e2-a33a-b7d69b23cfba" />

<br>
<br>

<p align="center">
  <a href="https://sonarcube.samsaraserver.space/dashboard?id=Samsara-server">
    <img src="https://sonarcube.samsaraserver.space/api/project_badges/measure?project=Samsara-server&metric=sqale_rating&token=sqb_f1c814e55b2b022b69df00e731feb80af8c5d8ae" alt="Maintainability Rating" />
  </a>
  <a href="https://sonarcube.samsaraserver.space/dashboard?id=Samsara-server">
    <img src="https://sonarcube.samsaraserver.space/api/project_badges/measure?project=Samsara-server&metric=vulnerabilities&token=sqb_f1c814e55b2b022b69df00e731feb80af8c5d8ae" alt="Vulnerabilities" />
  </a>
  <br>
  <a href="https://samsaraserver.space/">
    <img src="https://img.shields.io/badge/Website-samsaraserver.space-blue" alt="Project Website" />
  </a>
  <a href="https://github.com/termux/termux-app">
    <img src="https://img.shields.io/badge/Termux-App-black?logo=github" alt="Termux App" />
  </a>
  <a href="https://github.com/termux/termux-packages">
    <img src="https://img.shields.io/badge/Termux-Packages-black?logo=github" alt="Termux Packages" />
  </a>
</p>

<br>

## Contributors

<table align="center">
  <tr>
    <td align="center">
      <a href="https://github.com/Max0xDay">
        <img src="https://github.com/Max0xDay.png" width="100px" alt="Max Day"/><br/>
        <b>Max Day</b>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/JMosselson">
        <img src="https://github.com/JMosselson.png" width="100px" alt="Jordan Mosselson"/><br/>
        <b>Jordan Mosselson</b>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/MatthewHubbard123">
        <img src="https://github.com/MatthewHubbard123.png" width="100px" alt="Matthew Hubbard"/><br/>
        <b>Matthew Hubbard</b>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/Tashil10">
        <img src="https://github.com/Tashil10.png" width="100px" alt="Tashil Koseelan"/><br/>
        <b>Tashil Koseelan</b>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/SharDai">
        <img src="https://github.com/ShortJai.png" width="100px" alt="Jaiyesh Pillay"/><br/>
        <b>Jaiyesh Pillay</b>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ST10313742">
        <img src="https://samsaraserver.space/assets/pfp/Thomas.jpeg?v=20251007" width="100px" alt="Thomas Raaths"/><br/>
        <b>Thomas Raaths</b>
      </a>
    </td>
  </tr>
</table>


# Samsara Server

Repurpose your old Android devices into powerful Linux servers with a sleek interface and zero command-line complexity.

## About

Samsara Server aims to breathe new life into unused Android phones and tablets by converting them into fully functional Linux servers. Built on the robust foundation of Termux, this application provides an intuitive wrapper interface that makes server management accessible to everyone, from beginners to system administrators.

The project combines the power of Alpine Linux with Android's hardware capabilities, creating an ecosystem where community-driven projects can flourish on repurposed devices. Whether you're running a home automation system, hosting a personal cloud, or experimenting with distributed computing, Samsara Server makes it simple.


## Video Demonstation link:

https://drive.google.com/file/d/1NC4OvCwg1LJ13fd0tHL3dkO8U1tJwzBU/view?usp=sharing


## Features

Transform your forgotten Android device into a powerful server with just a few taps. Samsara Server eliminates the complexity traditionally associated with Linux server deployment by providing an intelligent setup system that automatically configures Alpine Linux environments. No more wrestling with terminal commands or complex configuration files - the app handles everything from initial installation to ongoing package management.

**Effortless Server Deployment**
- Automated Alpine Linux environment initialization
- One-click package installation and dependency resolution  
- Real-time system monitoring with intuitive dashboards
- Remote administration through integrated web interface

The user experience prioritizes accessibility without sacrificing functionality. Whether you're a complete beginner or an experienced system administrator, the interface adapts to your comfort level. Users can choose to operate entirely without creating accounts, ensuring privacy-first operation, or leverage the full authentication system with biometric security for enhanced convenience and cloud synchronization.

**Authentication & Access Control**
Multiple login pathways accommodate different user preferences - traditional email/password combinations, GitHub integration for developers, biometric authentication for convenience, or completely anonymous operation for maximum privacy. The system seamlessly transitions between these modes based on user choice.

Community-driven development takes center stage through the integrated project marketplace. Users can discover, install, and manage community-created applications directly through the interface, while developers benefit from streamlined deployment tools and comprehensive system monitoring capabilities. The architecture supports custom implementations and extensions, making it ideal for both personal projects and educational environments.

**Developer & Community Features**
- Curated project marketplace with easy installation
- Real-time system health monitoring and alerting
- Extensible plugin architecture for custom functionality
- Integrated documentation and community support forums

Security operates on a local-first principle, ensuring your server remains under your control. Optional cloud integration provides convenience features like configuration backup and cross-device synchronization without compromising the core privacy-focused design. All network communications are optimized to minimize overhead during server operation, ensuring maximum performance from your repurposed hardware.

## Technical Foundation

**Architecture**
- Multi-module Gradle project
- Android SDK 21+ compatibility
- NDK integration for native performance
- PostgreSQL backend with Supabase

**Core Technologies**
- Java-based Android application
- Alpine Linux server environment
- Termux terminal emulation engine
- Modern Android UI components

## Project Structure

Samsara Server maintains a clear separation between the core Termux functionality and our custom wrapper interface. This architecture ensures stability while allowing innovation on top of the proven Termux foundation.

**Module Organization**
```
├── app/                     # Samsara Server UI and functionality
│   ├── src/main/java/       # Main application code (our development focus)
│   ├── src/main/res/        # UI resources, layouts, and drawables
│   └── src/main/assets/     # Server setup scripts and configurations
├── termux-shared/           # Core Termux libraries (unmodified)
├── terminal-emulator/       # Terminal engine (unmodified)
├── terminal-view/          # Terminal UI components (unmodified)
└── art/                    # Graphics and icon generation
```

**Development Philosophy**
All Samsara Server development occurs exclusively within the `/app` module. The core Termux modules (`termux-shared`, `terminal-emulator`, `terminal-view`) remain completely unmodified, ensuring compatibility with upstream Termux updates while maintaining the stability that millions of Termux users rely on.

This separation allows us to build innovative server management interfaces without interfering with the battle-tested terminal emulation that forms our foundation. Users get the reliability of Termux with the convenience of our purpose-built server management layer.

## Project Team

This project is developed and maintained by passionate developers who believe in the power of repurposing technology for good. We're building on the shoulders of giants and are grateful for the incredible work of the open-source community.

**Special Thanks**

We owe an enormous debt of gratitude to the Termux community and development team. This project would not exist without their groundbreaking work:

- **[Termux App](https://github.com/termux/termux-app)** - The foundation upon which Samsara Server is built. Their terminal emulation engine and Android integration provide the core functionality that makes our server capabilities possible.

- **[Termux Packages](https://github.com/termux/termux-packages)** - The massive repository of packages and the incredible effort to maintain Alpine Linux compatibility on Android. The dedication of these developers enables the rich ecosystem we build upon.

The Termux project has pioneered running Linux environments on Android, and their commitment to open-source development has created opportunities for projects like ours to exist. We really appreciate their work and contribution to their amazing community.

## License

This project is licensed under the Apache License 2.0 - see the full license terms for details.

The Apache License 2.0 provides the freedom to use, modify, and distribute this software while ensuring compatibility with the broader open-source ecosystem. This aligns with our commitment to keeping server technology accessible and community-driven.

## Community & Support

**Documentation**
Comprehensive guides and tutorials are available through the in-app documentation viewer, covering everything from basic setup to advanced server configurations. Complete documentation is also available at [samsaraserver.space/docs](https://samsaraserver.space/docs).

**Forums**
Join our community forums at [samsaraserver.space/forums](https://samsaraserver.space/forums) to share projects, ask questions, and collaborate with other users. The same account system works across the mobile app and web platform.

**Contributing**
We welcome contributions from developers, documentation writers, and community members. Whether you're fixing bugs, adding features, or helping other users, every contribution makes the project better.

## Building and Running

### Prerequisites

Building Samsara Server requires specific JDK versions due to the Termux foundation components. The project uses older JDK versions to maintain compatibility with the core Termux libraries that remain unmodified.

**Required Software**
- JDK 17.0.16.8-hotspot (required for compilation)
- JDK 11 (required for Gradle daemon execution)
- Android NDK 22.1.7171670 (native code compilation)
- Android SDK with API level 30
- Gradle build system (wrapper included)

**JDK Version Requirements**

The Termux modules (`termux-shared`, `terminal-emulator`, `terminal-view`) were built with JDK 11 and require this version for proper compatibility. However, the main app module and Android Gradle Plugin 4.2.2 require JDK 17 for compilation. This dual-JDK setup ensures both the legacy Termux components and modern Android build tools work correctly.

**NDK Integration**

The project uses Android NDK 22.1.7171670 for compiling native terminal emulation components. The NDK version is specified in `gradle.properties` and should not be changed as it affects terminal rendering and native library compatibility. The NDK compiles C11 code with optimization flags for the terminal emulator's performance-critical sections.

### Build Scripts

The project includes several build scripts for different scenarios. All scripts are designed for Windows environments using batch files.

**build.bat**
```
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
./gradlew.bat build -x lint
```
Standard production build that compiles all modules and generates release APKs. This script:
- Sets JAVA_HOME to JDK 17 for compilation
- Runs a complete build including all modules
- Skips lint checks for faster builds
- Outputs APKs to `app/build/outputs/apk/`

**build-with-jdk17.bat**
```
Accepts optional JAVA_HOME path as first argument
Falls back to detecting JDK 17 in standard locations
Calls build.bat with proper JDK configuration
```
Flexible build script that automatically detects JDK 17 installations or accepts a custom path. Use this when:
- JDK 17 is installed in a non-standard location
- Building on different machines with varying JDK paths
- Automating builds in CI/CD environments

Usage:
```bash
# Auto-detect JDK 17
./build-with-jdk17.bat

# Specify custom JDK path
./build-with-jdk17.bat "C:\custom\path\to\jdk-17"
```

**quick-build.bat**
```
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot
./gradlew.bat installDebug -x lint -x sonarqube --daemon
```
Rapid development build that installs directly to a connected Android device. This script:
- Uses Gradle daemon for faster subsequent builds
- Skips lint and SonarQube analysis
- Installs debug APK immediately after building
- Ideal for iterative development and testing

**run build to phone.bat**
```
./gradlew.bat -Dorg.gradle.java.home="C:\Program Files\Java\jdk-11" clean installDebug
```
Development build using JDK 11 explicitly for Gradle execution. This demonstrates the dual-JDK requirement:
- Forces Gradle daemon to use JDK 11
- Performs clean build to avoid cache issues
- Installs debug build to connected device
- Useful when JDK version conflicts occur

### Build Configuration

**gradle.properties**
Contains project-wide configuration including:
- `compileSdkVersion=30` - Target Android API level
- `ndkVersion=22.1.7171670` - Native development kit version
- `minSdkVersion=21` - Minimum Android 5.0 Lollipop
- `targetSdkVersion=28` - Target Android 9.0 Pie
- JVM arguments for Gradle daemon memory and module access

**Module Structure**
The build system compiles four modules in sequence:
1. `termux-shared` - Core Termux utilities and constants
2. `terminal-emulator` - Native terminal emulation engine
3. `terminal-view` - Terminal UI rendering components
4. `app` - Samsara Server interface and functionality

Only the `app` module should be modified during development. The three Termux modules remain unchanged to maintain compatibility with the upstream Termux project.

### Building Step by Step

1. **Environment Setup**
   ```bash
   # Verify JDK 17 installation
   "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot\bin\java" -version
   
   # Verify JDK 11 installation
   "C:\Program Files\Java\jdk-11\bin\java" -version
   
   # Verify NDK installation (if not auto-downloaded by Android Studio)
   echo %ANDROID_SDK_ROOT%\ndk\22.1.7171670
   ```

2. **First Build**
   ```bash
   # Clean build recommended for first compile
   ./gradlew.bat clean build -x lint
   ```

3. **Development Builds**
   ```bash
   # Connect Android device via ADB
   adb devices
   
   # Quick install to device
   ./quick-build.bat
   ```

4. **Production Builds**
   ```bash
   # Full build with all checks
   ./build.bat
   
   # Outputs located in:
   # app/build/outputs/apk/debug/
   # app/build/outputs/apk/release/
   ```

### Running the Application

**Installing on Device**
- Use `quick-build.bat` to install debug builds automatically
- Enable USB debugging on Android device (Settings > Developer Options)
- Accept USB debugging authorization prompt on device
- APK installs as "Termux" (package: com.termux)

**Testing Changes**
- App module changes compile quickly (30-60 seconds)
- Gradle daemon caches dependencies for faster rebuilds
- Use `--daemon` flag in gradlew commands for persistent daemon
- Clean builds take 2-5 minutes depending on hardware

**Troubleshooting Builds**

Common build issues and solutions:

**JDK Version Mismatch**
```
Error: Android Gradle plugin requires Java 11 to run
```
Solution: Ensure Gradle uses JDK 11 by setting org.gradle.java.home or using `run build to phone.bat`

**NDK Not Found**
```
Error: NDK is not installed
```
Solution: Install NDK 22.1.7171670 via Android Studio SDK Manager or set `ANDROID_NDK_HOME` environment variable

**Out of Memory**
```
Error: GC overhead limit exceeded
```
Solution: Increase Gradle JVM memory in `gradle.properties`:
```
org.gradle.jvmargs=-Xmx4096M
```

**ADB Device Not Found**
```
Error: No connected devices
```
Solution: Enable USB debugging, reconnect device, run `adb devices` to verify connection

## Acknowledgments

Beyond the Termux ecosystem, we're grateful to the broader open-source community that makes projects like this possible. From the Linux kernel developers to the Android Open Source Project, from package maintainers to documentation writers - thank you for creating the foundation that allows us to build something meaningful.

The vision of turning discarded devices into useful servers wouldn't be possible without the collective effort of thousands of developers who believe in accessible, sustainable technology.

---
