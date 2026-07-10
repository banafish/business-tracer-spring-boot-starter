# 把 palantir-java-format 及其依赖复制到 .claude/tools/pjf（gitignored）。
# 只需在 clone 后执行一次；未执行时 format-java.ps1 会静默跳过。
$ErrorActionPreference = 'Stop'
$toolDir = Join-Path $PSScriptRoot '..\tools\pjf'
New-Item -ItemType Directory -Force $toolDir | Out-Null
$stubPom = Join-Path $env:TEMP 'pjf-stub-pom.xml'
@'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>tmp</groupId>
  <artifactId>pjf-stub</artifactId>
  <version>1</version>
  <dependencies>
    <dependency>
      <groupId>com.palantir.javaformat</groupId>
      <artifactId>palantir-java-format</artifactId>
      <version>2.50.0</version>
    </dependency>
  </dependencies>
</project>
'@ | Out-File -Encoding utf8 $stubPom
mvn -q -f $stubPom dependency:copy-dependencies "-DoutputDirectory=$toolDir"
Write-Host "formatter jars ready in $toolDir"
