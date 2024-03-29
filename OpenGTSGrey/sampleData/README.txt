-----------------------------------------------------------------------------------
Project: OpenGTS - Open GPS Tracking System
URL    : http://www.opengts.org
File   : sampleData/README.txt
-----------------------------------------------------------------------------------

This directory contains the sample GPS tracking data that can be viewed on the demo
website: http://track.opengts.org/track/Track

After the OpenGTS database has been properly initialized, the sample data can be
loaded using the following steps:
1) cd to the OpenGTS installation directory.
2) Create the 'demo' account (without any password):
     > bin/admin.pl  Account -account=demo -nopass -create
   Or, on Windows:
     > bin\admin.bat Account -account:demo -nopass -create
3) Create the devices 'demo' and 'demo2':
     > bin/admin.pl  Device -account=demo -device=demo  -create
     > bin/admin.pl  Device -account=demo -device=demo2 -create
   Or, on Windows:
     > bin\admin.bat Device -account:demo -device:demo  -create
     > bin\admin.bat Device -account:demo -device:demo2 -create
4) Load the data for 'demo' and 'demo2':
     > bin/dbAdmin.pl   -load=EventData.csv -dir=./sampleData
   Or, on Windows:
     > bin\dbConfig.bat -load:EventData.csv -dir:./sampleData
5) Make sure that 'demo="true"' is set on the 'Domain' tag in the 'private.xml' file.
   Rebuild and redeploy the 'track.war' file if any changes were made to 'private.xml'.

At this point, if no errors occurred, you should be able to access the demo account
and view the 'demo' data through the OpenGTS web interface.

Note:
- The date of all of the installed sample data occurs on March 13, 2007.  This date
  for the "demo" account and "demo"/"demo2" devices is hardcoded in the class
  "org.opengts.war.tools.RequestProperties".  This will become the default date range
  when displaying these points on a map or in a report. 

-----------------------------------------------------------------------------------
