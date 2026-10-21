# Builds the decompiled OptiFine 1.8.9 HD U M6 pre1 sources.
#   build/OptiFine-named.jar  - MCP (stable_22) names, for reading/porting/debugging
#   build/OptiFine-obf.jar    - reobfuscated to vanilla (notch) names, same layout as the patched original
# Requires a JDK 8. Set $env:JAVA8_HOME to override the default location.

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jdk = if ($env:JAVA8_HOME) { $env:JAVA8_HOME } else { "$env:USERPROFILE\.gradle\jdks\temurin-8-amd64-windows.2" }

$build = Join-Path $root 'build'
$classes = Join-Path $build 'classes'
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force $classes | Out-Null

$libs = Get-ChildItem (Join-Path $root 'libs') -Filter *.jar | ForEach-Object FullName
$cp = $libs -join ';'

$srcList = Join-Path $build 'sources.txt'
Get-ChildItem (Join-Path $root 'src\main\java') -Recurse -Filter *.java |
    ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' } |
    Set-Content -Encoding ASCII $srcList

Write-Host 'Compiling...'
& "$jdk\bin\javac.exe" -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -cp $cp -d $classes "@$srcList"
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Copy-Item -Recurse -Force (Join-Path $root 'src\main\resources\*') $classes

$named = Join-Path $build 'OptiFine-named.jar'
& "$jdk\bin\jar.exe" cfm $named (Join-Path $classes 'META-INF\MANIFEST.MF') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

Write-Host 'Reobfuscating...'
$tools = (Get-ChildItem (Join-Path $root 'tools') -Filter *.jar | ForEach-Object FullName) -join ';'
$obf = Join-Path $build 'OptiFine-obf.jar'
& "$jdk\bin\java.exe" -cp $tools net.fabricmc.tinyremapper.Main $named $obf (Join-Path $root 'tools\mappings-1.8.9-mcp.tiny') named official @libs
if ($LASTEXITCODE -ne 0) { throw 'tiny-remapper failed' }

Write-Host "Done: $named"
Write-Host "      $obf"
