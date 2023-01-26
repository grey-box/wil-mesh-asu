#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <ctype.h>
#include <string.h>

#define NUMBER_OF_STRING 5
#define MAX_STRING_SIZE 256

FILE* Uni = NULL;

// array of valid String IDs
char *ID_arr[] = {
    "1235",
    "1234",
    "12adawda",
    "Filler",
    "Filler2"
};

// checks if the given String ID is valid by comparing it to the existing array of valid IDs
static bool isValidID(char *s) {
    size_t n = sizeof ID_arr / sizeof *ID_arr;
    
    for (int i = 0; i < n; i++) {
        if (strcmp(s, ID_arr[i]) == 0) {
            printf("Input ID: '%s' is a valid ID. It matches element %d in the array. \n", s, i);
            return true;
        }
    }
    
    printf("Input ID: '%s' is not a valid ID. It does not match any element in the array. \n", s);
    return false;
}

// prints out the array of String IDs for testing purposes
void print_array(char *arr[]) {
    size_t n = sizeof ID_arr / sizeof *ID_arr;
    
	for (int i = 0; i < n; i++) {
	    int len = strlen(arr[i]);
		printf("%d: '%s' has length %d\n", i+1, arr[i], len);
	}
}

// asks for user input for a String and returns it
char *scanInput() {
    char *s;
    
    s=(char*)malloc(4);
	scanf("%s", s);
	
	return s;
}

// Loop to constantly check user input
void scanIDLoop() {
    char *s;
    s=(char*)malloc(4);
    
    printf("Scan Loop started for checking UNI ID.");
    
    while (strcmp(s, "q") != 0) {
        printf("\n");
        printf("Please input an ID or enter 'q' to terminate: ");
	    s = scanInput();
	    
	    if (strcmp(s, "q") != 0) {
	        isValidID(s);
	    }
    }
    
    printf("Loop exited.\n");
}

// reads the current IDs in the txt file and stores them in the array
void readIDFile(char *s) {
    char filler[256];
    
    Uni = fopen(s, "r");
    size_t n = sizeof ID_arr / sizeof *ID_arr;
    
    if (Uni != NULL) {
    for (int i=0; i < n; i++) {
        fscanf(Uni, "%s\n", ID_arr[i]);
    }
    }
    
    fclose(Uni);
}

// writes the current IDs stored in the array to the txt file
void saveIDFile(char *s) {
    Uni = fopen(s, "wb");
    fwrite(ID_arr, sizeof(char), sizeof(ID_arr), Uni);
    fclose(Uni);
}


// add inputted ID in the array
void addID(char *s) {
    size_t n = sizeof ID_arr / sizeof *ID_arr;
    
    ID_arr[n] = s;
}

// deletes inputted element in the array
void delID(int element) {
    size_t n = sizeof ID_arr / sizeof *ID_arr;
    
    if (element <= 0) {
        return;
    }
    
    else {
        for (int i = element; i <= n; i++) {
            ID_arr[i - 1] = ID_arr[i];
        }
    }
}

// tests some functions to make sure they work
void testRun() {
    char *correct = "12adawda";
    char *wrong = "123";
    
    print_array(ID_arr);
    // Checks if char *correct or *wrong ID matches any of the IDs stored in the array
    isValidID(correct);
    isValidID(wrong);

    printf("\n");
    // printf("Please input an ID: ");
    
    // Checks if user inputted ID matches any of the IDs stored in the array
    // isValidID(scanID());
    
    // printf("\n");
    // scanIDLoop();
    

    delID(2);
    print_array(ID_arr);
    print_array(ID_arr);
    
}


void readMode() {
   printf("Read selected.\n");
}


void saveMode() {
   printf("Save selected.\n");
}


void addMode() {
   printf("Add selected.\n");
}


void delMode() {
   printf("Delete selected.\n");
}


// Runs all the main interface for the user to interact with the functions
void userRun() {
    char s;
    
    printf("Enter one of the following characters to select a mode: \n\n");
    printf("r = Read and display all valid IDs\n");
    printf("s = Save current list of valid IDs to a file\n");
    printf("a = Add a new ID to the list of valid IDs\n");
    printf("d = Delete an existing ID from valid IDs\n");
    printf("q = Quit\n\n");
    printf("Enter one character: ");
    scanf(" %c", &s);
    
    switch(s) {
        case 'r' :
            readMode();
            break;
        case 's' :
            saveMode();
            break;
        case 'a' :
            addMode();
            break;
        case 'd' :
            delMode();
            break;    
        case 'q' :
            printf("Exitting program.");
            break;
        default :
            printf("Invalid input, only one character allowed.\n" );
   }
    
}

// Driver class
int main(void) {
   // testRun();
    
    userRun();
	return 0;
}