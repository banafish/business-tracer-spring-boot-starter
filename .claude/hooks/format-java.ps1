# PostToolUse hook：Claude 编辑/写入 .java 文件后，只格式化该文件（约 1 秒）。
# 任何前置条件不满足都静默退出（exit 0），绝不阻塞编辑；最终一致性由 mvn verify 的 spotless:check 兜底。
$ErrorActionPreference = 'SilentlyContinue'
$payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
$file = $payload.tool_input.file_path
if (-not $file) { exit 0 }
if (-not $file.EndsWith('.java')) { exit 0 }
if (-not (Test-Path $file)) { exit 0 }
$toolDir = Join-Path $PSScriptRoot '..\tools\pjf'
if (-not (Test-Path $toolDir)) { exit 0 }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) { exit 0 }
& $java `
  --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED `
  --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED `
  -cp "$toolDir\*" com.palantir.javaformat.java.Main --palantir --replace $file
exit 0
