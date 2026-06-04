$ErrorActionPreference = "SilentlyContinue"

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class MemoryTrim {
    [DllImport("psapi.dll")]
    public static extern bool EmptyWorkingSet(IntPtr hProcess);
}
"@

Get-Process |
    Where-Object { $_.WorkingSet64 -gt 80MB -and $_.Id -ne $PID } |
    ForEach-Object {
        try { [MemoryTrim]::EmptyWorkingSet($_.Handle) | Out-Null } catch {}
    }

[GC]::Collect()
[GC]::WaitForPendingFinalizers()
Write-Host "Memory trim requested."
