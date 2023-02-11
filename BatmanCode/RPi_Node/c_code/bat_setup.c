#include <stdlib.h>
// this is def bad and needs updating but it works atm
int main(){
	int s = system("sudo modprobe batman-adv");
	s = system("batctl -v");
	s = system("sudo ip link add name bat0 type batadv");
	s = system("sudo ip link set dev eth0 master bat0");
	s = system("sudo ip addr flush dev eth0");
	s = system("sudo ip link set bat0 up");
	s = system("sudo ip add add 11.1.1.0/24 dev bat0");
	s = system("ip -br addr");
	s = system("sudo batctl o");
	return 0;
}
