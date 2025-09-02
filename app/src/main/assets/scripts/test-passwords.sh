#!/bin/sh

echo "=== Password and Sudo Test ==="
echo ""

echo "[*] Testing passwords:"
echo "Root password should be: server"
echo "Samsara password should be: server"
echo ""

echo "[*] Current user info:"
whoami
id

echo ""
echo "[*] Testing sudo access:"
echo "Trying: sudo -l"
sudo -l 2>/dev/null || echo "Sudo failed or requires password"

echo ""
echo "[*] Manual password test commands:"
echo "1. Test samsara password:"
echo "   su - samsara"
echo "   (enter password: server)"
echo ""
echo "2. Test sudo with password:"
echo "   sudo whoami"
echo "   (enter password: server)"
echo ""
echo "3. Test root password:"
echo "   su -"
echo "   (enter password: server)"
echo ""

echo "[*] Checking sudo configuration:"
if [ -f /etc/sudoers ]; then
  echo "Sudoers file exists, relevant lines:"
  grep -E "(wheel|samsara)" /etc/sudoers || echo "No wheel/samsara entries found"
else
  echo "No sudoers file found!"
fi

echo ""
echo "[*] Checking groups:"
echo "Samsara groups:"
groups samsara 2>/dev/null || echo "Cannot get samsara groups"

echo ""
echo "========================"
