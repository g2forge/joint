$root = (Get-Item $PSScriptRoot).Parent.Parent.FullName
powershell "Get-FileHash -Algorithm SHA1 $root\package.json | Select -expand Hash" > "$root/node/init"
