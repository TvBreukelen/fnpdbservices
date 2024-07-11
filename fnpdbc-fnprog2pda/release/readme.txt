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
xBase           Microsoft Windows
JSON            All Platforms
HanDBase        Palm/Pocket PC/iPhone/iPad
MS-Excel        Microsoft Windows / MacOS
Firebird        All platforms
MariaDB         All platforms
PostGreSQL      All platforms
SQLite          All platforms
TSV             All platforms
XML             All platforms
YAML            All Platforms
BookBuddy       iPhone/iPad 
MovieBuddy      iPhone/iPad   
MusicBuddy      iPhone/iPad 


To be able to use the FNProg2PDA programs you'll need to have the following on your PC:

  AssetCAT, BookCAT, CATraxx, CATVids, StampCAT or SoftCAT database(s)
  MS-Windows, Mac OS X or Linux
  Java 17.0 (or later)


Notes 
-----
The Java runtime can be downloaded from https://adoptium.net/en-GB/ or 
  https://www.oracle.com/java/technologies/downloads/

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
1. Open the FNProg2PDA.dmg file
2. Drag the FNProg2PDA app to the Applications folder


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
5. What is New?
===================================================================================================

Version 10.6
------------
Added export to MariaDB


Version 10.5
------------
Added append option to Calc and Excel export.
Added export to Firebird and PostGreSQL databases.
Removed export to Palm-OS databases (JFile, List, MobileDB and PilotDB).
Viewer can now display images and items are shown with the correct row height.


Version 10.4
------------
Updated Java version to 17
Added export to SQLite databases.
Updated third party libraries.
Updated help files.
Fixed some minor bugs.


Version 10.3.1
--------------
CATVids: VideoCustom can now be exported from Contents.
Small improvements for MovieBuddy export


Version 10.3
------------
Added filter on "fuzzy dates"
Bug fix: Last import date couldn't be set manually  


Version 10.2
------------
Added support for BookBuddy, MovieBuddy and MusicBuddy


Version 10.1
------------
Added cast, roles and director fields from CATVids
Bug Fix: some Person fields were disappeared from the selection list


Version 10.0
------------
Moved DBase and FoxPro output files to xBase
Extended the number of fields to be exported
Minor bug fixes
