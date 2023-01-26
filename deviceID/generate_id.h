//Constant defined for the main function to test an arbirary length of key.
#define KEY_LENGTH 16

//Constant defined as the range of possible unique values a key could take based on the ASCII Table. DO NOT TOUCH!
#define RANGE_WIDTH 95

//Constant defined as where on the ASCII table it is acceptable to start pulling values from for the key. DO NOT TOUCH.
#define RANGE_START 32

char* generate_new_key(int r_seed, int length);
