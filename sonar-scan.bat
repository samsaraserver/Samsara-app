@echo off
REM Run SonarQube analysis via Gradle using provided properties

setlocal enabledelayedexpansion

REM #COMPLETION_DRIVE: Use user-provided Sonar params directly for convenience.
REM #SUGGEST_VERIFY: Prefer setting SONAR_HOST_URL and SONAR_TOKEN as env vars in CI to avoid committing secrets.

set SONAR_PROJECT_KEY=Samsara-server
set SONAR_PROJECT_NAME=Samsara server
set SONAR_HOST_URL=https://sonarcube.samsaraserver.space
set SONAR_TOKEN=sqp_cf3afe29d1bf5dc0dd56f6f744d8f53be9f32742

call .\gradlew.bat sonar ^
  -Dsonar.projectKey=!SONAR_PROJECT_KEY! ^
  -Dsonar.projectName="!SONAR_PROJECT_NAME!" ^
  -Dsonar.host.url=!SONAR_HOST_URL! ^
  -Dsonar.token=!SONAR_TOKEN!

endlocal
