#include <stdio.h>
#include <stdlib.h>
#include <winsock.h>
#include <string.h>
#include <unistd.h>

// Code reference: https://www.geeksforgeeks.org/socket-programming-cc/

struct LocationQuery {
    QueryType type;
    char[10] subtype;
    char[10] location;
};

enum QueryType {request, response}

int main() {
    char host[21];
    int port;
    char targetHost[21];
    int targetPort;
    int hostname;
    int sock = 0;
    int clientHandle;
    int val;
    struct sockaddr_in sAddr;

    printf("Starting Client\n");

    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0) {
        printf("Could not create socket");
        return -1;
    }

    // hostname = gethostname(host, sizeof(host)); // Why isn't this working!!!
    // if (hostname = -1) {
    //     printf("Could not retrieve hostname");
    //     return -1;
    // }
    // printf("%d", hostname); 
    // Get the current host this is running on. This will be useful for sending to other clients.
    // printf("localhost's ip is: %s\n", host);

    // input the port this should run on
    int portIsValid = 1;
    do {
        printf("\nPlease input port to run on: ");
        scanf("%d", &port);
        if (port > 9999 || port < 0) {
            printf("Port is invalid");
            portIsValid = 0;
        } else {
            portIsValid = 1;
        }
    } while (!portIsValid);
    printf("The given port is: %d\n", port);

    // Input the port to connect to
    int targetPortIsValid = 1;
    do {
        printf("\nPlease input port to connect to: ");
        scanf("%d", &targetPort);
        if (targetPort > 9999 || targetPort < 0) {
            printf("Port is invalid");
            targetPortIsValid = 0;
        } else {
            targetPortIsValid = 1;
        }
    } while (!targetPortIsValid);
    printf("The given port is: %d\n", targetPort);

    sAddr.sin_family = AF_INET;
    sAddr.sin_port = htons(targetPort);

    int targetHostIsValid = 0;
    do {
        printf("Please input host to connect to: ");
        scanf("%s", targetHost);
        printf("\nThe given host is (20 character max): %s\n", targetHost);
        targetHostIsValid = 0;
        for (int i = (sizeof(targetHost)/sizeof(char)) - 1; i >= 0; i--) {
            if (host[i] == '\0') targetHostIsValid = 1;
        }
    } while (!targetHostIsValid);

    char nodeLocation[10];
    printf("Please input the location name for this node");
    scanf("%s", &nodeLocation);
    
    clientHandle = connect(sock, (struct sockaddr*)&sAddr, sizeof(sAddr));
    if (clientHandle < 0) {
        printf("Could not connect to provided port and host");
    }
    printf("The given host is: %s\n", targetHost);

    close(clientHandle);
}