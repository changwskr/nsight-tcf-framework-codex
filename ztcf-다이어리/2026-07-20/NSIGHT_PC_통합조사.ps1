<#
.SYNOPSIS
    NSIGHT 현업 단말 PC 통합 조사 스크립트

.DESCRIPTION
    한 번의 실행으로 다음 정보를 수집합니다.
    - PC 제조사/모델/시리얼/BIOS/CPU/Memory/Disk/GPU/Monitor
    - Windows 버전/빌드/설치일/최근 부팅/TPM/Secure Boot/BitLocker
    - 설치 소프트웨어(레지스트리 기반, Win32_Product 미사용)
    - Microsoft Store 앱, 브라우저/런타임/업무·보안 솔루션 후보
    - 자동 시작 프로그램, 서비스, 비-Microsoft 예약 작업
    - 네트워크 어댑터/IP/DNS/Gateway/Listening Port
    - 현재 프로세스 및 일정 시간 CPU/Memory/Disk 성능 표본
    - Windows Hotfix 및 선택 기능
    - 통합 요약 CSV/HTML과 전체 결과 ZIP

.NOTES
    - Windows PowerShell 5.1 이상 권장
    - 관리자 권한으로 실행하면 TPM, BitLocker, 전체 사용자 앱 등 더 많은 정보가 수집됩니다.
    - MSI 재구성을 유발할 수 있는 Win32_Product는 사용하지 않습니다.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$OutputRoot = "$env:USERPROFILE\Desktop\NSIGHT_PC_조사결과",

    [Parameter(Mandatory = $false)]
    [ValidateRange(10, 600)]
    [int]$SampleSeconds = 30,

    [Parameter(Mandatory = $false)]
    [ValidateRange(1, 60)]
    [int]$SampleIntervalSeconds = 5
)

$ErrorActionPreference = "Continue"

# ---------------------------------------------------------------------------
# 공통 함수
# ---------------------------------------------------------------------------
function Write-Step {
    param([string]$Message)
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "HH:mm:ss"), $Message) -ForegroundColor Cyan
}

function Export-SafeCsv {
    param(
        [Parameter(Mandatory = $true)]$InputObject,
        [Parameter(Mandatory = $true)][string]$Path
    )
    try {
        @($InputObject) | Export-Csv -Path $Path -NoTypeInformation -Encoding UTF8
    }
    catch {
        Write-Warning "CSV 저장 실패: $Path / $($_.Exception.Message)"
    }
}

function Convert-ToGB {
    param($Bytes)
    if ($null -eq $Bytes -or $Bytes -eq "") { return $null }
    return [math]::Round(([double]$Bytes / 1GB), 2)
}

function Convert-WmiDate {
    param($Value)
    if ($null -eq $Value -or [string]::IsNullOrWhiteSpace([string]$Value)) { return $null }
    try {
        if ($Value -is [datetime]) { return $Value }
        return [Management.ManagementDateTimeConverter]::ToDateTime([string]$Value)
    }
    catch {
        try { return [datetime]$Value } catch { return $null }
    }
}

function Convert-MonitorText {
    param($Array)
    if ($null -eq $Array) { return "" }
    try {
        return (($Array | Where-Object { $_ -ne 0 } | ForEach-Object { [char]$_ }) -join "")
    }
    catch { return "" }
}

function Get-SafeTrimmedText {
    param($Value)
    if ($null -eq $Value) { return "" }
    return ([string]$Value).Trim()
}

function Get-InstalledSoftwareFromRegistry {
    $locations = @(
        @{ Path = "HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*"; Scope = "Computer"; Architecture = "64-bit/Native" },
        @{ Path = "HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*"; Scope = "Computer"; Architecture = "32-bit" },
        @{ Path = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*"; Scope = "CurrentUser"; Architecture = "User" }
    )

    $results = foreach ($location in $locations) {
        Get-ItemProperty -Path $location.Path -ErrorAction SilentlyContinue |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_.DisplayName) } |
            ForEach-Object {
                [PSCustomObject]@{
                    프로그램명       = $_.DisplayName
                    버전             = $_.DisplayVersion
                    제조사           = $_.Publisher
                    설치일           = $_.InstallDate
                    예상용량_MB      = if ($_.EstimatedSize) { [math]::Round($_.EstimatedSize / 1024, 2) } else { $null }
                    설치위치         = $_.InstallLocation
                    제거명령         = $_.UninstallString
                    설치범위         = $location.Scope
                    아키텍처         = $location.Architecture
                    RegistryKey      = $_.PSChildName
                }
            }
    }

    $results |
        Sort-Object 프로그램명, 버전, 설치범위 -Unique
}

function Get-ComponentCategory {
    param(
        [string]$Name,
        [string]$Publisher
    )

    $text = ("{0} {1}" -f $Name, $Publisher)

    if ($text -match "(?i)WEBTOP|WebSquare|Inswave|인스웨이브") { return "WEBTOPSUITE/WebSquare" }
    if ($text -match "(?i)WebView2") { return "WebView2" }
    if ($text -match "(?i)Microsoft Edge") { return "Edge" }
    if ($text -match "(?i)Google Chrome") { return "Chrome" }
    if ($text -match "(?i)SSO|Single Sign|통합인증|인증서|전자서명|AnySign|INISAFE|TouchEn|MagicLine|CrossCert|Veraport|nProtect") { return "인증/전자서명" }
    if ($text -match "(?i)EDR|Endpoint|Defender|V3|AhnLab|McAfee|Symantec|Trend Micro|Sentinel|CrowdStrike|백신|DLP|DRM|Fasoo|SoftCamp|MarkAny|NAC|매체통제|개인정보") { return "보안솔루션" }
    if ($text -match "(?i)Rexpert|OZ Report|UbiReport|Report Viewer|Print Viewer|레포트|리포트") { return "보고서/출력" }
    if ($text -match "(?i)Hancom|한글|Microsoft 365|Microsoft Office|Adobe Acrobat|PDF") { return "문서/PDF" }
    if ($text -match "(?i)Java|JRE|JDK") { return "Java" }
    if ($text -match "(?i)\.NET|ASP\.NET|Windows Desktop Runtime") { return ".NET" }
    if ($text -match "(?i)Visual C\+\+|VC\+\+") { return "Visual C++ Runtime" }
    if ($text -match "(?i)Git|Visual Studio Code|Eclipse|IntelliJ|Node\.js|Docker|WSL|DBeaver|SQL Developer") { return "개발/금지후보" }

    return $null
}

function Get-FileVersionObject {
    param(
        [string]$Name,
        [string]$Path
    )
    if (-not (Test-Path $Path)) { return $null }
    try {
        $item = Get-Item $Path -ErrorAction Stop
        return [PSCustomObject]@{
            구성요소 = $Name
            경로     = $Path
            파일버전 = $item.VersionInfo.FileVersion
            제품버전 = $item.VersionInfo.ProductVersion
            수정일   = $item.LastWriteTime
        }
    }
    catch { return $null }
}

function Test-IsAdministrator {
    try {
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
        $principal = New-Object Security.Principal.WindowsPrincipal($identity)
        return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    }
    catch { return $false }
}

# ---------------------------------------------------------------------------
# 초기화
# ---------------------------------------------------------------------------
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$computerName = $env:COMPUTERNAME
$outputDirectory = Join-Path $OutputRoot ("{0}_{1}" -f $computerName, $timestamp)
$zipPath = "$outputDirectory.zip"

New-Item -Path $outputDirectory -ItemType Directory -Force | Out-Null
$logPath = Join-Path $outputDirectory "99_실행로그.txt"

try {
    Start-Transcript -Path $logPath -Force | Out-Null
}
catch {
    Write-Warning "Transcript를 시작하지 못했습니다: $($_.Exception.Message)"
}

$isAdministrator = Test-IsAdministrator
Write-Step "NSIGHT 단말 PC 통합 조사를 시작합니다."
Write-Host "출력 경로: $outputDirectory" -ForegroundColor Yellow
Write-Host "관리자 권한: $isAdministrator" -ForegroundColor Yellow

# 결과 변수를 사전 초기화
$installedSoftware = @()
$targetComponents = @()
$performanceSamples = @()
$performanceSummary = @()
$systemSummary = @()

