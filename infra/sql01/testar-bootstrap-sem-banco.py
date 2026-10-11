"""Regressão pura do launcher: somente loader Spring Boot e main sem rede no JAR.

Requer Python 3 e JDK 21. --jar é lido somente para extrair classes do loader;
nenhuma classe WMS, biblioteca de persistência ou contexto Spring é executado.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import uuid
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--jar", type=Path, required=True)
parser.add_argument("--java", type=Path, required=True)
options = parser.parse_args()
root = Path(__file__).resolve().parents[2]
run = root / "orchestracao/.runtime/qual-conf01/sql01/verificacao" / ("v" + uuid.uuid4().hex[:8])
run.mkdir()
env = {k: v for k, v in os.environ.items() if k.upper() in (
    "SYSTEMROOT", "WINDIR", "TEMP", "TMP", "USERPROFILE", "LOCALAPPDATA", "APPDATA")}


def execute(command, private=None):
    return subprocess.run(command, input=private, capture_output=True, env=env, timeout=45)


source = run / "PureMain.java"
source.write_text('''package fixture;
public class PureMain {
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String prefix = "--spring.config.additional-location=";
            if (arg.startsWith(prefix)) {
                var config = java.nio.file.Path.of(java.net.URI.create(arg.substring(prefix.length())));
                java.nio.file.Files.writeString(config.resolveSibling("main-reached.txt"),
                    "PURE", java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
        System.out.println("PURE_BOOT_LOADER_REACHED");
    }
}
''', encoding="utf-8")
classes = run / "classes"
classes.mkdir()
compile_result = execute([str(options.java.with_name("javac.exe")), "-d", str(classes), str(source)])
if compile_result.returncode:
    raise SystemExit("PURE_FIXTURE_COMPILE_REFUSED")
jar = run / "pure-loader.jar"
with zipfile.ZipFile(options.jar) as original, zipfile.ZipFile(jar, "x") as target:
    for name in original.namelist():
        if name.startswith("org/springframework/boot/loader/") and not name.endswith("/"):
            target.writestr(name, original.read(name))
    target.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n"
                    "Main-Class: org.springframework.boot.loader.launch.JarLauncher\r\n"
                    "Start-Class: fixture.PureMain\r\n\r\n")
    target.writestr("BOOT-INF/classes/", b"")
    target.writestr("BOOT-INF/classes/fixture/PureMain.class",
                    (classes / "fixture/PureMain.class").read_bytes())
jar_hash = hashlib.sha256(jar.read_bytes()).hexdigest()
password = b"FICTICIO-" + b"x" * 32
key = b"RklDVElDSU9OQU9FUlNB"
private = struct.pack(">I", 0x574D5301)
for frame in (password, key, b""):
    private += struct.pack(">I", len(frame)) + frame
fixture_root = run / "r"
for directory in ("infra/sql01", "infra/auth", "database/scripts", "backend"):
    (fixture_root / directory).mkdir(parents=True)
for name in ("Sql01Bootstrap.java", "launcher-backend.ps1", "transporte-privado.ps1"):
    shutil.copyfile(root / "infra/sql01" / name, fixture_root / "infra/sql01" / name)
bootstrap = fixture_root / "infra/sql01/Sql01Bootstrap.java"
tests = []
direct = execute([str(options.java), "-jar", str(jar)])
tests.append({"case": "fixture_direct_java_jar", "passed": direct.returncode == 0
              and b"PURE_BOOT_LOADER_REACHED" in direct.stdout, "exit": direct.returncode})
for name, classpath in (("red_without_classpath", False), ("green_with_classpath", True)):
    run_id = str(uuid.uuid4())
    diagnostic_dir = fixture_root / "orchestracao/.runtime/qual-conf01/sql01/prumo/runs" / run_id
    diagnostic_dir.mkdir(parents=True)
    diagnostic = diagnostic_dir / "bootstrap-diagnostic.json"
    config_dir = run / name
    config_dir.mkdir()
    config = config_dir / "fixture.properties"
    config.write_text("fixture.only=true\n", encoding="utf-8")
    args = [str(jar), str(config), "25584", "FICTICIO-SERVER", "FICTICIO-CERT",
            str(config), str(fixture_root), str(config_dir), "a" * 64,
            jar_hash, run_id, str(diagnostic)]
    prefix = ["-cp", str(jar)] if classpath else []
    result = execute([str(options.java), *prefix, str(bootstrap), *args], private)
    reached = (config_dir / "main-reached.txt").exists()
    diag = json.loads(diagnostic.read_text()) if diagnostic.exists() else None
    expected = result.returncode == 0 and reached and diag is None if classpath else (
        result.returncode == 20 and not reached and diag is not None
        and diag["phase"] == "APPLICATION_LAUNCH"
        and diag["chain"][-1] == "java.net.MalformedURLException")
    tests.append({"case": name, "passed": expected, "exit": result.returncode,
                  "mainReached": reached, "diagnostic": diag})

# O Start real só recebe plano e material fictícios nesta cópia isolada.
# Nenhuma guarda, conexão, DPAPI ou consulta de portas é executada pelo harness.
(fixture_root / "database/scripts/d26-credencial-aplicacao.ps1").write_text('''
function Get-WmsDevApplicationCredential {
    [Management.Automation.PSCredential]::new('WMSDEV',
        (ConvertTo-SecureString ('FICTICIO-'+('x'*32)) -AsPlainText -Force))
}
''', encoding="utf-8")
(fixture_root / "infra/auth/login-protegido.ps1").write_text('''
function Get-WmsLoginMaterialPath { 'FICTICIO_NOT_A_REAL_FILE' }
function Assert-WmsLoginPrivatePath { }
''', encoding="utf-8")
run_id = str(uuid.uuid4())
diagnostic_dir = fixture_root / "orchestracao/.runtime/qual-conf01/sql01/prumo/runs" / run_id
observation = fixture_root / "orchestracao/.runtime/qual-conf01/sql01/cedro" / run_id / "observations"
diagnostic_dir.mkdir(parents=True)
observation.mkdir(parents=True)
config_dir = run / "actual-launcher"
config_dir.mkdir()
config = config_dir / "fixture.properties"
config.write_text("fixture.only=true\n", encoding="utf-8")
plan = {"run": str(diagnostic_dir), "runId": run_id, "observation": str(observation),
        "candidatePath": "FICTICIO_NO_CANDIDATE", "candidateSha256": "a" * 64,
        "sourceHash": "a" * 64, "jar": str(jar), "jarSha256": jar_hash,
        "java": str(options.java), "config": str(config),
        "guard": {"alvo": {"servidor": "FICTICIO-SERVER"},
                  "perfilProtegido": {"certificateHost": "FICTICIO-CERT"},
                  "tls": {"proveniencia": {"reciboPID": 1, "reciboInicioUtc": "2000-01-01T00:00:00Z"}}},
        "guardPath": "FICTICIO_NO_GUARD", "guardSha256": "b" * 64,
        "reviewPath": "FICTICIO_NO_REVIEW", "reviewSha256": "c" * 64,
        "truststore": str(config), "port": 25584}
(run / "plan.json").write_text(json.dumps(plan), encoding="utf-8")
harness = run / "test-launcher.ps1"
harness.write_text('''
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$script:PurePlan=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'plan.json') -Raw -Encoding UTF8|ConvertFrom-Json
. (Join-Path $PSScriptRoot 'r/infra/sql01/launcher-backend.ps1')
function Assert-WmsSql01LaunchPlan { $script:PurePlan }
function Get-WmsSql01Listeners { @() }
function Import-Clixml {
    ConvertTo-SecureString '{"schema":1,"privateKey":"RklDVElDSU9OQU9FUlNB"}' -AsPlainText -Force
}
$handle=Start-WmsSql01Backend -Candidato 'FICTICIO' -CandidateSha256 ('a'*64) `
    -GuardReceipt 'FICTICIO' -GuardSha256 ('b'*64) -Review 'FICTICIO' -ReviewSha256 ('c'*64) `
    -RunDirectory $script:PurePlan.run -ObservationDirectory $script:PurePlan.observation -Diagnostico
[pscustomobject]@{code=$handle.Sanitized.code;exit=$handle.Sanitized.backend.exitCode;
    phase=$handle.Sanitized.diagnostic.parentPhase;
    bootstrap=$handle.Sanitized.diagnostic.bootstrap;
    mainReached=(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'actual-launcher/main-reached.txt'));
    twinReceiptsMatch=($handle.ReceiptSha256 -ceq $handle.RuntimeReceiptSha256)}|ConvertTo-Json -Depth 8
''', encoding="utf-8")
powershell = Path(os.environ["SystemRoot"]) / "System32/WindowsPowerShell/v1.0/powershell.exe"
actual = execute([str(powershell), "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(harness)])
metadata = json.loads(actual.stdout.decode("utf-8-sig")) if actual.returncode == 0 else None
tests.append({"case": "actual_start_function_with_pure_child", "passed": metadata is not None
              and metadata["mainReached"] and metadata["exit"] == 0
              and metadata["code"] == "SQL01_PROCESS_EXITED" and metadata["twinReceiptsMatch"],
              "metadata": metadata, "harnessExit": actual.returncode})
receipt = {"scope": "PURE_NO_SPRING_CONTEXT_NO_DATABASE_NO_HTTP_NO_REAL_SECRET",
           "sourceJarSha256": hashlib.sha256(options.jar.read_bytes()).hexdigest(),
           "bootstrapSha256": hashlib.sha256(bootstrap.read_bytes()).hexdigest(),
           "launcherSha256": hashlib.sha256((fixture_root / "infra/sql01/launcher-backend.ps1").read_bytes()).hexdigest(),
           "run": str(run), "checks": len(tests), "passed": sum(t["passed"] for t in tests),
           "failed": sum(not t["passed"] for t in tests), "tests": tests,
           "preflightStubbed": True, "preflightProved": False,
           "functionalSQL": 0, "rawLogsSaved": False}
(run / "resultado.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
print(json.dumps(receipt, indent=2))
raise SystemExit(1 if receipt["failed"] else 0)
