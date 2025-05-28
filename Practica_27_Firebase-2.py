import network
import urequests
import time
from machine import Pin, time_pulse_us, PWM, ADC
import dht

# Config WiFi
SSID = 'TECNM-D-Alumnos'
PASSWORD = ''
FIREBASE_URL = 'https://sistemas-programables-37c92-default-rtdb.firebaseio.com/sensores.json'

# Pines sensores
TRIG = Pin(14, Pin.OUT)
ECHO = Pin(27, Pin.IN)
dht_sensor = dht.DHT11(Pin(34))
ldr = ADC(Pin(35))          # Conecta tu LDR a GPIO35
ldr.atten(ADC.ATTN_11DB)    # Rango de 0 a 3.3V
ldr.width(ADC.WIDTH_10BIT) # Resolución de 10 bits (0-1023)

# Zumbador (PWM)
buzzer = PWM(Pin(12), freq=1000, duty=0)  # frecuencia genérica

def conectar_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        print("Conectando a WiFi...")
        wlan.connect(SSID, PASSWORD)
        while not wlan.isconnected():
            time.sleep(1)
            print(".", end="")
    print("\nConectado. IP:", wlan.ifconfig()[0])

def medir_distancia():
    TRIG.off()
    time.sleep_us(2)
    TRIG.on()
    time.sleep_us(10)
    TRIG.off()
    duracion = time_pulse_us(ECHO, 1, 30000)
    distancia_cm = duracion * 0.0343 / 2
    return round(distancia_cm, 2)

def leer_dht():
    try:
        dht_sensor.measure()
        temp = dht_sensor.temperature()
        hum = dht_sensor.humidity()
        return temp, hum
    except:
        return None, None

def leer_ldr():
    valor = ldr.read()  # Devuelve un valor entre 0 y 1023
    return valor


def enviar_a_firebase(temp, hum, distancia, luz):
    datos = {
        "temperatura": temp,
        "humedad": hum,
        "distancia": distancia,
        "luz": luz
    }
    try:
        respuesta = urequests.post(FIREBASE_URL, json=datos)
        print("Respuesta Firebase:", respuesta.text)
        respuesta.close()
    except Exception as e:
        print("Error al enviar:", e)

def sonar_con_distancia(distancia):
    # Ajustar intervalo de pitido según la distancia
    if distancia < 10:
        intervalo = 0.1
    elif distancia < 30:
        intervalo = 0.3
    elif distancia < 60:
        intervalo = 0.6
    else:
        intervalo = 1.0

    # Emitir un solo pitido
    buzzer.duty(512)  # encender
    time.sleep(0.05)  # duración del pitido
    buzzer.duty(0)    # apagar
    time.sleep(intervalo - 0.05)  # esperar hasta el siguiente ciclo

# ---- PROGRAMA PRINCIPAL ----
conectar_wifi()

while True:
    distancia = medir_distancia()
    temp, hum = leer_dht()
    luz = leer_ldr()
    
    print("Distancia:", distancia, "cm | Temp:", temp, "°C | Hum:", hum, "% | Luz:", luz)
    enviar_a_firebase(temp, hum, distancia, luz)
    sonar_con_distancia(distancia)

