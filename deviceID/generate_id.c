#include <stdlib.h>
#include "generate_id.h"

/*
 * DESCRIPTION:
 *    Generates a random string of characters to serve as a unique identifier
 *    for nodes meant to form the mesh on the network.
 * PARAMETERS:
 *    r_seed: an integer value to seed the random generation of the key.
 *    size: an integer value to indicate the length of the key.
 * RETURNS:
 *    A char pointer to a string of the given size(the last char is '\0').
 */
char* generate_new_key(int r_seed, int size) {
    char* securityKey;
    unsigned int checkSum;

    checkSum = 1;
    securityKey = malloc((size + 1) * sizeof(char));
    srand(r_seed);

    while( checkSum % (size/2) != 0 ) {
        checkSum = 0;
        for( int i = 0; i < size; i++ ) {
            securityKey[i] = (rand() % RANGE_WIDTH) + RANGE_START;
            checkSum += securityKey[i];
        }
    }

    securityKey[size] = '\0';
    return securityKey;
}
