#include <stdio.h>
#include <stdlib.h>
#include <winsock.h>
#include <string.h>
#include <unistd.h>

int main() {
    int sockHandle;
    int connectionHandle;
    char buffer[80];
    struct sockaddr_in serverAddress;
    struct sockaddr_in client;
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(8080);
    serverAddress.sin_addr.s_addr = htonl(INADDR_ANY);

    // Currently seems like another method may be necessary since this relies on working on a different layer of the OSI model
    sockHandle = socket(AF_INET, SOCK_DGRAM, 0);
    // Might be able to change number based on protocol being used (aka batman)  
    if (sockHandle < 0) return -1;
    int saSize = sizeof(serverAddress);
    bzero(&serverAddress, saSize);
    if ((bind(sockHandle, (struct sockaddr*) &serverAddress, saSize)) != 0) return -1;
    
    if (listen(sockHandle, 3) != 0) return -1;

    int clientSize = sizeof(client);
    connectionHandle = accept(sockHandle, (struct sockaddr*) &clientSize);
    if (connectionHandle < 0) return -1;
    
    int doEndConnection = 0;
    while(!doEndConnection) {
        
    }

    close(sockHandle);
}