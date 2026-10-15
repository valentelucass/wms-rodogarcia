# Exclusao local antes de qualquer SQL. A espera nao repete conexao nem migration.
function Enter-WmsDatabasePreparationLock {
    param([ValidateRange(0,240)][int]$WaitSeconds=180,
        [string]$Name='Local\WMS_Rodogarcia_Preparo_Bancos')
    if(-not ('WmsPreparationMutexMetadata' -as [type])){
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class WmsPreparationMutexMetadata {
    [StructLayout(LayoutKind.Sequential)] private struct Owner { public IntPtr Process; public IntPtr Thread; }
    [DllImport("ntdll.dll")] private static extern int NtQueryMutant(IntPtr handle, int informationClass, out Owner info, uint length, out uint returned);
    public static long? OwnerPid(IntPtr handle) {
        try {
            Owner info; uint returned;
            if (NtQueryMutant(handle, 1, out info, (uint)Marshal.SizeOf(typeof(Owner)), out returned) == 0 && info.Process != IntPtr.Zero)
                return info.Process.ToInt64();
        } catch { }
        return null;
    }
}
'@
    }
    $mutex=[Threading.Mutex]::new($false,$Name)
    $watch=[Diagnostics.Stopwatch]::StartNew()
    $acquired=$false;$owner=$null;$abandoned=$false
    try {
        try {$acquired=$mutex.WaitOne(0)} catch [Threading.AbandonedMutexException] {$acquired=$true;$abandoned=$true}
        if(-not $acquired){
            $owner=[WmsPreparationMutexMetadata]::OwnerPid($mutex.SafeWaitHandle.DangerousGetHandle())
            if($WaitSeconds -gt 0){
                try {$acquired=$mutex.WaitOne([TimeSpan]::FromSeconds($WaitSeconds))}
                catch [Threading.AbandonedMutexException] {$acquired=$true;$abandoned=$true}
            }
            if(-not $acquired){$owner=[WmsPreparationMutexMetadata]::OwnerPid($mutex.SafeWaitHandle.DangerousGetHandle())}
        }
        $watch.Stop()
        [pscustomobject]@{mutex=$mutex;acquired=$acquired;waitedMs=$watch.ElapsedMilliseconds;
            waitLimitSeconds=$WaitSeconds;observedOwnerPid=$owner;abandoned=$abandoned}
    } catch {$mutex.Dispose();throw}
}
function Exit-WmsDatabasePreparationLock($Lock){
    if($Lock -and $Lock.mutex){
        try {if($Lock.acquired){$Lock.mutex.ReleaseMutex();$Lock.acquired=$false}}
        finally {$Lock.mutex.Dispose();$Lock.mutex=$null}
    }
}
function Get-WmsDatabasePreparationLockReport($Lock){
    if($Lock){[pscustomobject]@{adquirida=$Lock.acquired;esperaMs=$Lock.waitedMs;
        esperaLimiteSegundos=$Lock.waitLimitSeconds;pidObservado=$Lock.observedOwnerPid;abandonada=$Lock.abandoned}}
}
