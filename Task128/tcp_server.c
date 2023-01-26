#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

//#include <sys/types.h>
//#include <sys/socket.h>
//#include <netinet/in.h>

#include <arpa/inet.h>

#define SIZE 1024

void test_send(int client_socket_descriptor, FILE *file_pointer){
	// Data container
	char data[SIZE] = {0};
	// Get data from file "send_file.txt"
	fgets(data, SIZE, file_pointer);
	// send func sends data through server socket to client socket
	// "client_socket_descriptor" client socket to send data too
	// "data" the data container
	// size of the data being sent
	int data_sent = send(client_socket_descriptor,data,sizeof(data),0);
	// Check if data send successfully (-1) unsuccessfully
	if(data_sent == -1){
		printf("!!! Error Sending Data !!!");
		return;
	}
	printf("+++ Data Send Successfully +++\n");
	
}


int main(){
		
	// IPv4 Address of server over local pc bat0 net interface
	char *server_ip_addr = "192.168.1.9";
	int port_num = 8080;
	
	// File used to send data
	FILE *file_pointer;
	// Filename being sent with data to cleint socket
	char *file_name = "send_file.txt";
	file_pointer = fopen(file_name, "r");
	if(file_pointer == NULL){
		printf("!!! Error Reading File !!!");
		exit(1);
	}
	
	// Create (Server) network socket function with parameters... 
	// "AF_INET" specifies address domain / family (Internet),
	// "SOCK_STREAM" socket type - TCP (connection),
	// "0" default TCP protocol
	// return int server socket descriptor
	int server_socket_descrp = socket(AF_INET, SOCK_STREAM, 0);
	// Test if socket created successfully
	if(server_socket_descrp < 0) {
		printf("!!! Server Socket Not Created !!!");
		exit(1);
	}
	printf("+++ Server Socket Created +++\n");
	
	// Struct data for client socket - domain, address and port
	struct sockaddr_in server_addr;
	// Specify address family / domain (Internet)
	server_addr.sin_family = AF_INET;
	// Server socket port data 
	server_addr.sin_port = port_num;
	// Server IPv4 address struct within struct with field s_addr
	// arpa/inet.inet_addr converts server char ip addr into byte order
	server_addr.sin_addr.s_addr = inet_addr(server_ip_addr);
	
	// bind func to bind server socket to ip address and port
	// "server_socket_descrp" server socket int descriptor
	// pointer to server_addr cast to struct sockaddr
	// size of the address using sizeof func for server_addr
	// returns int to check bind 0 (ok) and -1 (error)
	int bound = bind(server_socket_descrp, 
					 (struct sockaddr*)&server_addr, 
					 sizeof(server_addr));
	// Check bind			 
	if(bound  == -1){
		printf("!!! Server Socket Bind Error !!!\n");
		exit(1);
	}
	printf("++ Succesful Bind for Server Socket +++\n");
	
	// listen func listens for server socket connections
	// "server_socket_descrp" server socket being listened too
	// "10" backlog - how many connections can be waiting for server
	// for this test we are only waiting on one connection
	// return int 0 (connection found)
	if(listen(server_socket_descrp, 1) == 0){
		 printf("+++ Server is Listening... +++\n");
	}else{
		printf("[!!! Listening Error !!!");
		exit(1);
	}
	
	
	// To accept connection send data back to client socket
	int client_socket_descrp;
	// accept func accept connection and return client socket
	// this allows us to manipulate the client socket
	// two way connection between client and server to send / receive
	// "server_socket_descrp" server socket that is accepting connection
	// assign client socket address to NULL
	// assign client socket size to NULL
	// return int client socket descriptor
	client_socket_descrp = accept(server_socket_descrp, NULL, NULL);
	// Call my test_send func passing client socket
	test_send(client_socket_descrp, file_pointer);

	// Close server socket
	printf("+++ Closing Connection +++\n");
	close(server_socket_descrp);
	
	
	return 0;
}
