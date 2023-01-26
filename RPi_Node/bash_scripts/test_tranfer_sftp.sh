#!/bin/bash

# sftp test sending Test.txt from RPi1 to RPi3
# RUN in bash command ---> test_transfer_sftp.sh

clear
sftp johng@169.254.88.151 << {
put /home/johng/Desktop/Test.txt /home/johng/Desktop
exit
}


