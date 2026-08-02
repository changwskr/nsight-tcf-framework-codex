param([ValidateSet('Contracts','Skills','Simulation','Verifier','All')][string]$Mode='All')
$ErrorActionPreference='Stop'
$script:Failures=[System.Collections.Generic.List[string]]::new()
$Root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
function Assert-True { param([bool]$Condition,[string]$Message) if(-not $Condition){$script:Failures.Add($Message)} }
function Assert-FileContains { param([string]$Path,[string[]]$Patterns) if(-not(Test-Path -LiteralPath $Path -PathType Leaf)){$script:Failures.Add("Missing file: $Path");return};$content=Get-Content -Raw -Encoding UTF8 -LiteralPath $Path;foreach($pattern in $Patterns){if($content -notmatch [regex]::Escape($pattern)){$script:Failures.Add("Missing '$pattern' in $Path")}} }
function Test-Contracts {
  foreach($relative in @('AGENTS.md','README.md','docs/quickstart.md','docs/migration-from-claude.md','agents/analyst.md','agents/builder.md','agents/qa.md')){Assert-True (Test-Path -LiteralPath (Join-Path $Root $relative) -PathType Leaf) "Missing file: $relative"}
  foreach($role in @('analyst','builder','qa')){$path=Join-Path $Root "agents/$role.md";if(Test-Path $path){$content=Get-Content -Raw -Encoding UTF8 $path;$count=([regex]::Matches($content,'(?m)^## [^#\r\n]+$')).Count;Assert-True ($count -eq 6) "Expected 6 role sections in $path, found $count"}}
  Assert-FileContains (Join-Path $Root 'README.md') @('Codex','AGENTS.md','SKILL.md','verify-codex-harness.ps1')
}
function Test-Skills {
  Assert-FileContains (Join-Path $Root 'skills/harness/SKILL.md') @('---','name: harness','description:','spawn_agent','send_message','followup_task','wait_agent')
  Assert-FileContains (Join-Path $Root 'skills/harness/orchestrator-sample/SKILL.md') @('---','name:','description:','Analyst','Builder','QA','tcf-harness-world')
  foreach($relative in @('skills/harness/references/agent-design-patterns.md','skills/harness/references/orchestrator-template.md','skills/harness/references/codex-tool-mapping.md')){Assert-True (Test-Path -LiteralPath (Join-Path $Root $relative) -PathType Leaf) "Missing file: $relative"}
}
switch($Mode){'Contracts'{Test-Contracts};'Skills'{Test-Skills};'All'{Test-Contracts;Test-Skills};default{$script:Failures.Add("Test mode is not implemented yet: $Mode")}}
if($script:Failures.Count -gt 0){$script:Failures|ForEach-Object{Write-Error $_ -ErrorAction Continue};exit 1}
Write-Host "$Mode checks passed."
exit 0
