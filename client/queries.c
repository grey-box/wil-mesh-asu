//Nicholas Ellender
//12/2/2022
#include <stdio.h>
#include <time.h>
#include <string.h>

char deviceId[8];
char wikipedia[25] = "https://www.wikipedia.com";

//method to set the deviceID on this class to then refer to when needed
char *setDeviceID(char *ID){
    if(strlen(deviceId) == 8){
        deviceId[0] = '\0';
    }
    strncpy(deviceId, ID, 8);
}

//gets the id of the device, for API call 11
//12/1/2022 updated with error handling for an non existent ID
char *getDeviceID(){
    if(strlen(deviceId) < 8 || strlen(deviceId) > 8){
        static char error[41];
        strncpy(error, "Error, no ID associated with this device", 40);
        return error;
    }
    else{
        return deviceId;
    }
}

//getting resources from this node. i have only "wikipedia" load but it can change depending on what is on the node
char *getResources(int resource){
    static char error[44];
    switch(resource){
        case 1:
            return wikipedia;
        break;
        default:
            strncpy(error, "Error, resource does not exist on this node", 43);
            return error;
        break;
    }
}

//get time from device to send across so then the other device can compare
char *getStartTime(){
    //found this setup here: https://stackoverflow.com/questions/5141960/get-the-current-time-in-c
    //not my original code
    time_t rawtime;
    struct tm * timeinfo;

    time ( &rawtime );
    timeinfo = localtime ( &rawtime );
    static char returnArr[9];
    strncpy(returnArr, asctime (timeinfo)+11, 8);
    
    return returnArr;
}

//testing all my methods, to make sure everything works
int main()
{
    setDeviceID("test123");
    printf("Fail: %s", getDeviceID());
    char temp[8] = "test1234"; 
    setDeviceID(temp);
    printf("\nPass: %s", getDeviceID());
    temp[0] = '\0';
    strncpy(temp, "1234test", 8);
    setDeviceID(temp);
    printf("\nDouble Check set Device id works: %s", getDeviceID());
    printf("\nWikipedia is stored on: %s", getResources(1));
    printf("\nFail to find resource: %s", getResources(2));
    printf("\nCurrent Time: %s", getStartTime());
    return 0;
}

