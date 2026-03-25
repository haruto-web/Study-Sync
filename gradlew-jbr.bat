@echo off
setlocal enabledelayedexpansion

REM Prefer explicit overrides
if not "%ANDROID_STUDIO_JBR%"=="" (
  if exist "%ANDROID_STUDIO_JBR%\bin\java.exe" (
    set "JBR=%ANDROID_STUDIO_JBR%"
    goto :found
  )
)
if not "%ANDROID_STUDIO_HOME%"=="" (
  if exist "%ANDROID_STUDIO_HOME%\jbr\bin\java.exe" (
    set "JBR=%ANDROID_STUDIO_HOME%\jbr"
    goto :found
  )
)

REM Common install paths
set "CAND1=C:\Program Files\Android\Android Studio\jbr"
set "CAND2=C:\Program Files (x86)\Android\Android Studio\jbr"
set "CAND3=%LOCALAPPDATA%\Programs\Android Studio\jbr"
set "CAND4=%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr"

for %%P in ("%CAND1%" "%CAND2%" "%CAND3%" "%CAND4%") do (
  if exist "%%~P\bin\java.exe" (
    set "JBR=%%~P"
    goto :found
  )
)

echo Could not find Android Studio embedded JBR.
echo Set ANDROID_STUDIO_JBR to your Android Studio jbr folder, e.g.
echo   setx ANDROID_STUDIO_JBR "C:\Program Files\Android\Android Studio\jbr"
exit /b 1

:found
echo Using embedded JBR: %JBR%
set "JAVA_HOME=%JBR%"
set "PATH=%JBR%\bin;%PATH%"

call .\gradlew.bat %*
exit /b %ERRORLEVEL%
