# Тестовий прогін — як перевіряти гру в цьому середовищі (PC-only)

Середовище: Linux (Codespace), 4 ядра, 15 GiB RAM, GPU немає.
Обидва способи перевірені й працюють.

## 1) Dedicated server smoke test (швидкий, ~1 хв)
```bash
cd Thaumcraft-4-Source-Code-1.21.1
JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.10-ms timeout 300 ./gradlew runServer --console=plain
```
Перевіряє: завантаження модів, реєстри, датапаки (рецепти/дослідження/світоген),
перший запуск світу. Дивитись `run/logs/latest.log` на `ERROR` та `Parsing error`.
Для gametest-варіанта (`runGameTestServer`) потрібен хоча б один `@GameTest`
(поки їх немає — сервер падає з "No test functions were given!").

## 2) Клієнт зі софтверним OpenGL (Xvfb + Mesa llvmpipe) — працює!
Пакети (вже встановлені): `xvfb mesa-utils libgl1-mesa-dri libglx-mesa0 libegl1
libgl1 libxi6 libxcursor1 libxrandr2 libxinerama1 libxxf86vm1 libglfw3`.

```bash
JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.10-ms \
timeout 480 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient --console=plain
```
- Клієнт стартує за ~4.5 хв і доходить до title screen (перевірено через jstack:
  Render thread у `glDrawElements`).
- Звук вимкнеться сам: `Failed to open OpenAL device` — очікувано без аудіопристрою.
- `libflite.so` (text-to-speech) відсутній — нешкідливо.
- Корисні перевірки в `run/logs/latest.log`:
  - `ERROR` / `Exception` (крім OpenAL/flite),
  - `Missing textures in model ...` (моделі без текстур),
  - `Parsing error loading recipe ...` (биті рецепти).

## 3) Скріншоти (за потреби)
Xvfb-дисплей можна знімати через `xwd`/`xwdtopnm` (не встановлені;
`sudo apt-get install x11-apps netpbm`), поки клієнт запущений під `xvfb-run`.
Для автоматичного входу в гру/відкриття GUI потрібен `xdotool` — можливо,
але це вже не обов'язково для перевірки запуску.
