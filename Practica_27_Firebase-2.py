# Importación de módulos para manejar red, hardware, sensores, multihilos y HTTP
import network              # Conexión WiFi
import socket               # Comunicación mediante sockets
import time                 # Funciones de espera
from machine import Pin, PWM, ADC, time_pulse_us  # Acceso a hardware (GPIO, PWM, ADC)
import dht                  # Soporte para sensor DHT11
import _thread              # Soporte para hilos
import urequests            # Librería para hacer peticiones HTTP (como POST)

# --- Configuración de red WiFi y Firebase ---
SSID = 'POCO X5 Pro 5G'     # Nombre de red WiFi
PASSWORD = '135792468'      # Contraseña del WiFi
FIREBASE_URL = 'https://sistemas-programables-37c92-default-rtdb.firebaseio.com/sensores.json'  # URL de Firebase

# --- Inicialización de sensores ---
TRIG = Pin(14, Pin.OUT)     # Pin para disparar el sensor ultrasónico
ECHO = Pin(27, Pin.IN)      # Pin para recibir eco del sensor ultrasónico
dht_sensor = dht.DHT11(Pin(4))  # Sensor DHT11 conectado al pin digital 4

ldr = ADC(Pin(35))          # Sensor de luz (LDR) en pin 35 (analógico)
ldr.atten(ADC.ATTN_11DB)    # Configura el rango de entrada a 0-3.6V
ldr.width(ADC.WIDTH_10BIT) # Configura la resolución del ADC a 10 bits (0-1023)

# --- Zumbador (buzzer) ---
buzzer = PWM(Pin(12), freq=1000, duty=0)  # Inicializa PWM en el pin 12, frecuencia 1kHz

# Variable global que indica si el buzzer está activado
sonar_habilitado = False

# --- Conexión a WiFi ---
def conectar_wifi():
    wlan = network.WLAN(network.STA_IF)  # Modo estación (cliente)
    wlan.active(True)                    # Activar interfaz WiFi
    if not wlan.isconnected():          # Si no está conectado aún
        print("Conectando a WiFi...")
        wlan.connect(SSID, PASSWORD)    # Intentar conectar con credenciales
        while not wlan.isconnected():   # Esperar hasta que se conecte
            time.sleep(1)
            print(".", end="")          # Imprimir punto mientras espera
    print("\nConectado. IP:", wlan.ifconfig()[0])  # Mostrar IP obtenida

# --- Medición de distancia con sensor ultrasónico ---
def medir_distancia():
    TRIG.off()
    time.sleep_us(2)                    # Pausa breve
    TRIG.on()
    time.sleep_us(10)                   # Pulso de 10us para iniciar medición
    TRIG.off()
    duracion = time_pulse_us(ECHO, 1, 30000)  # Tiempo hasta que el eco regrese (máx 30ms)
    distancia_cm = duracion * 0.0343 / 2      # Convertir tiempo a distancia (cm)
    return round(distancia_cm, 2)             # Redondear a 2 decimales

# --- Lectura del sensor DHT11 (temperatura y humedad) ---
def leer_dht():
    try:
        dht_sensor.measure()                # Iniciar medición
        temp = dht_sensor.temperature()     # Obtener temperatura
        hum = dht_sensor.humidity()         # Obtener humedad
        return temp, hum
    except Exception as e:
        print("Error al leer DHT11:", e)
        return None, None                   # Si hay error, retornar None

# --- Lectura del sensor de luz (LDR) ---
def leer_ldr():
    return ldr.read()  # Leer valor analógico (0-1023)

# --- Envío de datos a Firebase ---
def enviar_a_firebase(temp, hum, distancia, luz):
    datos = {
        "temperatura": temp,
        "humedad": hum,
        "distancia": distancia,
        "luz": luz
    }
    try:
        respuesta = urequests.post(FIREBASE_URL, json=datos)  # Enviar POST
        print("Respuesta Firebase:", respuesta.text)
        respuesta.close()
    except Exception as e:
        print("Error al enviar:", e)

# --- Control del zumbador ---
def sonar_buzzer(encender):
    if encender:
        buzzer.duty(512)  # Encender (50% ciclo de trabajo)
    else:
        buzzer.duty(0)    # Apagar

# --- Servidor HTTP para activar/desactivar zumbador ---
def iniciar_servidor():
    global sonar_habilitado
    addr = socket.getaddrinfo('0.0.0.0', 80)[0][-1]  # Obtener dirección local
    s = socket.socket()
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # Reusar dirección
    s.bind(addr)      # Asignar IP y puerto
    s.listen(1)       # Escuchar 1 conexión a la vez
    print('Servidor HTTP iniciado en', addr)

    while True:
        cl, addr = s.accept()       # Aceptar nueva conexión
        print('Cliente conectado desde', addr)
        request = cl.recv(1024)     # Leer datos del cliente
        request_str = request.decode('utf-8')  # Decodificar texto
        print("Request:", request_str)

        if 'GET /activar' in request_str:
            sonar_habilitado = True
            sonar_buzzer(True)
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nZumbador activado"
        elif 'GET /desactivar' in request_str:
            sonar_habilitado = False
            sonar_buzzer(False)
            response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nZumbador desactivado"
        else:
            response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nRuta no encontrada"

        cl.send(response)  # Enviar respuesta HTTP
        cl.close()         # Cerrar conexión

# --- Programa principal ---

conectar_wifi()  # Conectar al WiFi

# Iniciar servidor web en un hilo aparte (para no bloquear el loop principal)
_thread.start_new_thread(iniciar_servidor, ())

# Bucle principal para lectura de sensores y envío de datos
while True:
    distancia = medir_distancia()             # Leer distancia
    temp, hum = leer_dht()                    # Leer temperatura y humedad
    luz = leer_ldr()                          # Leer valor del LDR

    # Mostrar resultados en consola
    print("Distancia:", distancia, "cm | Temp:", temp, "°C | Hum:", hum, "% | Luz:", luz)

    # Enviar datos a Firebase solo si el sensor DHT11 respondió correctamente
    if temp is not None and hum is not None:
        enviar_a_firebase(temp, hum, distancia, luz)
    else:
        print("Saltando envío a Firebase por fallo en sensor DHT")

    # Control del buzzer si está habilitado
    if sonar_habilitado:
        buzzer.duty(512)   # Emitir sonido breve
        time.sleep(0.05)
        buzzer.duty(0)
        time.sleep(0.3)
    else:
        buzzer.duty(0)     # Asegurarse de que esté apagado
        time.sleep(1)
