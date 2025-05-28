# ProVision - Sistema IoT de Monitoreo y Traducción Multilenguaje

## 📱 Aplicación Android: ProVision

**ProVision** es una app Android todo en uno que permite:

- 📡 Controlar dispositivos IoT mediante **WiFi** y **Bluetooth**.
- 📷 Capturar imágenes desde la **cámara** del dispositivo o una **IP Webcam**.
- 🔍 Traducir texto en tiempo real usando la cámara (OCR) o IP webcam.
- 🌐 Traducciones disponibles: **Español**, **Inglés**, **Francés**, y **Alemán**.
- 🔊 Reproducir traducciones usando **text-to-speech (TTS)**.
- ☁️ Enviar y recibir datos desde **Firebase Realtime Database**.
- 🕓 Mantener un historial de traducciones y actividades.

### Tecnologías Usadas

- Google Translate API
- Google Text-to-Speech API
- Firebase Realtime Database
- Android Camera2 API / IP WebCam Server
- OCR (Reconocimiento Óptico de Caracteres)
- Bluetooth & WiFi Manager

---

## 🤖 Proyecto IoT con ESP32 + Firebase

Este proyecto permite la **recolección de datos ambientales** usando un ESP32 y varios sensores, los cuales son enviados a Firebase y pueden ser controlados vía una interfaz web o desde ProVision.

### 🔧 Hardware Utilizado

- ESP32
- Sensor ultrasónico HC-SR04 (distancia)
- Sensor DHT11 (temperatura y humedad)
- LDR (sensor de luz)
- Zumbador (buzzer)

### 📡 Funciones

- Conexión WiFi automática al inicio.
- Lectura periódica de:
  - Temperatura y humedad (DHT11)
  - Distancia (HC-SR04)
  - Luz (LDR)
- Envío de datos a **Firebase** en tiempo real.
- Servidor HTTP embebido para activar o desactivar el zumbador remotamente.
- Interacción desde la app ProVision o navegador web.

### 🌐 API Web (HTTP)

| Método | Ruta         | Acción                  |
|--------|--------------|-------------------------|
| GET    | `/activar`   | Activa el zumbador      |
| GET    | `/desactivar`| Desactiva el zumbador   |

---

## 🔗 Integración IoT + App

La app **ProVision** permite:

- Visualizar los datos del ESP32 obtenidos desde Firebase.
- Activar o desactivar el buzzer vía WiFi.
- Traducir texto capturado desde la cámara del celular o IP webcam.
- Almacenar y reproducir traducciones mediante TTS.
- Usar Bluetooth para comandos adicionales en futuras versiones.

---

## ⚙️ Instalación

### ESP32 (MicroPython)

1. Flashea tu ESP32 con MicroPython.
2. Sube el archivo `main.py` al ESP32 usando Thonny o ampy.
3. Asegúrate de editar las credenciales WiFi y URL de Firebase dentro del código:
   ```python
   SSID = 'TuSSID'
   PASSWORD = 'TuClaveWiFi'
   FIREBASE_URL = 'https://...firebaseio.com/sensores.json'
