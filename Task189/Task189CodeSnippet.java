/**Nicholas Ellender
 * 2/11/2023
 * This is a setup and idea for WifiP2pGroup
 * NOT RUNNABLE
 * Not sure how the manager and groups connect to each other
 */

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
public class Task189CodeSnippet {

    public static void main(String[] args){
        //make the group object
        WifiP2pManager main = new WifiP2pManager();
        
        //make this device listen to p2p network
        main.createGroup(WifiP2pManager.initialize(),  WifiP2pManager.ActionListener listener)
        
        //check to see if this device is the owner
        System.out.println(main.isGroupOwner()); 
        
        //check to see who is the owner
        System.out.println(main.getOwner());
        
        //Gets the network name
        System.out.println(main.getNetworkName());
        
        //gets passphrase
        System.out.println(main.getPassPhrase());
        
        
        
        
    }
}
