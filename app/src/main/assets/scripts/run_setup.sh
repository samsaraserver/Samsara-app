#!/bin/sh
set -e

echo ""
echo "SamsaraServer Alpine Linux Setup"
echo "===================================="
echo "Based on proven working configuration"
echo ""

# Step 1: User and system setup
echo "STEP 1: Setting up user and packages..."
echo "======================================="
/bin/sh ./setup_user.sh

echo ""
echo "STEP 2: Setting up SSH server..."  
echo "==============================="
/bin/sh ./setup_ssh.sh

echo ""
echo "SETUP COMPLETE!"
echo "=================="
echo ""
echo "Your SamsaraServer is ready to use!"
echo ""
echo "Connection Details:"
echo "   • Username: samsara (or root)"
echo "   • Password: server"
echo "   • Port: 2222"
echo ""
echo "From another device on same WiFi:"
echo "   ssh samsara@<phone-ip> -p 2222"
echo ""
echo "To find your phone's IP:"
echo "   • Check WiFi settings on your phone"
echo "   • Or run: ip addr show"
echo ""
echo "[!] Note: This script does not exit the proot session. The calling launcher will keep an interactive shell open so any daemons started (sshd) will remain running while this session is active." 
echo "If you want to stop here and exit, run: exit" 