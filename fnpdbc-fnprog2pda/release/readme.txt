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
JFile           Palm
JSON            All Platforms
HanDBase        Palm/Pocket PC/iPhone/iPad
List            Palm 
MobileDB        Palm/PocketPC
MS-Excel        Microsoft Windows / Mac OS X
Pilot-DB        Palm  
TSV             All platforms
XML             All platforms
YAML            All Platforms
BookBuddy       iPhone/iPad 
MovieBuddy      iPhone/iPad   
MusicBuddy      iPhone/iPad 


To be able to use the FNProg2PDA programs you'll need to have the following on your PC:

  AssetCAT, BookCAT, CATraxx, CATVids, StampCAT or SoftCAT database(s)
  MS-Windows, Mac OS X or Linux
  Java 11.0 (or later)


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
5. What is New?
===================================================================================================

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


Version 9.9
-----------
Created a Settings option for the HanDBase Desktop program. 
Replaced local fork of JavaDBF with standard version.
Minor bug fixes.


Version 9.8
-----------
Minimum Java version is now set to Java 11
Added international languages support for exporting to Handbase and xBase databases
Option to manually change the viewer content has been removed, due to high memory
consumption.
Save the positions and size of the Help dialog
Added the slf4j-nop library so that the logging warning no longer appears in batch
Removed support for JFile 3 and 4
Bug Fix: appending new records to Palm database files didn't function correctly


Version 9.7
-----------
Removed DBConvert as a tool. This program has a different function than FNProg2PDA and is available
separately. Both programs still use the same code base, but are now functionaly separated. This makes
maitainance easier, because there is no need to release a new version of one program when only the
other progam has changed.

Bug fix: export to a non standard CSV file with "Incl. Field Names" switched off caused an error (
null pointer exception).


Version 9.6
-----------
Added option to skip empty records
Included DBConvert with MySQL/MariaDB, PostgreSQL and VCard support
Minor bug fixes


Version 9.5
-----------
Added Group By function to XML, JSON and YAML exports
Added Misc. option for BookCAT to place the book's release number behind the series
Replaced local CSV code by Jackson CSV library
Fixed an age old Textfile "export to bug"
Done some code refactoring to improve readabilty
Reinstated build of Mac App


Version 9.4
-----------
Improved export to JSON and YAML files
Updated help files
Minor bug fixes
Included DBConvert with dBase version 7 support


Version 9.3
-----------
Added support for YAML files
Updated 3rd party libraries
Corrected Linux bash files


Version 9.2
-----------
Improved the JSON export
Included DBConvert with JSON file import


Version 9.1
-----------
Improved WantList, PlayList and BoxSet exports.
Added support for JSON files,
Removed support for P. Referencer.
Minor bug fixes.


Version 9.0
-----------
Added support for OpenOffice/LibreOffice Calc spreadsheet (ODS files).
Changed the order of the export files (moved legacy formats to the end of the list).
Updated external libraries.
