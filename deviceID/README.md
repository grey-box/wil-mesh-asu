# How the Code Works:

**generate\_id:** These files are meant to address a security concern: how do we know that any node that connects to the mesh is another UNI device?  How do we know it's not somebody's laptop looking to cause some damage to the system? This was originally created as a simple, scaleable solution to verifying the identity of a connecting device.

Before trying to establish a network connection, the UNI devices will call this method to generate a unique ID for itself which it can then send to other UNI devices to initiate a connection.
The ID is generated using stdlib rand() function. It is stored as a character sequence internally and its length is dictated by input(a default length is hardcoded if needed though).

The function will repeatedly make char arrays until it qualifies a rule, at which point, the function will return a pointer to the array of char with the unique ID.
The rule is that the sum of the ASCII values of each char in the ID must be divisible by a number equalling half the length of the ID. I figured this would make it difficult to guess through random guessing/brute force but is wide enough that it would allow for a large network of nodes to be compatible.

Since putting this together, however, I've had a group mate consider just keeping a log of IDs hard-coded in and then denying any connection not on that list. We're currently still talking about how we wish to proceed.

**main.c:** This file includes some code to test that the generate\_new\_key method works as intended. It will print the key the function generates to the console.

# How to Use The Code:
For just testing the capabilities of the function, compile:
> gcc generate\_id.c main.c -o "gen\_id".

From there, the executable can be run with:
> ./gen\_id \<key\_length\> \<random\_seed\>

The two command line arguments are optional. The default key\_length is 15 and the default random\_seed is based on the time.

Otherwise, just include **generate\_id.c** among other project files to get access to the generate\_new\_key() method.

generate\_new\_key method requires two inputs:
> r\_seed: This integer value will seed the srand function which is used to generate the key.

> key\_length: This integer value will determine how long the key should be.

NOTE: The key this method returns is actually 1 character longer than the requested size. Each key will end with a '\0' character at the key\_length position in the char array. 
