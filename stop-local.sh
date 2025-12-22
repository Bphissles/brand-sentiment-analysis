#!/bin/bash

# =============================================================================
# Peterbilt Sentiment Analyzer - Local Development Shutdown Script
# =============================================================================
# Gracefully stops all three services: Frontend, Backend, and ML Engine
# Usage: ./stop-local.sh
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$SCRIPT_DIR/.pids"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}  Peterbilt Sentiment Analyzer - Stopping   ${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# Function to stop a service gracefully
stop_service() {
    local name=$1
    local pid_file="$PID_DIR/$name.pid"
    local port=$2
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${YELLOW}Stopping $name (PID: $pid)...${NC}"
            kill $pid 2>/dev/null
            
            # Wait for graceful shutdown (up to 10 seconds)
            for i in {1..10}; do
                if ! ps -p $pid > /dev/null 2>&1; then
                    echo -e "${GREEN}✓${NC} $name stopped gracefully"
                    rm -f "$pid_file"
                    return 0
                fi
                sleep 1
            done
            
            # Force kill if still running
            echo -e "${YELLOW}  Force killing $name...${NC}"
            kill -9 $pid 2>/dev/null
            echo -e "${GREEN}✓${NC} $name stopped (forced)"
            rm -f "$pid_file"
        else
            echo -e "${YELLOW}⚠${NC} $name PID file exists but process not running"
            rm -f "$pid_file"
        fi
    else
        echo -e "${YELLOW}⚠${NC} No PID file for $name"
    fi
    
    # Also kill by port as fallback
    if [ -n "$port" ]; then
        local port_pid=$(lsof -ti:$port 2>/dev/null)
        if [ -n "$port_pid" ]; then
            echo -e "${YELLOW}  Killing process on port $port...${NC}"
            kill $port_pid 2>/dev/null
            sleep 1
            # Force kill if needed
            port_pid=$(lsof -ti:$port 2>/dev/null)
            if [ -n "$port_pid" ]; then
                kill -9 $port_pid 2>/dev/null
            fi
            echo -e "${GREEN}✓${NC} Port $port cleared"
        fi
    fi
}

# Stop services in reverse order
stop_service "frontend" 3000
stop_service "backend" 8080
stop_service "ml-engine" 5000

# Kill any remaining Gradle daemons for this project
echo -e "${YELLOW}Stopping Gradle daemons...${NC}"
cd "$SCRIPT_DIR/backend" 2>/dev/null && ./gradlew --stop > /dev/null 2>&1
echo -e "${GREEN}✓${NC} Gradle daemons stopped"

echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${GREEN}  All services stopped${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
