<#
Start the Chef AI server (PowerShell).
Binds the server to 0.0.0.0 so other devices on your LAN can connect.
#>
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

if (-Not (Test-Path ".venv")) {
    python -m venv .venv
}

Set-Location $PSScriptRoot
& .\.venv\Scripts\Activate.ps1

pip install -r requirements.txt

python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload