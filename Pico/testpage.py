import network
import socket
from time import sleep
from picozero import pico_led
import machine

ssid = 'CGNET'
password = 'helloworld123'
pin = '1234'

numCandies = 0;

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

def serve(connection):
    #Start a web server
    pico_led.off()
    while True:
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

try:
    ip = connect()
    connection = open_socket(ip)
    serve(connection)
except KeyboardInterrupt:
    machine.reset()