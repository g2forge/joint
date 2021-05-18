SET "ROOT=%~dp0..\..\"

powershell "Get-FileHash -Algorithm SHA1 %ROOT%\package.json | Select -expand Hash" > "%ROOT%/node/init"
