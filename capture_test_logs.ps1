# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 🎬 SCRIPT DE TEST AUTOMATIQUE - BuyV Kotlin Android
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Ce script capture TOUS les logs de votre session de test
# depuis la seconde 0 jusqu'à la fin et génère un rapport HTML
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

param(
    [switch]$RapportSeul  # Générer seulement le rapport d'un log existant
)

# Couleurs
function Write-Success { param($msg) Write-Host "✅ $msg" -ForegroundColor Green }
function Write-Info { param($msg) Write-Host "ℹ️  $msg" -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host "⚠️  $msg" -ForegroundColor Yellow }
function Write-Error2 { param($msg) Write-Host "❌ $msg" -ForegroundColor Red }
function Write-Title { param($msg) Write-Host "`n━━━ $msg ━━━" -ForegroundColor Magenta }

# Variables
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = "test_session_$timestamp.log"
$rapportFile = "RAPPORT_TEST_$timestamp.html"

# Vérifier si ADB est disponible
function Test-ADB {
    try {
        $null = adb version 2>&1
        return $true
    } catch {
        return $false
    }
}

# Fonction de génération de rapport
function New-TestReport {
    param([string]$logPath)
    
    Write-Title "Génération du rapport"
    
    if (-not (Test-Path $logPath)) {
        Write-Error2 "Fichier log introuvable: $logPath"
        return
    }
    
    # Analyser les logs
    $totalLogs = (Get-Content $logPath).Count
    $erreurs = @(Select-String -Path $logPath -Pattern "❌|Error|Exception|FATAL" -ErrorAction SilentlyContinue)
    $succes = @(Select-String -Path $logPath -Pattern "✅|successful|Success|completed" -ErrorAction SilentlyContinue)
    $apiCalls = @(Select-String -Path $logPath -Pattern "Retrofit|OkHttp" -ErrorAction SilentlyContinue)
    $marketplace = @(Select-String -Path $logPath -Pattern "MARKETPLACE_VM|PRODUCT_DETAIL" -ErrorAction SilentlyContinue)
    $admin = @(Select-String -Path $logPath -Pattern "AdminAuthVM|AdminDashboardVM|AdminOrderVM|AdminCommissionVM" -ErrorAction SilentlyContinue)
    $stripe = @(Select-String -Path $logPath -Pattern "StripePayment|PaymentViewModel" -ErrorAction SilentlyContinue)
    $firebase = @(Select-String -Path $logPath -Pattern "Firebase|FCM" -ErrorAction SilentlyContinue)
    
    Write-Info "Total logs: $totalLogs"
    Write-Info "Erreurs: $($erreurs.Count)"
    Write-Info "Succès: $($succes.Count)"
    Write-Info "API Calls: $($apiCalls.Count)"
    
    # Générer HTML
    $html = @"
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rapport Test BuyV - $timestamp</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        header {
            background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        header h1 { font-size: 2.5em; margin-bottom: 10px; }
        header p { opacity: 0.9; font-size: 1.1em; }
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
        }
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            text-align: center;
            transition: transform 0.2s;
        }
        .stat-card:hover { transform: translateY(-5px); }
        .stat-value {
            font-size: 3em;
            font-weight: bold;
            margin: 15px 0;
        }
        .stat-label {
            color: #666;
            text-transform: uppercase;
            font-size: 0.85em;
            letter-spacing: 1px;
        }
        .success { color: #4CAF50; }
        .error { color: #f44336; }
        .info { color: #2196F3; }
        .warning { color: #FF9800; }
        section {
            padding: 30px;
            border-top: 1px solid #e0e0e0;
        }
        h2 {
            color: #333;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-left: 5px solid #2196F3;
            padding-left: 15px;
            font-size: 1.8em;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            background: white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            border-radius: 8px;
            overflow: hidden;
        }
        th {
            background: #2196F3;
            color: white;
            padding: 15px;
            text-align: left;
            font-weight: 600;
        }
        td {
            padding: 12px 15px;
            border-bottom: 1px solid #e0e0e0;
        }
        tr:hover { background: #f5f5f5; }
        .log-entry {
            font-family: 'Consolas', 'Monaco', monospace;
            font-size: 0.9em;
            line-height: 1.5;
            white-space: pre-wrap;
            word-break: break-all;
        }
        .timestamp {
            color: #999;
            font-size: 0.85em;
            white-space: nowrap;
        }
        .no-data {
            text-align: center;
            padding: 40px;
            color: #4CAF50;
            font-size: 1.2em;
        }
        footer {
            background: #263238;
            color: #fff;
            padding: 20px;
            text-align: center;
        }
        .badge {
            display: inline-block;
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 0.85em;
            margin: 0 5px;
        }
        .badge-success { background: #4CAF50; color: white; }
        .badge-error { background: #f44336; color: white; }
        .badge-info { background: #2196F3; color: white; }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>📊 Rapport de Test Android</h1>
            <p><strong>BuyV Kotlin E-Commerce</strong></p>
            <p>Session du $(Get-Date -Format "dd/MM/yyyy à HH:mm:ss")</p>
        </header>
        
        <div class="stats">
            <div class="stat-card">
                <div class="stat-label">Total Logs</div>
                <div class="stat-value info">$totalLogs</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">API Calls</div>
                <div class="stat-value info">$($apiCalls.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Succès ✅</div>
                <div class="stat-value success">$($succes.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Erreurs ❌</div>
                <div class="stat-value error">$($erreurs.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Marketplace</div>
                <div class="stat-value info">$($marketplace.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Admin</div>
                <div class="stat-value warning">$($admin.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Stripe</div>
                <div class="stat-value success">$($stripe.Count)</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Firebase</div>
                <div class="stat-value warning">$($firebase.Count)</div>
            </div>
        </div>
        
        <section>
            <h2>🚨 Erreurs Détectées</h2>
            $(
                if ($erreurs.Count -gt 0) {
                    "<table><tr><th style='width:120px'>Ligne</th><th>Message</th></tr>" +
                    ($erreurs | ForEach-Object {
                        "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                    } | Out-String) +
                    "</table>"
                } else {
                    "<div class='no-data'>✅ Aucune erreur détectée - Session parfaite !</div>"
                }
            )
        </section>
        
        <section>
            <h2>✅ Actions Réussies (30 dernières)</h2>
            <table>
                <tr><th style='width:120px'>Ligne</th><th>Message</th></tr>
                $($succes | Select-Object -Last 30 | ForEach-Object {
                    "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                })
            </table>
        </section>
        
        <section>
            <h2>🌐 Appels API (30 derniers)</h2>
            <table>
                <tr><th style='width:120px'>Ligne</th><th>Requête</th></tr>
                $($apiCalls | Select-Object -Last 30 | ForEach-Object {
                    "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                })
            </table>
        </section>
        
        <section>
            <h2>🛒 Navigation Marketplace</h2>
            <table>
                <tr><th style='width:120px'>Ligne</th><th>Action</th></tr>
                $($marketplace | Select-Object -Last 20 | ForEach-Object {
                    "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                })
            </table>
        </section>
        
        <section>
            <h2>🔐 Activité Admin</h2>
            $(
                if ($admin.Count -gt 0) {
                    "<table><tr><th style='width:120px'>Ligne</th><th>Action</th></tr>" +
                    ($admin | Select-Object -Last 20 | ForEach-Object {
                        "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                    } | Out-String) +
                    "</table>"
                } else {
                    "<div class='no-data'>Aucune activité admin dans cette session</div>"
                }
            )
        </section>
        
        <section>
            <h2>💳 Paiements Stripe</h2>
            $(
                if ($stripe.Count -gt 0) {
                    "<table><tr><th style='width:120px'>Ligne</th><th>Action</th></tr>" +
                    ($stripe | ForEach-Object {
                        "<tr><td class='timestamp'>$($_.LineNumber)</td><td class='log-entry'>$([System.Web.HttpUtility]::HtmlEncode($_.Line))</td></tr>"
                    } | Out-String) +
                    "</table>"
                } else {
                    "<div class='no-data'>Aucun paiement effectué dans cette session</div>"
                }
            )
        </section>
        
        <footer>
            <p><strong>Fichier source:</strong> $logPath</p>
            <p style="margin-top: 10px; opacity: 0.7;">Généré automatiquement par BuyV Test Suite - $(Get-Date -Format "dd/MM/yyyy HH:mm:ss")</p>
        </footer>
    </div>
</body>
</html>
"@

    # Sauvegarder
    $html | Out-File -FilePath $rapportFile -Encoding UTF8
    Write-Success "Rapport généré: $rapportFile"
    
    # Ouvrir dans le navigateur
    Start-Process $rapportFile
    Write-Info "Rapport ouvert dans le navigateur"
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# PROGRAMME PRINCIPAL
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host @"

    ╔═══════════════════════════════════════════════════════════╗
    ║                                                           ║
    ║         📱 BuyV Kotlin - Test Logger & Reporter         ║
    ║                                                           ║
    ║         Capture complète de vos sessions de test         ║
    ║                                                           ║
    ╚═══════════════════════════════════════════════════════════╝

"@ -ForegroundColor Cyan

# Mode rapport seul
if ($RapportSeul) {
    $logFiles = Get-ChildItem -Filter "test_session_*.log" | Sort-Object LastWriteTime -Descending
    if ($logFiles.Count -eq 0) {
        Write-Error2 "Aucun fichier de log trouvé"
        exit 1
    }
    
    Write-Info "Fichiers log disponibles:"
    for ($i = 0; $i -lt [Math]::Min(5, $logFiles.Count); $i++) {
        Write-Host "  $($i+1). $($logFiles[$i].Name) - $($logFiles[$i].LastWriteTime)"
    }
    
    $choice = Read-Host "`nChoisir un fichier (1-$([Math]::Min(5, $logFiles.Count)))"
    $selectedLog = $logFiles[$choice - 1].FullName
    
    New-TestReport -logPath $selectedLog
    exit 0
}

# Vérifier ADB
if (-not (Test-ADB)) {
    Write-Error2 "ADB n'est pas installé ou pas dans le PATH"
    Write-Info "Installez Android SDK Platform Tools"
    exit 1
}

# Vérifier device connecté
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Error2 "Aucun device Android détecté"
    Write-Info "Connectez votre device et activez le débogage USB"
    exit 1
}

Write-Success "Device Android détecté"

# Vider les anciens logs
Write-Info "Nettoyage des anciens logs..."
adb logcat -c
Write-Success "Logs vidés"

# Démarrer la capture
Write-Title "CAPTURE EN COURS"
Write-Info "Fichier: $logFile"
Write-Warning "Effectuez vos tests maintenant"
Write-Warning "Appuyez sur Ctrl+C quand vous avez terminé"
Write-Host ""

try {
    # Capturer avec horodatage
    adb logcat -v time 2>&1 | Tee-Object -FilePath $logFile
} finally {
    Write-Host "`n"
    Write-Title "Session Terminée"
    Write-Info "Arrêt de la capture à $(Get-Date -Format 'HH:mm:ss')"
    
    # Générer le rapport
    New-TestReport -logPath $logFile
    
    # Statistiques finales
    Write-Host "`n"
    Write-Success "Session de test complète enregistrée"
    Write-Info "Log complet: $logFile"
    Write-Info "Rapport HTML: $rapportFile"
}
