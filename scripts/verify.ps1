param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 --release 24 -Xlint:all -Werror -d out @sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.test.SmartDeviceAcceptanceTest
    if ($LASTEXITCODE -ne 0) { throw 'Acceptance failed' }
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.test.SmartDeviceTest
    if ($LASTEXITCODE -ne 0) { throw 'Original demo failed' }

    # 使用真实子进程验证 main 和标准输入 EOF；10 秒超时可发现无限菜单循环。
    $smokeInputs = @("abc`n99`n1`n2`nR`n巡检`n5`nR`n6`nR`n11`nR`n14`n15`n0`n", '', "1`n2`nR`n")
    for ($index = 0; $index -lt $smokeInputs.Count; $index++) {
        $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName = (Get-Command java).Source
        $startInfo.WorkingDirectory = $projectRoot
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardInput = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true
        $startInfo.StandardInputEncoding = [System.Text.UTF8Encoding]::new($false)
        $startInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
        $startInfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8
        foreach ($argument in @('-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-cp', 'out', 'com.cyberzen.smartdevice.app.SmartDeviceApplication')) {
            $startInfo.ArgumentList.Add($argument)
        }
        $process = [System.Diagnostics.Process]::new()
        $process.StartInfo = $startInfo
        try {
            [void]$process.Start()
            $stdoutTask = $process.StandardOutput.ReadToEndAsync()
            $stderrTask = $process.StandardError.ReadToEndAsync()
            $process.StandardInput.Write($smokeInputs[$index])
            $process.StandardInput.Close()
            if (-not $process.WaitForExit(10000)) {
                $process.Kill($true)
                throw "Console smoke $index timed out"
            }
            $stdout = $stdoutTask.GetAwaiter().GetResult()
            $stderr = $stderrTask.GetAwaiter().GetResult()
            if ($process.ExitCode -ne 0) { throw "Console smoke $index failed: $stderr" }
            $expected = if ($index -eq 0) {
                @('请输入数字菜单选项', '无效菜单选项', '注册成功', '正在执行区域巡检任务', '任务已结束', '已从管理系统移除', '设备总数：0', 'VIEW_RECORDS', '已退出')
            } else {
                @('输入已结束，正常退出')
            }
            foreach ($message in $expected) {
                if (-not $stdout.Contains($message)) { throw "Console smoke $index missing: $message" }
            }
            Write-Output "PASS: console process smoke $index (exit 0)"
        } finally {
            $process.Dispose()
        }
    }
    Write-Output 'PASS: compilation, acceptance, original demo and 3 console processes'
} finally {
    Pop-Location
}
