# 2023 Gabrian Chua

import network
import socket
from time import sleep
from machine import Pin, PWM
from mfrc522 import MFRC522
import utime
import _thread

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
frameDelay = 0.05 # 20 fps
rechargeFrames = 100 # 5 seconds at 20fps
frames = 0

# vars

numCandies = 0;

# functs

def updateLeds():
    global numCandies
    global leds
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

def setCandies(amt):
    global numCandies
    amt = max(0, amt)
    amt = min(3, amt)
    numCandies = amt
    print(f'Set numCandies to {amt}')

def dispense():
    global numCandies
    global servo
    global SERVO_MAX
    global SERVO_MIN
    if (numCandies > 0):
        print('Dispensing candy...')
        numCandies -= 1
        updateLeds()
        servo.duty_ns(SERVO_MAX)
        sleep(2)
        servo.duty_ns(SERVO_MIN)
        sleep(2)
    else:
        print('Cannot dispense candy, none left')

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
    global numCandies
    print(f'GET: {request}')
    html = '{"status": "404"}'
    if (request == '/rld'):
        html = '{"status": "alive"}'
    elif (request == f'/{pin}'):
        html = '{"status": ' + str(numCandies) + '}'
    elif (request == f'/{pin}?disp'):
        print('Force dispensed candy')
        dispense()
        html = '{"status": "success"}'
    elif (f'/{pin}?set=' in request):
        amt = int(request[len(f'/{pin}?set='):])
        setCandies(amt)
        updateLeds()
        html = '{"status": "success"}'
    return str(html)

# HTTP LOOP: thread 0

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
        print("HTTP ready for more requests")

# MAIN LOOP: thread 1

def loop():
    global btn
    global frameDelay
    global numCandies
    global frames
    global rechargeFrames
    while True:
        frames += 1
        if (frames > rechargeFrames):
            print("Candies recharged from time")
            frames = 0
            setCandies(numCandies + 1)
            updateLeds()
            
        if (btn.value()):
            print("Button pressed!")
            dispense()
            
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
    print("Initializing...")
    ip = connect()
    connection = open_socket(ip)
    _thread.start_new_thread(loop, ()) # enter main loop on thread 1
    serve(connection) # enter HTTP loop on thread 0
except KeyboardInterrupt:
    machine.reset()