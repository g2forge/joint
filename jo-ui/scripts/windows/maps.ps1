$root = (Get-Item $PSScriptRoot).Parent.Parent.FullName
$maps = "$root\dist\maps"

New-Item -Path "$maps" -ItemType Directory -Force
Move-Item -Path "$args[0]\*.map" -Destination "$maps"
