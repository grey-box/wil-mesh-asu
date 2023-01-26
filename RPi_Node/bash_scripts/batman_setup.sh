#!/bin/bash

# Batman-adv mesh setup
# RUN in bash command ---> bash batman_setup.sh
clear
sudo modprobe batman-adv
batctl -v
sudo ip link add name bat0 type batadv
sudo ip link set dev eth0 master bat0
sudo ip addr flush dev eth0
sudo ip link set bat0 up
sudo ip add add 11.1.1.0/24 dev bat0
ip -br addr
echo "BATMAN-ADV Originator"
sudo batctl o
