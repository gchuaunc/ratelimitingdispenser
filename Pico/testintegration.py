# 2023 Gabrian Chua

import network
import socket
from time import sleep
from machine import Pin, PWM
from mfrc522 import MFRC522
import utime

# consts

ledPins = [0, 1, 2]
leds = []
btnPin = 3
btn = None
SERVO_MIN = 500000
SERVO_MAX = 2500000
servoPin = 4
servo = None
ssid = 'CGNET'
password = 'helloworld123'
pin = '1234'
frameDelay = 50 # 20 fps

# vars

numCandies = 0;

# functs

def updateLeds():
    if numCandies == 0:
        leds[0].off()
        leds[1].off()
        leds[2].off()
    elif numCandies == 1:
        leds[0].on()
        leds[1].off()
        leds[2].off()
    elif numCandies == 2:
        leds[0].on()
        leds[1].on()
        leds[2].off()
    elif numCandies == 3:
        leds[0].on()
        leds[1].on()
        leds[2].on()
    else:
        print("ERROR with numcandies, resetting to 0")
        print(f"Old numCandies = {numCandies}")
        numCandies = 0
        updateLeds()



def connect():
    #Connect to WLAN
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    wlan.connect(ssid, password)
    while wlan.isconnected() == False:
        print('Waiting for connection...')
        sleep(1)
    ip = wlan.ifconfig()[0]
    print(f'Connected to {ssid} on {ip}')
    return ip

def open_socket(ip):
    # Open a socket
    port = 80
    address = (ip, port)
    connection = socket.socket()
    connection.bind(address)
    connection.listen(1)
    print(f'Socket opened on port {port}');
    return connection

def get(request):
    global numCandies;
    print(f'GET: {request}')
    html = '{"status": "404"}'
    if (request == '/rld'):
        html = '{"status": "alive"}'
    elif (request == f'/{pin}'):
        html = '{"status": ' + str(numCandies) + '}'
    elif (request == f'/{pin}?disp'):
        print('Force dispensed candy')
        html = '{"status": "success"}'
    elif (f'/{pin}?set=' in request):
        amt = int(request[len(f'/{pin}?set='):])
        amt = max(0, amt)
        amt = min(3, amt)
        print(f'Set amount to {amt}')
        html = '{"status": "success"}'
        numCandies = amt
    return str(html)

# MAIN LOOP

def serve(connection):
    while True:
        # HTTP web server
        client = connection.accept()[0]
        request = client.recv(1024)
        request = str(request)
        try:
            request = request.split()[1]
        except IndexError:
            pass
        html = get(request)
        client.send(html)
        client.close()
        
        print("ready for other stuff")
        
        sleep(frameDelay)

# set up pins

leds.append(Pin(ledPins[0], Pin.OUT))
leds.append(Pin(ledPins[1], Pin.OUT))
leds.append(Pin(ledPins[2], Pin.OUT))
btn = Pin(btnPin, Pin.IN, Pin.PULL_DOWN)
servo = PWM(Pin(servoPin))
servo.freq(50)
servo.duty_ns(SERVO_MIN)

# begin loop

try:
    print("Hello, world!")
    ip = connect()
    connection = open_socket(ip)
    serve(connection) # enters main loop
except KeyboardInterrupt:
    machine.reset()