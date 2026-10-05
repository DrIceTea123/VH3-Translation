@echo off
setlocal
for /f "tokens=2 delims=:" %%C in ('chcp') do set "build_codepage=%%C"
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
set "build_exit=%errorlevel%"
if not "%build_exit%"=="0" (echo Build failed.)
pause
if defined build_codepage chcp %build_codepage% >nul
exit /b %build_exit%
