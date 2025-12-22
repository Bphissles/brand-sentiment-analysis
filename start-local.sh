#!/bin/bash

# =============================================================================
# Peterbilt Sentiment Analyzer - Local Development Startup Script
# =============================================================================
# Starts all three services: Frontend, Backend, and ML Engine
# Usage: ./start-local.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$SCRIPT_DIR/.pids"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create PID directory
mkdir -p "$PID_DIR"

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}  Peterbilt Sentiment Analyzer - Starting   ${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# Load environment variables
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo -e "${GREEN}✓${NC} Loading environment variables..."
    export $(grep -v '^#' "$SCRIPT_DIR/.env" | xargs)
else
    echo -e "${RED}✗${NC} .env file not found. Please create one from .env.example"
    exit 1
fi

# Check for Java 17
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
if [ ! -d "$JAVA_HOME" ]; then
    echo -e "${RED}✗${NC} Java 17 not found at $JAVA_HOME"
    exit 1
fi

# =============================================================================
# Start ML Engine (Python Flask)
# =============================================================================
echo -e "${YELLOW}Starting ML Engine...${NC}"
cd "$SCRIPT_DIR/ml-engine"

if [ ! -d "venv" ]; then
    echo "  Creating Python virtual environment..."
    python3 -m venv venv
fi

source venv/bin/activate
pip install -q -r requirements.txt

# Start Flask in background
python app/api.py > "$SCRIPT_DIR/logs/ml-engine.log" 2>&1 &
ML_PID=$!
echo $ML_PID > "$PID_DIR/ml-engine.pid"
echo -e "${GREEN}✓${NC} ML Engine started (PID: $ML_PID) → http://localhost:5000"

deactivate
cd "$SCRIPT_DIR"

# =============================================================================
# Start Backend (Grails)
# =============================================================================
echo -e "${YELLOW}Starting Backend...${NC}"
cd "$SCRIPT_DIR/backend"

# Start Grails in background
./gradlew bootRun -q > "$SCRIPT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$PID_DIR/backend.pid"
echo -e "${GREEN}✓${NC} Backend starting (PID: $BACKEND_PID) → http://localhost:8080"

cd "$SCRIPT_DIR"

# =============================================================================
# Start Frontend (Nuxt)
# =============================================================================
echo -e "${YELLOW}Starting Frontend...${NC}"
cd "$SCRIPT_DIR/frontend"

# Start Nuxt in background
npm run dev > "$SCRIPT_DIR/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > "$PID_DIR/frontend.pid"
echo -e "${GREEN}✓${NC} Frontend started (PID: $FRONTEND_PID) → http://localhost:3000"

cd "$SCRIPT_DIR"

# =============================================================================
# Wait for services to be ready
# =============================================================================
echo ""
echo -e "${YELLOW}Waiting for services to be ready...${NC}"

# Wait for ML Engine
for i in {1..30}; do
    if curl -s http://localhost:5000/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} ML Engine is ready"
        break
    fi
    sleep 1
done

# Wait for Backend
for i in {1..60}; do
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Backend is ready"
        break
    fi
    sleep 1
done

# Wait for Frontend
for i in {1..30}; do
    if curl -s http://localhost:3000 > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Frontend is ready"
        break
    fi
    sleep 1
done

echo ""
echo -e "${BLUE}=============================================${NC}"
echo -e "${GREEN}  All services started successfully!${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""
echo "  Frontend:   http://localhost:3000"
echo "  Backend:    http://localhost:8080"
echo "  ML Engine:  http://localhost:5000"
echo ""
echo "  Logs:       ./logs/"
echo "  Stop:       ./stop-local.sh"
echo ""
