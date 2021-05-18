SET "ROOT=%~dp0..\..\"
SET "MAPS=%ROOT%dist\maps"

if not exist "%MAPS%" mkdir "%MAPS%"
MOVE "%1\*.map" "%MAPS%"
