## RoomView Mobile

I wrote this because I got tired of having to walk all the way back to the office at the helpdesk I work at only to see that the classroom I just left issued another help request. I also did not like having to have a monitor dedicated to RoomView. 

So, I did some packet analysis with Wireshark and discovered both how RoomView Express connects to the classrooms and what the help request packets looked like, and built a mobile app!

Right now the only feature is detection of help requests from rooms - rooms are loaded via a CSV file with "room" and "ip" columns. Help requests are shown in a dialog and as notifications. 

I'm planning on switching to using local SQLite storage instead of relying on reading a CSV - it means I may have to build a room management area but it's better than having to worry about a file vanishing. 

I also plan to investigate adding some more monitoring features, as well as the ability to issue some remote commands such as shutdown/startup to cover the two most common things I have to walk out to do. 

## Installing

You can either load the app by running it on your device via Android Studio, or copy the built apk to your phone storage and install from there. CSV file can be on a Google drive or any other location that supports the Storage Framework.