try {
    # -----------------------------------------------------------------------
    # 1. 하드웨어 / OS
    # -----------------------------------------------------------------------
    Write-Step "하드웨어 및 운영체제 정보를 수집합니다."

    $computer = Get-CimInstance Win32_ComputerSystem -ErrorAction SilentlyContinue
    $bios = Get-CimInstance Win32_BIOS -ErrorAction SilentlyContinue
    $baseBoard = Get-CimInstance Win32_BaseBoard -ErrorAction SilentlyContinue
    $os = Get-CimInstance Win32_OperatingSystem -ErrorAction SilentlyContinue
    $cpus = @(Get-CimInstance Win32_Processor -ErrorAction SilentlyContinue)
    $memoryModules = @(Get-CimInstance Win32_PhysicalMemory -ErrorAction SilentlyContinue)
    $logicalDisks = @(Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" -ErrorAction SilentlyContinue)
    $diskDrives = @(Get-CimInstance Win32_DiskDrive -ErrorAction SilentlyContinue)
    $gpus = @(Get-CimInstance Win32_VideoController -ErrorAction SilentlyContinue)

    $cpuInfo = $cpus | ForEach-Object {
        [PSCustomObject]@{
            CPU명          = Get-SafeTrimmedText $_.Name
            제조사         = $_.Manufacturer
            Socket         = $_.SocketDesignation
            물리Core       = $_.NumberOfCores
            논리Processor  = $_.NumberOfLogicalProcessors
            최대클럭_MHz   = $_.MaxClockSpeed
            현재클럭_MHz   = $_.CurrentClockSpeed
            가상화Firmware = $_.VirtualizationFirmwareEnabled
        }
    }
    Export-SafeCsv $cpuInfo (Join-Path $outputDirectory "02_CPU.csv")

    $memoryInfo = $memoryModules | ForEach-Object {
        [PSCustomObject]@{
            제조사       = $_.Manufacturer
            PartNumber   = Get-SafeTrimmedText $_.PartNumber
            용량_GB      = Convert-ToGB $_.Capacity
            속도_MHz     = $_.Speed
            설정속도_MHz = $_.ConfiguredClockSpeed
            Bank         = $_.BankLabel
            Device       = $_.DeviceLocator
            SerialNumber = $_.SerialNumber
        }
    }
    Export-SafeCsv $memoryInfo (Join-Path $outputDirectory "03_Memory.csv")

    $logicalDiskInfo = $logicalDisks | ForEach-Object {
        $sizeGB = Convert-ToGB $_.Size
        $freeGB = Convert-ToGB $_.FreeSpace
        $usedPercent = $null
        if ($_.Size -gt 0) {
            $usedPercent = [math]::Round((($_.Size - $_.FreeSpace) / $_.Size) * 100, 1)
        }
        [PSCustomObject]@{
            드라이브       = $_.DeviceID
            볼륨명         = $_.VolumeName
            파일시스템     = $_.FileSystem
            전체용량_GB    = $sizeGB
            여유공간_GB    = $freeGB
            사용률_Percent = $usedPercent
        }
    }
    Export-SafeCsv $logicalDiskInfo (Join-Path $outputDirectory "04_LogicalDisk.csv")

    $diskDriveInfo = $diskDrives | ForEach-Object {
        [PSCustomObject]@{
            모델         = $_.Model
            Interface    = $_.InterfaceType
            MediaType    = $_.MediaType
            Firmware     = $_.FirmwareRevision
            SerialNumber = Get-SafeTrimmedText $_.SerialNumber
            전체용량_GB  = Convert-ToGB $_.Size
            상태         = $_.Status
        }
    }
    Export-SafeCsv $diskDriveInfo (Join-Path $outputDirectory "05_PhysicalDisk.csv")

    $gpuInfo = $gpus | ForEach-Object {
        [PSCustomObject]@{
            GPU명          = $_.Name
            DriverVersion  = $_.DriverVersion
            DriverDate     = Convert-WmiDate $_.DriverDate
            비디오메모리_GB = Convert-ToGB $_.AdapterRAM
            해상도         = if ($_.CurrentHorizontalResolution -and $_.CurrentVerticalResolution) {
                                "{0}x{1}" -f $_.CurrentHorizontalResolution, $_.CurrentVerticalResolution
                            } else { "" }
        }
    }
    Export-SafeCsv $gpuInfo (Join-Path $outputDirectory "06_GPU.csv")

    $monitorInfo = @()
    try {
        $monitorInfo = Get-CimInstance -Namespace root\wmi -ClassName WmiMonitorID -ErrorAction Stop |
            ForEach-Object {
                [PSCustomObject]@{
                    제조사코드   = Convert-MonitorText $_.ManufacturerName
                    제품코드     = Convert-MonitorText $_.ProductCodeID
                    모니터명     = Convert-MonitorText $_.UserFriendlyName
                    SerialNumber = Convert-MonitorText $_.SerialNumberID
                    활성상태     = $_.Active
                    InstanceName = $_.InstanceName
                }
            }
    }
    catch {
        $monitorInfo = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $monitorInfo (Join-Path $outputDirectory "07_Monitor.csv")

    # TPM / Secure Boot / BitLocker
    $tpmInfo = @()
    try {
        if (Get-Command Get-Tpm -ErrorAction SilentlyContinue) {
            $tpm = Get-Tpm -ErrorAction Stop
            $tpmInfo = @([PSCustomObject]@{
                TpmPresent       = $tpm.TpmPresent
                TpmReady         = $tpm.TpmReady
                TpmEnabled       = $tpm.TpmEnabled
                TpmActivated     = $tpm.TpmActivated
                ManagedAuthLevel = $tpm.ManagedAuthLevel
                ManufacturerId   = $tpm.ManufacturerIdTxt
                ManufacturerVersion = $tpm.ManufacturerVersion
            })
        }
    }
    catch {
        $tpmInfo = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $tpmInfo (Join-Path $outputDirectory "08_TPM.csv")

    $secureBootValue = "확인불가"
    try {
        if (Get-Command Confirm-SecureBootUEFI -ErrorAction SilentlyContinue) {
            $secureBootValue = [string](Confirm-SecureBootUEFI -ErrorAction Stop)
        }
    }
    catch {
        $secureBootValue = "확인불가: $($_.Exception.Message)"
    }

    $bitLockerInfo = @()
    try {
        if (Get-Command Get-BitLockerVolume -ErrorAction SilentlyContinue) {
            $bitLockerInfo = Get-BitLockerVolume -ErrorAction Stop |
                ForEach-Object {
                    [PSCustomObject]@{
                        MountPoint       = $_.MountPoint
                        VolumeType       = $_.VolumeType
                        ProtectionStatus = $_.ProtectionStatus
                        EncryptionMethod = $_.EncryptionMethod
                        VolumeStatus     = $_.VolumeStatus
                        EncryptionPercent = $_.EncryptionPercentage
                        AutoUnlockEnabled = $_.AutoUnlockEnabled
                    }
                }
        }
    }
    catch {
        $bitLockerInfo = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $bitLockerInfo (Join-Path $outputDirectory "09_BitLocker.csv")

    # -----------------------------------------------------------------------
    # 2. 설치 소프트웨어 / 앱 / 핵심 구성요소
    # -----------------------------------------------------------------------
    Write-Step "설치 소프트웨어와 핵심 구성요소를 수집합니다."

    $installedSoftware = @(Get-InstalledSoftwareFromRegistry)
    Export-SafeCsv $installedSoftware (Join-Path $outputDirectory "10_InstalledSoftware.csv")

    $storeApps = @()
    try {
        if ($isAdministrator) {
            $storeApps = Get-AppxPackage -AllUsers -ErrorAction Stop
        }
        else {
            $storeApps = Get-AppxPackage -ErrorAction Stop
        }

        $storeApps = $storeApps | ForEach-Object {
            [PSCustomObject]@{
                Name            = $_.Name
                PackageFullName = $_.PackageFullName
                Version         = $_.Version
                Publisher       = $_.Publisher
                InstallLocation = $_.InstallLocation
                Architecture    = $_.Architecture
            }
        }
    }
    catch {
        $storeApps = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $storeApps (Join-Path $outputDirectory "11_StoreApps.csv")

    $targetComponents = $installedSoftware |
        ForEach-Object {
            $category = Get-ComponentCategory -Name $_.프로그램명 -Publisher $_.제조사
            if ($category) {
                [PSCustomObject]@{
                    분류       = $category
                    프로그램명 = $_.프로그램명
                    버전       = $_.버전
                    제조사     = $_.제조사
                    설치일     = $_.설치일
                    설치위치   = $_.설치위치
                    아키텍처   = $_.아키텍처
                    설치범위   = $_.설치범위
                }
            }
        } |
        Sort-Object 분류, 프로그램명, 버전 -Unique

    Export-SafeCsv $targetComponents (Join-Path $outputDirectory "12_NSIGHT_핵심솔루션후보.csv")

    # 주요 브라우저 실행파일 버전
    $browserPaths = @(
        @{ Name = "Microsoft Edge"; Path = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe" },
        @{ Name = "Microsoft Edge"; Path = "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe" },
        @{ Name = "Google Chrome"; Path = "$env:ProgramFiles\Google\Chrome\Application\chrome.exe" },
        @{ Name = "Google Chrome"; Path = "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe" },
        @{ Name = "WebView2"; Path = "${env:ProgramFiles(x86)}\Microsoft\EdgeWebView\Application\msedgewebview2.exe" }
    )

    $browserVersions = @()
    foreach ($entry in $browserPaths) {
        $result = Get-FileVersionObject -Name $entry.Name -Path $entry.Path
        if ($null -ne $result) { $browserVersions += $result }
    }
    Export-SafeCsv ($browserVersions | Sort-Object 구성요소, 제품버전 -Unique) (Join-Path $outputDirectory "13_Browser_Runtime_Version.csv")

    # Windows Hotfix
    $hotfixes = @()
    try {
        $hotfixes = Get-HotFix -ErrorAction Stop |
            Sort-Object InstalledOn -Descending |
            Select-Object HotFixID, Description, InstalledBy, InstalledOn
    }
    catch {
        $hotfixes = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $hotfixes (Join-Path $outputDirectory "14_WindowsHotfix.csv")

    # Windows 선택 기능
    $windowsFeatures = @()
    try {
        if (Get-Command Get-WindowsOptionalFeature -ErrorAction SilentlyContinue) {
            $windowsFeatures = Get-WindowsOptionalFeature -Online -ErrorAction Stop |
                Select-Object FeatureName, State |
                Sort-Object State, FeatureName
        }
    }
    catch {
        $windowsFeatures = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $windowsFeatures (Join-Path $outputDirectory "15_WindowsOptionalFeature.csv")

    # -----------------------------------------------------------------------
    # 3. 자동실행 / 서비스 / 예약작업 / 현재 프로세스
    # -----------------------------------------------------------------------
    Write-Step "자동실행, 서비스, 예약 작업, 프로세스를 수집합니다."

    $startupItems = @()
    try {
        $startupItems = Get-CimInstance Win32_StartupCommand -ErrorAction Stop |
            Select-Object Name, Command, Location, User
    }
    catch {
        $startupItems = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $startupItems (Join-Path $outputDirectory "20_StartupItems.csv")

    $services = @()
    try {
        $services = Get-CimInstance Win32_Service -ErrorAction Stop |
            Select-Object Name, DisplayName, State, StartMode, StartName, ProcessId, PathName |
            Sort-Object State, DisplayName
    }
    catch {
        $services = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $services (Join-Path $outputDirectory "21_Services.csv")

    $scheduledTasks = @()
    try {
        if (Get-Command Get-ScheduledTask -ErrorAction SilentlyContinue) {
            $scheduledTasks = Get-ScheduledTask -ErrorAction Stop |
                Where-Object { $_.TaskPath -notlike "\Microsoft\*" } |
                ForEach-Object {
                    [PSCustomObject]@{
                        TaskName    = $_.TaskName
                        TaskPath    = $_.TaskPath
                        State       = $_.State
                        Author      = $_.Author
                        Description = $_.Description
                    }
                }
        }
    }
    catch {
        $scheduledTasks = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $scheduledTasks (Join-Path $outputDirectory "22_NonMicrosoft_ScheduledTasks.csv")

    $processes = @()
    try {
        $processes = Get-Process -ErrorAction SilentlyContinue |
            ForEach-Object {
                $path = ""
                $startTime = $null
                try { $path = $_.Path } catch { $path = "" }
                try { $startTime = $_.StartTime } catch { $startTime = $null }
                [PSCustomObject]@{
                    ProcessName = $_.ProcessName
                    Id          = $_.Id
                    CPUSeconds  = if ($null -ne $_.CPU) { [math]::Round($_.CPU, 2) } else { $null }
                    WorkingSetMB = [math]::Round($_.WorkingSet64 / 1MB, 2)
                    PrivateMB    = [math]::Round($_.PrivateMemorySize64 / 1MB, 2)
                    Handles      = $_.Handles
                    Threads      = $_.Threads.Count
                    StartTime    = $startTime
                    Path         = $path
                }
            } |
            Sort-Object WorkingSetMB -Descending
    }
    catch {
        $processes = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $processes (Join-Path $outputDirectory "23_CurrentProcesses.csv")

    # -----------------------------------------------------------------------
    # 4. 네트워크
    # -----------------------------------------------------------------------
    Write-Step "네트워크 설정과 Listening Port를 수집합니다."

    $networkAdapters = @()
    try {
        if (Get-Command Get-NetAdapter -ErrorAction SilentlyContinue) {
            $networkAdapters = Get-NetAdapter -ErrorAction Stop |
                Select-Object Name, InterfaceDescription, Status, MacAddress, LinkSpeed, MediaType, DriverInformation
        }
        else {
            $networkAdapters = Get-CimInstance Win32_NetworkAdapter -ErrorAction Stop |
                Where-Object { $_.PhysicalAdapter } |
                Select-Object Name, Description, NetEnabled, MACAddress, Speed
        }
    }
    catch {
        $networkAdapters = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $networkAdapters (Join-Path $outputDirectory "30_NetworkAdapters.csv")

    $ipConfigurations = @()
    try {
        if (Get-Command Get-NetIPConfiguration -ErrorAction SilentlyContinue) {
            $ipConfigurations = Get-NetIPConfiguration -ErrorAction Stop |
                ForEach-Object {
                    [PSCustomObject]@{
                        InterfaceAlias = $_.InterfaceAlias
                        InterfaceIndex = $_.InterfaceIndex
                        IPv4Address    = ($_.IPv4Address.IPAddress -join ", ")
                        IPv6Address    = ($_.IPv6Address.IPAddress -join ", ")
                        IPv4Gateway    = ($_.IPv4DefaultGateway.NextHop -join ", ")
                        DNSServer      = ($_.DNSServer.ServerAddresses -join ", ")
                        NetProfile     = $_.NetProfile.Name
                    }
                }
        }
        else {
            $ipConfigurations = Get-CimInstance Win32_NetworkAdapterConfiguration -Filter "IPEnabled=True" |
                ForEach-Object {
                    [PSCustomObject]@{
                        Description = $_.Description
                        IPAddress   = ($_.IPAddress -join ", ")
                        Gateway     = ($_.DefaultIPGateway -join ", ")
                        DNSServer   = ($_.DNSServerSearchOrder -join ", ")
                        DHCPEnabled = $_.DHCPEnabled
                    }
                }
        }
    }
    catch {
        $ipConfigurations = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $ipConfigurations (Join-Path $outputDirectory "31_IPConfiguration.csv")

    $listeningPorts = @()
    try {
        if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
            $listeningPorts = Get-NetTCPConnection -State Listen -ErrorAction Stop |
                ForEach-Object {
                    $processName = ""
                    try { $processName = (Get-Process -Id $_.OwningProcess -ErrorAction Stop).ProcessName } catch {}
                    [PSCustomObject]@{
                        LocalAddress  = $_.LocalAddress
                        LocalPort     = $_.LocalPort
                        OwningProcess = $_.OwningProcess
                        ProcessName   = $processName
                        State         = $_.State
                    }
                } |
                Sort-Object LocalPort, ProcessName -Unique
        }
        else {
            $listeningPorts = netstat -ano -p tcp |
                Select-String "LISTENING" |
                ForEach-Object { [PSCustomObject]@{ Raw = $_.Line.Trim() } }
        }
    }
    catch {
        $listeningPorts = @([PSCustomObject]@{ 오류 = $_.Exception.Message })
    }
    Export-SafeCsv $listeningPorts (Join-Path $outputDirectory "32_ListeningPorts.csv")

    # -----------------------------------------------------------------------
    # 5. 성능 표본
    # -----------------------------------------------------------------------
    Write-Step "CPU/Memory/Disk 성능을 $SampleSeconds 초 동안 측정합니다."

    $sampleCount = [math]::Ceiling($SampleSeconds / $SampleIntervalSeconds)
    for ($index = 1; $index -le $sampleCount; $index++) {
        try {
            $processorPerf = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter "Name='_Total'" -ErrorAction SilentlyContinue
            $memoryPerf = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory -ErrorAction SilentlyContinue
            $diskPerf = Get-CimInstance Win32_PerfFormattedData_PerfDisk_PhysicalDisk -Filter "Name='_Total'" -ErrorAction SilentlyContinue

            $performanceSamples += [PSCustomObject]@{
                측정시각                  = Get-Date
                CPU_Percent               = if ($processorPerf) { [double]$processorPerf.PercentProcessorTime } else { $null }
                AvailableMemory_MB         = if ($memoryPerf) { [double]$memoryPerf.AvailableMBytes } else { $null }
                CommittedMemory_Percent    = if ($memoryPerf) { [double]$memoryPerf.PercentCommittedBytesInUse } else { $null }
                DiskTime_Percent           = if ($diskPerf) { [double]$diskPerf.PercentDiskTime } else { $null }
                DiskQueueLength            = if ($diskPerf) { [double]$diskPerf.CurrentDiskQueueLength } else { $null }
                DiskReadBytesPerSec        = if ($diskPerf) { [double]$diskPerf.DiskReadBytesPersec } else { $null }
                DiskWriteBytesPerSec       = if ($diskPerf) { [double]$diskPerf.DiskWriteBytesPersec } else { $null }
            }
        }
        catch {
            Write-Warning "성능 표본 수집 실패: $($_.Exception.Message)"
        }

        if ($index -lt $sampleCount) {
            Start-Sleep -Seconds $SampleIntervalSeconds
        }
    }

    Export-SafeCsv $performanceSamples (Join-Path $outputDirectory "40_PerformanceSamples.csv")

    if ($performanceSamples.Count -gt 0) {
        $cpuStats = $performanceSamples | Measure-Object CPU_Percent -Average -Maximum
        $availableMemStats = $performanceSamples | Measure-Object AvailableMemory_MB -Average -Minimum
        $committedMemStats = $performanceSamples | Measure-Object CommittedMemory_Percent -Average -Maximum
        $diskTimeStats = $performanceSamples | Measure-Object DiskTime_Percent -Average -Maximum
        $diskQueueStats = $performanceSamples | Measure-Object DiskQueueLength -Average -Maximum

        $performanceSummary = @(
            [PSCustomObject]@{ 지표 = "CPU 평균"; 값 = [math]::Round($cpuStats.Average, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "CPU 최대"; 값 = [math]::Round($cpuStats.Maximum, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "가용 Memory 평균"; 값 = [math]::Round($availableMemStats.Average, 2); 단위 = "MB" },
            [PSCustomObject]@{ 지표 = "가용 Memory 최소"; 값 = [math]::Round($availableMemStats.Minimum, 2); 단위 = "MB" },
            [PSCustomObject]@{ 지표 = "Committed Memory 평균"; 값 = [math]::Round($committedMemStats.Average, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "Committed Memory 최대"; 값 = [math]::Round($committedMemStats.Maximum, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "Disk 사용률 평균"; 값 = [math]::Round($diskTimeStats.Average, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "Disk 사용률 최대"; 값 = [math]::Round($diskTimeStats.Maximum, 2); 단위 = "%" },
            [PSCustomObject]@{ 지표 = "Disk Queue 평균"; 값 = [math]::Round($diskQueueStats.Average, 2); 단위 = "" },
            [PSCustomObject]@{ 지표 = "Disk Queue 최대"; 값 = [math]::Round($diskQueueStats.Maximum, 2); 단위 = "" }
        )
    }
    Export-SafeCsv $performanceSummary (Join-Path $outputDirectory "41_PerformanceSummary.csv")

    # -----------------------------------------------------------------------
    # 6. 통합 요약 및 등급 판정
    # -----------------------------------------------------------------------
    Write-Step "통합 요약과 단말 등급을 산정합니다."

    $totalCores = ($cpus | Measure-Object NumberOfCores -Sum).Sum
    $totalLogicalProcessors = ($cpus | Measure-Object NumberOfLogicalProcessors -Sum).Sum
    $totalMemoryGB = [math]::Round((($memoryModules | Measure-Object Capacity -Sum).Sum / 1GB), 1)

    $systemDrive = $logicalDisks | Where-Object { $_.DeviceID -eq $env:SystemDrive } | Select-Object -First 1
    $systemDriveFreeGB = if ($systemDrive) { Convert-ToGB $systemDrive.FreeSpace } else { $null }
    $systemDriveSizeGB = if ($systemDrive) { Convert-ToGB $systemDrive.Size } else { $null }

    $grade = "미달"
    if ($totalCores -ge 8 -and $totalMemoryGB -ge 32 -and $systemDriveFreeGB -ge 250) {
        $grade = "A"
    }
    elseif ($totalCores -ge 6 -and $totalMemoryGB -ge 16 -and $systemDriveFreeGB -ge 150) {
        $grade = "B"
    }
    elseif ($totalCores -ge 4 -and $totalMemoryGB -ge 8 -and $systemDriveFreeGB -ge 100) {
        $grade = "C"
    }

    $averageCpu = $null
    $maximumCpu = $null
    $minimumAvailableMemory = $null
    if ($performanceSamples.Count -gt 0) {
        $averageCpu = [math]::Round(($performanceSamples | Measure-Object CPU_Percent -Average).Average, 2)
        $maximumCpu = [math]::Round(($performanceSamples | Measure-Object CPU_Percent -Maximum).Maximum, 2)
        $minimumAvailableMemory = [math]::Round(($performanceSamples | Measure-Object AvailableMemory_MB -Minimum).Minimum, 2)
    }

    $lastBoot = Convert-WmiDate $os.LastBootUpTime
    $installDate = Convert-WmiDate $os.InstallDate
    $uptimeDays = $null
    if ($lastBoot) {
        $uptimeDays = [math]::Round(((Get-Date) - $lastBoot).TotalDays, 1)
    }

    $systemSummary = @([PSCustomObject]@{
        조사시각                   = Get-Date
        컴퓨터명                   = $computerName
        사용자                     = $env:USERNAME
        관리자권한실행             = $isAdministrator
        도메인                     = $computer.Domain
        제조사                     = $computer.Manufacturer
        모델                       = $computer.Model
        SystemType                 = $computer.SystemType
        SerialNumber               = $bios.SerialNumber
        BIOSVersion                = ($bios.SMBIOSBIOSVersion -join ", ")
        BaseBoard                   = Get-SafeTrimmedText ("{0} {1}" -f $baseBoard.Manufacturer, $baseBoard.Product)
        OS                         = $os.Caption
        OSVersion                  = $os.Version
        OSBuild                    = $os.BuildNumber
        OSArchitecture             = $os.OSArchitecture
        Windows설치일              = $installDate
        최근부팅시각               = $lastBoot
        가동일수                   = $uptimeDays
        CPU명                      = (($cpus | Select-Object -ExpandProperty Name) -join " / ")
        물리Core                   = $totalCores
        논리Processor              = $totalLogicalProcessors
        Memory_GB                  = $totalMemoryGB
        SystemDrive                = $env:SystemDrive
        SystemDriveSize_GB         = $systemDriveSizeGB
        SystemDriveFree_GB         = $systemDriveFreeGB
        SecureBoot                 = $secureBootValue
        설치소프트웨어수           = $installedSoftware.Count
        NSIGHT핵심솔루션후보수      = $targetComponents.Count
        CPU평균_Percent            = $averageCpu
        CPU최대_Percent            = $maximumCpu
        최소가용Memory_MB          = $minimumAvailableMemory
        단말등급                   = $grade
        성능측정초                 = $SampleSeconds
    })

    Export-SafeCsv $systemSummary (Join-Path $outputDirectory "00_PC_통합요약.csv")

    # -----------------------------------------------------------------------
    # 7. HTML 보고서
    # -----------------------------------------------------------------------
    Write-Step "HTML 통합 보고서를 생성합니다."

    $style = @"
<style>
body { font-family: 'Malgun Gothic', Arial, sans-serif; margin: 24px; color: #222; }
h1 { background:#17365D; color:white; padding:14px; font-size:22px; }
h2 { color:#1F4E78; border-bottom:2px solid #1F4E78; padding-bottom:5px; margin-top:26px; }
table { border-collapse: collapse; width:100%; margin-bottom:18px; font-size:12px; }
th { background:#0F6B78; color:white; padding:7px; border:1px solid #cbd5df; }
td { padding:6px; border:1px solid #d9e1f2; vertical-align:top; }
tr:nth-child(even) { background:#f5f8fb; }
.note { background:#fff2cc; padding:10px; border:1px solid #e6d27a; }
.ok { color:#176b2c; font-weight:bold; }
</style>
"@

    $summaryHtml = $systemSummary | ConvertTo-Html -Fragment
    $componentHtml = $targetComponents | Select-Object -First 100 | ConvertTo-Html -Fragment
    $performanceHtml = $performanceSummary | ConvertTo-Html -Fragment
    $diskHtml = $logicalDiskInfo | ConvertTo-Html -Fragment
    $networkHtml = $ipConfigurations | ConvertTo-Html -Fragment
    $topProcessHtml = $processes | Select-Object -First 30 | ConvertTo-Html -Fragment

    $html = @"
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="utf-8">
<title>NSIGHT PC 통합 조사 결과 - $computerName</title>
$style
</head>
<body>
<h1>NSIGHT 현업 단말 PC 통합 조사 결과</h1>
<div class="note">
조사 PC: <b>$computerName</b> /
조사 시각: <b>$(Get-Date -Format "yyyy-MM-dd HH:mm:ss")</b> /
관리자 권한: <b>$isAdministrator</b><br>
본 자료의 자원 사용량은 $SampleSeconds 초 동안의 표본이며, 업무 피크 시간대에 다시 실행하면 더 정확한 결과를 얻을 수 있습니다.
</div>

<h2>1. PC 통합 요약</h2>
$summaryHtml

<h2>2. NSIGHT 핵심 솔루션 후보</h2>
$componentHtml

<h2>3. 성능 요약</h2>
$performanceHtml

<h2>4. Disk 현황</h2>
$diskHtml

<h2>5. 네트워크 설정</h2>
$networkHtml

<h2>6. Memory 상위 프로세스 30개</h2>
$topProcessHtml

<h2>7. 상세 결과 파일</h2>
<p>동일 폴더의 CSV 파일에서 설치 프로그램, 서비스, 자동실행, 예약 작업, Hotfix, Windows 기능, 포트 등 상세 내용을 확인하십시오.</p>
</body>
</html>
"@

    $htmlPath = Join-Path $outputDirectory "00_PC_통합보고서.html"
    $html | Out-File -FilePath $htmlPath -Encoding UTF8 -Force

    # -----------------------------------------------------------------------
    # 8. 조사 파일 목록
    # -----------------------------------------------------------------------
    $fileIndex = Get-ChildItem -Path $outputDirectory -File |
        ForEach-Object {
            [PSCustomObject]@{
                파일명       = $_.Name
                크기_KB      = [math]::Round($_.Length / 1KB, 2)
                수정시각     = $_.LastWriteTime
                전체경로     = $_.FullName
            }
        }
    Export-SafeCsv $fileIndex (Join-Path $outputDirectory "98_결과파일목록.csv")
}
catch {
    Write-Error "통합 조사 중 예외가 발생했습니다: $($_.Exception.Message)"
    $_ | Out-File -FilePath (Join-Path $outputDirectory "99_오류상세.txt") -Encoding UTF8 -Append
}
finally {
    try { Stop-Transcript | Out-Null } catch {}
}

# ---------------------------------------------------------------------------
# ZIP 압축
# ---------------------------------------------------------------------------
Write-Step "조사 결과를 ZIP으로 압축합니다."
try {
    if (Test-Path $zipPath) {
        Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
    }
    Compress-Archive -Path (Join-Path $outputDirectory "*") -DestinationPath $zipPath -Force
}
catch {
    Write-Warning "ZIP 압축 실패: $($_.Exception.Message)"
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "NSIGHT 단말 PC 통합 조사가 완료되었습니다." -ForegroundColor Green
Write-Host "결과 폴더 : $outputDirectory" -ForegroundColor Yellow
Write-Host "ZIP 파일  : $zipPath" -ForegroundColor Yellow
Write-Host "요약 보고서: $(Join-Path $outputDirectory '00_PC_통합보고서.html')" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Green

# 결과 폴더를 자동으로 엽니다.
try {
    Start-Process explorer.exe $outputDirectory
}
catch {}
