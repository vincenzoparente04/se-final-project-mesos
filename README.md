Mesos 

Developed features
- complete rules
- TUI
- GUI
- socket and RMI
Advanced functionality
- database
- mutiple games
- resilience to disconnections

Prerequisites
- Java 21 is required to run this application.
- MySQL installed, running, and properly configured.

Execution help
(from /Mesos directory)
Server run: java -jar target/serverMain.jar
Cli Client: java -jar target/clientMainCLI.jar
Gui Client: java -jar target/clientMain.jar

Note:
For Windows users, add the following command before the above commands to execute the jar:
"chcp 65001 > $null;"    
e.g.  chcp 65001 > $null; java -jar target/serverMain.jar
This is necessary to avoid windows terminal encoding problems

Note:
The server runs on fixed standard ports: 1099 for RMI and 9999 for Socket connections.
