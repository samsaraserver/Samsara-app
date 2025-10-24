# Samsara Server
[![Maintainability Rating](https://sonarcube.samsaraserver.space/api/project_badges/measure?project=Samsara-server&metric=sqale_rating&token=sqb_f1c814e55b2b022b69df00e731feb80af8c5d8ae)](https://sonarcube.samsaraserver.space/dashboard?id=Samsara-server)
[![Vulnerabilities](https://sonarcube.samsaraserver.space/api/project_badges/measure?project=Samsara-server&metric=vulnerabilities&token=sqb_f1c814e55b2b022b69df00e731feb80af8c5d8ae)](https://sonarcube.samsaraserver.space/dashboard?id=Samsara-server)

Repurpose your old Android devices into powerful Linux servers with a sleek interface and zero command-line complexity.

<img width="217" height="205" alt="Logo-detailed-no-background_1" src="https://github.com/user-attachments/assets/67748b4d-0057-4dd0-b02e-7b86ce544dd0" /> 

## About

Samsara Server aims to breathe new life into unused Android phones and tablets by converting them into fully functional Linux servers. Built on the robust foundation of Termux, this application provides an intuitive wrapper interface that makes server management accessible to everyone, from beginners to system administrators.

The project combines the power of Alpine Linux with Android's hardware capabilities, creating an ecosystem where community-driven projects can flourish on repurposed devices. Whether you're running a home automation system, hosting a personal cloud, or experimenting with distributed computing, Samsara Server makes it simple.

**Project Website:** [samsaraserver.space](https://samsaraserver.space)

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
Comprehensive guides and tutorials are available through the in-app documentation viewer, covering everything from basic setup to advanced server configurations. Complete documentation is also available at [wiki.samsaraserver.space](https://wiki.samsaraserver.space).

**Forums**
Join our community forums at [forums.samsaraserver.space](https://forums.samsaraserver.space) to share projects, ask questions, and collaborate with other users. The same account system works across the mobile app and web platform.

**Contributing**
We welcome contributions from developers, documentation writers, and community members. Whether you're fixing bugs, adding features, or helping other users, every contribution makes the project better.

## Building from Source

**Prerequisites**
- JDK 17.0.16.8-hotspot (compile)
- JDK 11 (Gradle)
- Android NDK 22.1.7171670
- Gradle build system

**Build Process**
```bash
# Quick development build
./quick-build.bat

# Full production build
./build.bat
```

## Acknowledgments

Beyond the Termux ecosystem, we're grateful to the broader open-source community that makes projects like this possible. From the Linux kernel developers to the Android Open Source Project, from package maintainers to documentation writers - thank you for creating the foundation that allows us to build something meaningful.

The vision of turning discarded devices into useful servers wouldn't be possible without the collective effort of thousands of developers who believe in accessible, sustainable technology.

---
