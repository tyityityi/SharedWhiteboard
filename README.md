# SharedWhiteboard  
A distributed whiteboard system.

# Envirorment  
JDK 1.8  

# How to run?  
  
Step 1:  
Open your terminal and cd to the project directory  
  
Step 2:   
"mvn package"   
   
Step 3:   
Open the whiteboard server 
via "java -cp target/pb3-0.0.1-SNAPSHOT-jar-with-dependencies.jar pb.WhiteboardServer"   
  
Step 4:  
Open other terminals and the whiteboard peers for sharing   
via "java -cp target/pb3-0.0.1-SNAPSHOT-jar-with-dependencies.jar pb.WhiteboardPeer -port portNumberYouLike"
