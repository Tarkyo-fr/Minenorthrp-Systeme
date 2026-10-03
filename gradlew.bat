@echo off
setlocal
set "GRADLE_VERSION=8.8"
set "GRADLE_HOME=%~dp0.gradle-bootstrap\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"
if exist "%GRADLE_BIN%" goto run

echo.
echo [MineNorth] Gradle %GRADLE_VERSION% n'est pas encore installe.
echo Telechargement automatique de Gradle... 
if not exist "%~dp0.gradle-bootstrap" mkdir "%~dp0.gradle-bootstrap"
set "ZIP=%~dp0.gradle-bootstrap\gradle-%GRADLE_VERSION%-bin.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.8-bin.zip' -OutFile '%ZIP%'"
if errorlevel 1 (
  echo.
  echo Echec du telechargement de Gradle. Verifiez votre connexion Internet.
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%~dp0.gradle-bootstrap' -Force"
if errorlevel 1 exit /b 1

del /q "%ZIP%" >nul 2>&1
if not exist "%GRADLE_BIN%" (
  echo Impossible de trouver Gradle apres extraction.
  exit /b 1
)

:run
call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
