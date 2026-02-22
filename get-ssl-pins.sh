#!/bin/bash

# ========================================
# 🔒 Certificate Pin Generator
# ========================================
# 
# Script pour générer les pins SHA-256 des certificats SSL
# pour Certificate Pinning dans Android.
#
# Usage:
#   ./get-ssl-pins.sh api.buyv.com
#   ./get-ssl-pins.sh api.buyv.com 443
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -eq 0 ]; then
    echo -e "${RED}❌ Error: Domain name required${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 <domain> [port]"
    echo ""
    echo "Examples:"
    echo "  $0 api.buyv.com"
    echo "  $0 api.buyv.com 443"
    exit 1
fi

DOMAIN=$1
PORT=${2:-443}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔒 Certificate Pin Generator${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Domain: ${GREEN}$DOMAIN${NC}"
echo -e "Port:   ${GREEN}$PORT${NC}"
echo ""

# Check if openssl is installed
if ! command -v openssl &> /dev/null; then
    echo -e "${RED}❌ Error: openssl is not installed${NC}"
    echo "Install with: brew install openssl (macOS) or apt-get install openssl (Linux)"
    exit 1
fi

echo -e "${YELLOW}📡 Fetching certificate from server...${NC}"
echo ""

# Get certificate chain
CERT_CHAIN=$(echo | openssl s_client -servername "$DOMAIN" -connect "$DOMAIN:$PORT" 2>/dev/null)

if [ -z "$CERT_CHAIN" ]; then
    echo -e "${RED}❌ Error: Could not connect to $DOMAIN:$PORT${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Certificate chain retrieved${NC}"
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📋 Certificate Information${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Extract and display certificate details
echo "$CERT_CHAIN" | openssl x509 -noout -subject -issuer -dates 2>/dev/null

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔑 Certificate Pins (SHA-256)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Get the server certificate pin
SERVER_PIN=$(echo "$CERT_CHAIN" | openssl x509 -pubkey -noout 2>/dev/null | \
             openssl pkey -pubin -outform der 2>/dev/null | \
             openssl dgst -sha256 -binary | \
             base64)

echo -e "${GREEN}📌 Server Certificate Pin:${NC}"
echo -e "   sha256/${SERVER_PIN}"
echo ""

# Try to get intermediate CA pins
echo -e "${YELLOW}📌 Intermediate CA Pins (if available):${NC}"

# Get all certificates in the chain
CERT_COUNT=$(echo "$CERT_CHAIN" | grep -c "BEGIN CERTIFICATE" || echo "1")

if [ "$CERT_COUNT" -gt 1 ]; then
    # Extract intermediate certificates
    echo "$CERT_CHAIN" | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/ {print}' | \
    while IFS= read -r line; do
        if [[ "$line" == "-----BEGIN CERTIFICATE-----" ]]; then
            CERT_BUFFER="$line"
        elif [[ "$line" == "-----END CERTIFICATE-----" ]]; then
            CERT_BUFFER="$CERT_BUFFER
$line"
            PIN=$(echo "$CERT_BUFFER" | openssl x509 -pubkey -noout 2>/dev/null | \
                  openssl pkey -pubin -outform der 2>/dev/null | \
                  openssl dgst -sha256 -binary | \
                  base64)
            if [ ! -z "$PIN" ] && [ "$PIN" != "$SERVER_PIN" ]; then
                SUBJECT=$(echo "$CERT_BUFFER" | openssl x509 -noout -subject 2>/dev/null | sed 's/subject=//')
                echo -e "   sha256/${PIN}"
                echo -e "   ${BLUE}Subject: $SUBJECT${NC}"
                echo ""
            fi
            CERT_BUFFER=""
        else
            CERT_BUFFER="$CERT_BUFFER
$line"
        fi
    done
else
    echo -e "   ${YELLOW}⚠️  No intermediate CA found in chain${NC}"
    echo ""
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📝 Configuration Examples${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Android XML (network_security_config.xml):${NC}"
echo ""
cat <<EOF
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">$DOMAIN</domain>
    <pin-set expiration="2027-01-01">
        <pin digest="sha256">$SERVER_PIN</pin>
    </pin-set>
    <trust-anchors>
        <certificates src="system"/>
    </trust-anchors>
</domain-config>
EOF

echo ""
echo ""
echo -e "${YELLOW}Kotlin (CertificatePinningConfig.kt):${NC}"
echo ""
cat <<EOF
CertificatePinner.Builder()
    .add("$DOMAIN", "sha256/$SERVER_PIN")
    .build()
EOF

echo ""
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Certificate pins generated successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}⚠️  Important Reminders:${NC}"
echo "  • Always have at least 2 pins (current + backup)"
echo "  • Test in staging before deploying to production"
echo "  • Set up certificate expiration alerts"
echo "  • Update pins BEFORE certificate expires"
echo ""
echo -e "${BLUE}📚 Documentation:${NC}"
echo "  See CERTIFICATE_PINNING.md for detailed instructions"
echo ""
