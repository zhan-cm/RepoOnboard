@echo off
setlocal

set "REPOONBOARD_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "REPOONBOARD_JAVA=%JAVA_HOME%\bin\java.exe"

"%REPOONBOARD_JAVA%" -version >nul 2>&1
if errorlevel 1 (
  echo RepoOnboard requires Java 21 or newer. Install a JDK and set JAVA_HOME or add java to PATH. 1>&2
  exit /b 1
)

set "REPOONBOARD_JAVA_VERSION="
for /f "tokens=3" %%V in ('call "%REPOONBOARD_JAVA%" -version 2^>^&1 ^| findstr /I "version"') do if not defined REPOONBOARD_JAVA_VERSION set "REPOONBOARD_JAVA_VERSION=%%~V"
if not defined REPOONBOARD_JAVA_VERSION (
  echo RepoOnboard could not determine the Java version. Java 21 or newer is required. 1>&2
  exit /b 1
)
for /f "tokens=1 delims=." %%V in ("%REPOONBOARD_JAVA_VERSION%") do set "REPOONBOARD_JAVA_MAJOR=%%V"
for /f "delims=0123456789" %%V in ("%REPOONBOARD_JAVA_MAJOR%") do (
  echo RepoOnboard could not determine the Java version. Java 21 or newer is required. 1>&2
  exit /b 1
)
if %REPOONBOARD_JAVA_MAJOR% LSS 21 (
  echo RepoOnboard requires Java 21 or newer; found Java %REPOONBOARD_JAVA_VERSION%. 1>&2
  exit /b 1
)

if defined REPOONBOARD_JAR (
  set "REPOONBOARD_JAR_PATH=%REPOONBOARD_JAR%"
) else if exist "%~dp0repoonboard.jar" (
  set "REPOONBOARD_JAR_PATH=%~dp0repoonboard.jar"
) else if exist "%~dp0target\repoonboard.jar" (
  set "REPOONBOARD_JAR_PATH=%~dp0target\repoonboard.jar"
) else (
  echo RepoOnboard JAR not found. Keep repoonboard.jar beside this script or run .\mvnw.cmd clean verify. 1>&2
  exit /b 1
)

"%REPOONBOARD_JAVA%" -jar "%REPOONBOARD_JAR_PATH%" %*
exit /b %ERRORLEVEL%
