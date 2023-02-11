#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>

#define SIZE 1024

void test_receive(int client_socket_descrp){
	// File used to store data
	FILE *file_pointer;
	// Filename being created to hold recieved data
	char *file_name = "/data/data/com.termux/files/home/test/recieved_file.txt";
	// Data container
	char data[SIZE];
	
	// Recieve data from server socket with recv func
	// "client_socket_descrp" new client socket
	// "container" will be where our data is temp stored
	// "SIZE" defined size of container storing server response data
	// "0" optional flags parameter default no flag
	int data_recieved = recv(client_socket_descrp, data, SIZE, 0);
	if(data_recieved <= 0){
		printf("!!! No Data Received !!!");
		close(client_socket_descrp);
		return;
	}
	// Data_recieved so create new file "recieved_file.txt"
	// "file_name" file name and type being created
	// "w" write new file
	file_pointer = fopen(file_name, "w");
	// Print data into "recieved_file.txt"
	// new file created to store data "recieved_file.txt"
	// container holding data
	fprintf(file_pointer, "%s", data);
	// Confirm Test Data Sent
	printf("+++ Data Sent Succesfully +++\n");

	return;
	
}


int main(){
	
	// IPv4 Address of server over local pc bat0 net interface
	char *server_ip_addr = "192.168.1.9";
	//Port number
	int port_num = 8080;
	
	// Create (Client) network socket function call with parameters...
	// "AF_INET" specify address domain / family (Internet),
	// "SOCK_STREAM" socket type - TCP (connection),
	// "0" default TCP protocol
	// return int socket descriptor
	int client_socket_descrp = socket(AF_INET, SOCK_STREAM, 0);
	// Test if socket created succesfully
	if(client_socket_descrp < 0) {
		printf("!!! Client Socket Not Created !!!");
		exit(1);
	}
	printf("+++ Client Socket Created +++\n");
	
		
	// Struct data for server socket - domain, address and port 
	struct sockaddr_in server_addr;
	// Specify address family / domain (Internet)
	server_addr.sin_family = AF_INET;
	// Server socket port data converted from int using htons func
	// htons func converts int port number into network byte order
	// !!!???!!! NOT USING
	server_addr.sin_port = port_num;
	// Server IPv4 address struct within struct with field s_addr
	// arpa/intet.inet_addr converts server char ip addr into byte order
	server_addr.sin_addr.s_addr = inet_addr(server_ip_addr);
	
	// connection func to connect client socket to server socket ...
	// "client_socket_descrp" client socket int descriptor
	// pointer to server_addr cast to struct sockaddr
	// size of the address using sizeof func for server_addr 
	// returns int to check connection 0 (ok) and -1 (error)
	int connected = connect(client_socket_descrp, 
								(struct sockaddr*)&server_addr, 
								sizeof(server_addr));
	// Check connection
	if(connected == -1) {
		printf("!!! Server Socket Connect Error !!!\n");
		exit(1);
	}
	printf("+++ Successful Connection to Server Socket+++\n");
	
	// Call my test_recieve func passing new client socket
	test_receive(client_socket_descrp);
	
	// Close client socket when finished
	printf("+++  Closing Connection +++\n");
	close(client_socket_descrp);
	
	return 0;
	
}
