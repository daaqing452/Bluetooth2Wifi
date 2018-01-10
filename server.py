# -*- coding:utf-8 -*-
import socket
import socketserver
import threading

PORT = 11121
ip = socket.gethostbyname(socket.gethostname())
print('[     ip     ]:', ip)

'''
# socketserver
class MyServer(socketserver.BaseRequestHandler):
    def handle(self):
        client = self.request
        print('new connection: ', self.client_address)
        client.sendall(bytes('你好\n', encoding='utf-8'))
        while True:
            buf_bytes = client.recv(1024)
            print(len(buf_bytes))
            print(buf)
            buf = str(buf_bytes, encoding='utf-8')
            if buf == 'q':
                break
server = socketserver.ThreadingTCPServer((ip, PORT), MyServer)
server.serve_forever()
'''

server = socket.socket()
server.bind((ip, PORT))
server.listen(5)
clients = []

def hysend(client, s):
	client.sendall(bytes(s + '\n', encoding='utf-8'))

def recv_thread(client, address):
	while True:
		b = client.recv(1024)
		if len(b) <= 0:
			clients.remove(client)
			print('[disconnected]: %s:%d (%d)' % (address[0], address[1], len(clients)))
			break
		s = str(b, encoding='utf-8')
		print('[%s:%d]: %s' % (address[0], address[1], s))

def listen_thread():
	global clients
	print('[ listening  ] ...')
	while True:
		client, address = server.accept()
		clients.append(client)
		print('[ connected  ]: %s:%d (%d)' % (address[0], address[1], len(clients)))
		t = threading.Thread(target=recv_thread, args=[client, address])
		t.start()
		hysend(client, "服务器端发送!")

t = threading.Thread(target=listen_thread)
t.setDaemon(True)
t.start()

while True:
	s = input()
	print('[ broadcast  ] %d clients: "%s"' % (len(clients), s))
	for client in clients:
		hysend(client, s)