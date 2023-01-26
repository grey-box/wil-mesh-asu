#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <time.h>
#include "generate_id.h"

/*
 * Once compiled, call the following executable:
 * ./id_gen <key_length> <random_seed>
 * Both arguments are optional.
 * NOTE: THIS IS PURELY FOR TESTING PURPOSES. THIS MAIN SHOULD NOT BE PRESENT IN THE FINAL PRODUCT.
 */
int main(int argc, char** argv) {
    char* testKey;
    time_t seedTime;
    int randomSeed, keyLength;

    switch(argc) {

        //The user passes in no arguments
        case 1:
            testKey = malloc(KEY_LENGTH * sizeof(char));
            testKey = generate_new_key((unsigned)time(&seedTime), KEY_LENGTH);
            break;

        //The user passes in 2 arguments, presumably <key_length> <random_seed>
        case 3:
            for(int i = 0; argv[2][i] != '\0'; i++) {
                if( !isdigit(argv[2][i]) ) {
                    perror("Arguments must be integers. Please do not put any alpha or special characters in for arguments.");
                    return 2;
                }
            }
            randomSeed = atoi(argv[2]);
        //The user passes in 1 argument, presumably <key_length>
        case 2:
            for(int i = 0; argv[1][i] != '\0'; i++) {
                if( !isdigit(argv[1][i]) ) {
                    perror("Arguments must be integers. Please do not put any alpha or special characters in for arguments.");
                    return 2;
                }
            }
            keyLength = atoi(argv[1]);
            testKey = malloc(keyLength * sizeof(char));
            testKey = generate_new_key(randomSeed, keyLength);
            break;
        //The user passes in 3 or more arguments for some reason.
        default:
            perror("Too many arguments. This executable should only take a maximum of 2 integer arguments.");
            return 1;
    }

    printf("Your generated key is: %s\n", testKey);
    return 0;
}
