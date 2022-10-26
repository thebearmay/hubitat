<b>Hubitat® Elevation Higher Availability (HE-Ha)</b>

For homes where automation of the environment has become the expected condition it is highly disruptive when a key component fails.  In the Hubitat® environment, the loss of the hub is significant, and while the Hub Protection service provides for a replacement hub and permits restoration of the hub’s data and radios there is a delay before the restoration can occur due to shipping.  In the ideal world, a secondary hub would be on hot standby and would automatically assume control of the environment if it detected a failure (loss of communication in the primary hub).  This details one solution.

<b><i>Components</i></b>

<b>From Hubitat</b>
  1.	Hub Protection Cloud Backups
  2.	Spare C7 Hub 

<b>Community Supplied Apps</b>

  1.	<a href='https://raw.githubusercontent.com/thebearmay/hubitat/main/heHa/heFailover.groovy'>Hub Failover Manager Application</a>
  2.	<a href='https://raw.githubusercontent.com/thebearmay/hubitat/main/heHa/hubFileSync.groovy'>Hub File Manager Sync (Optional)</a>      
    a.	for application data files, etc.
  3.	<a href='https://raw.githubusercontent.com/thebearmay/hubitat/main/apps/hubVarSync.groovy'>Hub Variable Sync (Optional)</a>      
    a.	Only needed if using variables that affect real time processing 

<b><i>Setup and Flow</i></b>

<b>Initial</b>

  1.	Power up and register the spare hub.
  2.	Turn off the ZWave and Zigbee radios on the spare hub
  3.	Install and configure the Failover Manager app and, if desired, the optional File Manager Sync and Variable Sync apps on the production hub.
  4.	Create a Cloud Backup of the production hub and restore it on the spare hub.
  5.	Temporarily turn off the Zigbee Radio on the production hub
  6.	Pair all of the Zigbee devices with the spare hub.
  7.	Turn off the Zigbee radio on the spare hub.
  8.	Turn on the Zigbee radio on the production hub.
  9.  Check the configuration of the Failover Manager app, and then      
    a. Press the button to disable all apps on the spare hub      
    b. Toggle the <i>Turn off all radios and start monitoring heartbeat</i> switch

<b>Ongoing Maintenance</b>

  1.	Periodically take a backup from production and restore it to the spare to capture any rule or application changes.
  2.	After adding a new device to production do a Cloud Backup of production and restore it to the spare; if a zigbee device pair the device to the spare afterwards.
  3.	Re-initialize the Hub Failover app by doing 9(a) above.

  <b>In Operation</b>

  •	Failover App on the spare hub does a periodic (configurable) ping of the production hub.  
  •	If the production hub fails to respond X (configurable) times:        
&emsp;•	Failover app sends shutdown command to production (in case it is up and can’t respond)        
&emsp;•	Failover turns on the Zigbee and Zwave radio on the spare hub       
&emsp;•	Failover turns on all disabled apps 
