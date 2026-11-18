param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArguments = @('test'))
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $backendRoot -Parent
$taskJavaCandidates = @($env:JAVA_HOME)
$localJdkRoot = Join-Path $projectRoot 'outputs/tools/jdk21'
if (Test-Path -LiteralPath $localJdkRoot) {
    $taskJavaCandidates += (Get-ChildItem -LiteralPath $localJdkRoot -Directory).FullName
}
$selectedJdk = $null
foreach ($candidate in $taskJavaCandidates) {
    if (!$candidate) { continue }
    $javaExecutable = Join-Path $candidate 'bin/java.exe'
    if (!(Test-Path -LiteralPath $javaExecutable)) { continue }
    $versionText = (& $javaExecutable -version 2>&1 | Out-String)
    if ($versionText -match 'version "21\.') { $selectedJdk = $candidate; break }
}
if (!$selectedJdk) { throw '请安装 JDK 21 并设置 JAVA_HOME。此脚本不会更改系统级 Java 配置。' }
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
try {
    $env:JAVA_HOME = $selectedJdk
    $env:PATH = "$selectedJdk\bin;$previousPath"
    & mvn -f (Join-Path $backendRoot 'pom.xml') @MavenArguments
    $mavenExitCode = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
}
exit $mavenExitCode
