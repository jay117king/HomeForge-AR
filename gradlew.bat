@echo off
REM Minimal gradlew.bat helper for Windows runners
SET GRADLE_VERSION=8.6
SET INSTALL_DIR=%USERPROFILE%\.gradle-wrapper
SET GRADLE_DIR=%INSTALL_DIR%\gradle-%GRADLE_VERSION%
IF NOT EXIST "%GRADLE_DIR%" (
  echo Gradle %GRADLE_VERSION% not found, downloading...
  powershell -Command "Invoke-WebRequest -Uri https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip -OutFile '%TEMP%\gradle.zip'"
  powershell -Command "Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%INSTALL_DIR%'"
)
SET PATH=%GRADLE_DIR%\bin;%PATH%
gradle %*
