#!/bin/bash

# Generate RSA key pair for JWT signing
# This script generates a 2048-bit RSA key pair and outputs:
# 1. Private key (jwt-private.pem)
# 2. Public key (jwt-public.pem)
# 3. Base64 encoded keys for Spring Boot configuration

set -e

echo "Generating RSA key pair for JWT signing..."

# Generate private key
openssl genrsa -out jwt-private.pem 2048

# Extract public key
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

echo ""
echo "==============================================="
echo "JWT RSA Key Pair Generated Successfully!"
echo "==============================================="
echo ""
echo "Private Key (jwt-private.pem):"
cat jwt-private.pem
echo ""
echo "Public Key (jwt-public.pem):"
cat jwt-public.pem
echo ""
echo "==============================================="
echo "Base64 Encoded Keys for Spring Boot Config:"
echo "==============================================="
echo ""
echo "Private Key (Base64 - for application.yml):"
base64 -i jwt-private.pem | tr -d '\n'
echo ""
echo ""
echo "Public Key (Base64 - for application.yml):"
base64 -i jwt-public.pem | tr -d '\n'
echo ""
echo ""
echo "==============================================="
echo "Add these to application.yml:"
echo "==============================================="
echo "security:"
echo "  jwt:"
echo "    algorithm: RS256"
echo "    private-key: |"
cat jwt-private.pem | sed 's/^/      /'
echo "    public-key: |"
cat jwt-public.pem | sed 's/^/      /'
echo ""
echo "IMPORTANT: Store private key in Vault for production!"
echo "==============================================="
