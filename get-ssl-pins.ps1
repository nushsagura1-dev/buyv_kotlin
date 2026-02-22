# ========================================
# 🔒 Certificate Pin Generator (PowerShell)
# ========================================
# 
# Script PowerShell pour générer les pins SHA-256 des certificats SSL
# pour Certificate Pinning dans Android.
#
# Usage:
#   .\get-ssl-pins.ps1 api.buyv.com
#   .\get-ssl-pins.ps1 api.buyv.com 443
#

param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Domain,
    
    [Parameter(Mandatory=$false, Position=1)]
    [int]$Port = 443
)

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Get-CertificatePin {
    param(
        [System.Security.Cryptography.X509Certificates.X509Certificate2]$Certificate
    )
    
    # Extract public key
    $publicKey = $Certificate.PublicKey.EncodedKeyValue.RawData
    
    # Calculate SHA-256 hash
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $hash = $sha256.ComputeHash($publicKey)
    
    # Convert to Base64
    $base64 = [Convert]::ToBase64String($hash)
    
    return $base64
}

Write-ColorOutput "========================================" "Blue"
Write-ColorOutput "🔒 Certificate Pin Generator" "Blue"
Write-ColorOutput "========================================" "Blue"
Write-Host ""
Write-ColorOutput "Domain: $Domain" "Green"
Write-ColorOutput "Port:   $Port" "Green"
Write-Host ""

Write-ColorOutput "📡 Fetching certificate from server..." "Yellow"
Write-Host ""

try {
    # Create TCP client
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $tcpClient.Connect($Domain, $Port)
    
    # Create SSL stream
    $sslStream = New-Object System.Net.Security.SslStream($tcpClient.GetStream(), $false, {$true})
    $sslStream.AuthenticateAsClient($Domain)
    
    # Get remote certificate
    $remoteCertificate = $sslStream.RemoteCertificate
    $certificate = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($remoteCertificate)
    
    Write-ColorOutput "✅ Certificate retrieved successfully" "Green"
    Write-Host ""
    
    Write-ColorOutput "========================================" "Blue"
    Write-ColorOutput "📋 Certificate Information" "Blue"
    Write-ColorOutput "========================================" "Blue"
    Write-Host ""
    
    Write-Host "Subject:    $($certificate.Subject)"
    Write-Host "Issuer:     $($certificate.Issuer)"
    Write-Host "Valid From: $($certificate.NotBefore)"
    Write-Host "Valid To:   $($certificate.NotAfter)"
    Write-Host "Thumbprint: $($certificate.Thumbprint)"
    Write-Host ""
    
    # Calculate pin
    $pin = Get-CertificatePin -Certificate $certificate
    
    Write-ColorOutput "========================================" "Blue"
    Write-ColorOutput "🔑 Certificate Pin (SHA-256)" "Blue"
    Write-ColorOutput "========================================" "Blue"
    Write-Host ""
    
    Write-ColorOutput "📌 Server Certificate Pin:" "Green"
    Write-ColorOutput "   sha256/$pin" "White"
    Write-Host ""
    
    # Try to get certificate chain
    Write-ColorOutput "📌 Certificate Chain:" "Yellow"
    $chain = New-Object System.Security.Cryptography.X509Certificates.X509Chain
    $chain.Build($certificate) | Out-Null
    
    if ($chain.ChainElements.Count -gt 1) {
        Write-Host ""
        foreach ($element in $chain.ChainElements) {
            if ($element.Certificate.Thumbprint -ne $certificate.Thumbprint) {
                $chainPin = Get-CertificatePin -Certificate $element.Certificate
                Write-ColorOutput "   sha256/$chainPin" "White"
                Write-ColorOutput "   Subject: $($element.Certificate.Subject)" "DarkGray"
                Write-Host ""
            }
        }
    } else {
        Write-ColorOutput "   ⚠️  No intermediate CA found in chain" "Yellow"
        Write-Host ""
    }
    
    Write-ColorOutput "========================================" "Blue"
    Write-ColorOutput "📝 Configuration Examples" "Blue"
    Write-ColorOutput "========================================" "Blue"
    Write-Host ""
    
    Write-ColorOutput "Android XML (network_security_config.xml):" "Yellow"
    Write-Host ""
    Write-Host @"
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">$Domain</domain>
    <pin-set expiration="2027-01-01">
        <pin digest="sha256">$pin</pin>
    </pin-set>
    <trust-anchors>
        <certificates src="system"/>
    </trust-anchors>
</domain-config>
"@
    
    Write-Host ""
    Write-Host ""
    Write-ColorOutput "Kotlin (CertificatePinningConfig.kt):" "Yellow"
    Write-Host ""
    Write-Host @"
CertificatePinner.Builder()
    .add("$Domain", "sha256/$pin")
    .build()
"@
    
    Write-Host ""
    Write-Host ""
    Write-ColorOutput "========================================" "Green"
    Write-ColorOutput "✅ Certificate pins generated successfully!" "Green"
    Write-ColorOutput "========================================" "Green"
    Write-Host ""
    
    Write-ColorOutput "⚠️  Important Reminders:" "Yellow"
    Write-Host "  • Always have at least 2 pins (current + backup)"
    Write-Host "  • Test in staging before deploying to production"
    Write-Host "  • Set up certificate expiration alerts"
    Write-Host "  • Update pins BEFORE certificate expires"
    Write-Host ""
    
    Write-ColorOutput "📚 Documentation:" "Blue"
    Write-Host "  See CERTIFICATE_PINNING.md for detailed instructions"
    Write-Host ""
    
    # Cleanup
    $sslStream.Close()
    $tcpClient.Close()
    
} catch {
    Write-ColorOutput "❌ Error: $($_.Exception.Message)" "Red"
    Write-Host ""
    Write-Host "Possible reasons:"
    Write-Host "  • Domain is not reachable"
    Write-Host "  • Port is incorrect"
    Write-Host "  • Firewall blocking connection"
    Write-Host "  • Certificate validation failed"
    Write-Host ""
    exit 1
}
