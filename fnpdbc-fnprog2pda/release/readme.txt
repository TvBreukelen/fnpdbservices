===================================================================================================
FNProg2PDA - README.TXT                                         
===================================================================================================

This README file contains important, last minute information  about FNProg2PDA.

FNProg2PDA is developed by

  Tom van Breukelen
  Bad Schwalbach, Germany


----------------------------------------
   Contents
----------------------------------------

1. What is FNProg2PDA?
2. How to install FNProg2PDA?
3. How to remove FNProg2PDA?
4. Disclaimer
5. What is new?


===================================================================================================
1. What is FNProg2PDA?
===================================================================================================
FNProg2PDA is a add-on tool for the FNProgramvare software to transfer information from the
AssetCAT, BookCAT, CATraxx, CATVids, SoftCAT or StampCAT databases to one of the following file
formats:

Name            OS            
--------------------------------------------
Calc            All platforms
CSV             All platforms
DBase3          MS-DOS
DBase4          MS-DOS
DBase5          Microsoft Windows
Foxpro          Microsoft Windows 
JFile 3         Palm
JFile 4         Palm
JFile 5+        Palm
JSON            All Platforms
HanDBase        Palm/Pocket PC/iPhone/iPad
List            Palm 
MobileDB        Palm/PocketPC
MS-Excel        Microsoft Windows / Mac OS X
Pilot-DB        Palm  
TSV             All platforms
XML	            All platforms


To be able to use the FNProg2PDA programs you'll need to have the following on your PC:

  AssetCAT, BookCAT, CATraxx, CATVids, StampCAT or SoftCAT database(s)
  MS-Windows, Mac OS X or Linux
  Java 8.0 (or later)


Notes 
-----
The Java runtime can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads
All FNProgramvare products can be found at http://www.fnprg.com. BookCAT, CATraxx, CATVids and
StampCAT are shareware programs. SoftCAT has been released as freeware and shareware version.


===================================================================================================
2. How to install FNProg2PDA?
===================================================================================================

For Windows
-----------
1. Start the setup program

2. Follow the instructions on the screen. 
   When requested, specify the folder where you want to install FNProg2PDA. Unless you specify a 
   different folder, the application will be installed in your program files folder 
   (example: C:\Program Files\FNProg2PDA).
   

For Mac
-------
1. Unpack the FNProg2PDA_Mac.zip file in your Applications folder.


For Linux
---------
1. Unpack the FNProg2PDA.zip file in your Applications folder

2. Grant FNProg2PDA.jar or FNProg2PDA.sh execute permissions. If you intend to run FNProg2PDA in
   batch mode, grant FNProg2PDA_batch.sh execute permissions as well.


============================================================
3. How to remove DBConvert
============================================================

On a Windows PC
---------------

1. From the Start button, choose Settings / Control Panel.

2. In Control Panel, double click the Add/Remove Programs icon.

3. Locate FNProg2PDA in the program list.
   Click on it to select it.

4. Click the Add/Remove button.


On a Mac or Linux PC
--------------------

Delete the FNProg2PDA installation folder from your applications folder.



===================================================================================================
4. Disclaimer
===================================================================================================

FNProg2PDA is free software; you can redistribute it and/or modify it under the terms of the GNU
General Public License as published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

FNProg2PDA is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.



===================================================================================================
4. What is New?
===================================================================================================

Version 9.2
-----------
Improved the JSON export
Included DBConvert with JSON file import


Version 9.1
-----------
Improved WantList, PlayList and BoxSet exports.
Added support for Json files,
Removed support for P. Referencer.
Minor bug fixes.


Version 9.0
-----------
Added support for OpenOffice/LibreOffice Calc spreadsheet (ODS files).
Changed the order of the export files (moved legacy formats to the end of the list).
Updated external libraries.


Version 8.9.1
-------------
New DBConvert version (SQLite import improvements). 
External libraries update.
Fixed check for new version each month could not be saved and other minor bugs.


Version 8.9
-----------
Updated help files. Added Delete button to the Sort dialog. Updated deprecated code.
Minor bug fixes.


Version 8.8.1
-------------
Added bash startup scripts for running FNProg2PDA under Linux in standard and batch mode
Renamed FNProg2PDA_NoGUI to FNProg2PDA_batch
Bug Fix: problem with running FNProg2PDA_NoGUI


Version 8.8
-----------
Bug Fix: under Linux icons were not shown


Version 8.7.1
-------------
The rather confusing conversion settings for booleans, times and dates has disappeared. It is now
possible to change the export for these values on a field by field basis.
A bug prevented to include author roles in the BookCAT export, this has been fixed. 


Version 8.7
-----------
Cleaned up General Config dialog.
Used SonarCube to improve code quality
Fixed MS-Excel empty cells bug
Added more fields to export for BookCAT
Used Restful Service API to perform SourceForge version check


Version 8.6.2
-------------
Removed "Replace Records in Database" feature for non HanDBase exports
Added field name, type and size checks for DBase and Foxpro exports
Replaced "Save" by "Apply" button in the sort, filter and miscellaneous dialogs. 
Added an application icon


Version 8.6.1
-------------
Fixed a DBase/Foxpro export error


Version 8.6
-----------
Removed deprecated Apple Java extensions
Fixed an Artist Name bug (CATraxx)
Image export is now possible for all formats
More consistent GUI design (incl. a better date picker)
Converted program to Java 8
Updated external libraries


Version 8.5
-----------
Added support for author- and artist's information
Updated 3rd party libraries
Added logging


Version 8.4
-----------
Added support for all images (covers, screenshots, images and thumbnails)


Version 8.3
-----------
Removed the external image file existence check, in case only the image filenames have to be
exported. 
Added the General Setting option to exclude the path from the image filename. 


Version 8.2.4
-------------
FNProg2PDA can now check every x. no of days for a new version.
Added "Friends of FNProgramvare" web site links (Facebook and Forum)
Added support for SQLite export in DBConvert
Updated 3rd party libraries
Migrated from Java 6 to Java 7 
Removed FNProgramvare web sites
Removed support for SmartList to Go


Version 8.2.3
-------------
Bug fix: double click on DBConvert.jar wasn't supported under Linux
Bug fix: you had to click the OK button twice to close the About dialog.


Version 8.2.2
-------------
Bug fix: Tracks without an Artist, caused an IndexOutOfRange exception in case the Track Artist had
to be exported 


Version 8.2.1
-------------
Added Tag field to AssetCAT export
Bugfix: Misc. Settings were no longer displayed
Bugfix: xViewer wouldn't edit


Version 8.2
-----------
Added AssetCAT support
Added online version checking
Added Excel 2007+ support (.xlsx files)
Added password support to HanDBase export


Version 8.1
-----------
Added WantList support for BookCAT, CATraxx and CATVids
Added Boxset and PlayList support for CATraxx
Bugfix: timestamp wasn't set when cloning/copying a profile
Bugfix: couldn't save alternative field name
Bugfix: a couple of one-to-many relationships between tables were not properly supported
Bugfix: CATraxx and CATVids media field wasn't sorted
Several small bug fixes, mainly related to the GUI


Version 8.0 
-----------
ODBC driverless (OS independent) and Unicode compatible version
