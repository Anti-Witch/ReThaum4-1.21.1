#!/usr/bin/env bash
# Запускає Minecraft-клієнт (runClient) у віртуальному дисплеї Xvfb
# і піднімає VNC + noVNC, щоб дивитися/грати через браузер:
#
#   1) відкрий вкладку Ports у VS Code / Codespaces
#   2) знайди порт 6080 (noVNC) -> Open in Browser
#   3) у вікні noVNC натисни Connect (пароля немає)
#
# Продуктивність: llvmpipe (софтверний OpenGL) — повільно, але гра запускається.
# Зупинити: bash scripts/game-vnc.sh stop
#
# Використання:
#   scripts/game-vnc.sh        # запустити
#   scripts/game-vnc.sh stop   # зупинити все

set -u

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DISPLAY_NUM=:99
SCREEN=1280x720x24
VPORT=5900
NPORT=6080
JAVA_HOME_LINUX="${JAVA_HOME:-/usr/local/sdkman/candidates/java/21.0.10-ms}"

stop_all() {
  echo "[game-vnc] зупиняю..."
  pkill -f "websockify.*${NPORT}" 2>/dev/null
  pkill -f "x11vnc.*${DISPLAY_NUM}" 2>/dev/null
  pkill -f "Xvfb ${DISPLAY_NUM}" 2>/dev/null
  pkill -f "devlaunch.Main" 2>/dev/null
  pkill -f "gradlew runClient" 2>/dev/null
  sleep 1
  echo "[game-vnc] готово"
}

if [ "${1:-}" = "stop" ]; then
  stop_all
  exit 0
fi

command -v Xvfb >/dev/null || { echo "Немає Xvfb: sudo apt-get install -y xvfb"; exit 1; }
command -v x11vnc >/dev/null || { echo "Немає x11vnc: sudo apt-get install -y x11vnc novnc websockify"; exit 1; }

# чиста зупинка перед стартом
stop_all >/dev/null 2>&1 || true

echo "[game-vnc] Xvfb ${DISPLAY_NUM} (${SCREEN})..."
Xvfb "${DISPLAY_NUM}" -screen 0 "${SCREEN}" >/tmp/opencode/xvfb.log 2>&1 &
sleep 2

# гарантована розкладка для віртуального дисплея (клавіші не "залипають")
command -v setxkbmap >/dev/null && setxkbmap -display "${DISPLAY_NUM}" us 2>/dev/null || true

echo "[game-vnc] запуск Minecraft (це триватиме кілька хвилин на llvmpipe)..."
cd "${REPO_DIR}"
DISPLAY="${DISPLAY_NUM}" JAVA_HOME="${JAVA_HOME_LINUX}" \
  nohup ./gradlew runClient --console=plain >/tmp/opencode/runclient.log 2>&1 &

echo "[game-vnc] x11vnc (порт ${VPORT})..."
# -xkb       : коректна обробка розкладки та модифікаторів (Shift/Ctrl/Alt)
# -repeat    : автоповтор клавіш (інакше рух липне)
# -nolookup  : не робити reverse-DNS (швидший старт)
# -shared    : можна перепідключатися без розриву сесії
x11vnc -display "${DISPLAY_NUM}" -nopw -forever -shared -xkb -repeat -nolookup \
  -rfbport "${VPORT}" >/tmp/opencode/x11vnc.log 2>&1 &
sleep 2

echo "[game-vnc] noVNC (порт ${NPORT})..."
websockify --web=/usr/share/novnc "${NPORT}" localhost:"${VPORT}" \
  >/tmp/opencode/websockify.log 2>&1 &
sleep 2

echo
echo "=============================================================="
echo "  Відкрий у браузері Forwarded Port -> ${NPORT} (noVNC)"
echo "  Пряме посилання:  http://localhost:${NPORT}/vnc.html?autoconnect=1&resize=scale"
echo "  Далі Connect -> побачиш екран Minecraft."
echo
echo "  ВАЖЛИВО для керування:"
echo "   1) клацни мишею по чорному екрану (це дає фокус клавіатурі)"
echo "   2) натисни Full Screen (кнопка в лівому верхньому куті noVNC)"
echo "   3) якщо лаги вводу - краще підключись будь-яким VNC-клієнтом"
echo "      (TigerVNC/RealVNC) на localhost:${VPORT} через VS Code Ports"
echo "  Логи: /tmp/opencode/runclient.log"
echo "  Зупинити: bash scripts/game-vnc.sh stop"
echo "=============================================================="
