============================================================
DBConvert - README.TXT                                         
============================================================

This README file contains important, last minute information about DBConvert.

DBConvert is developed by

  Tom van Breukelen
  Bad Schwalbach, Germany
  

----------------------------------------
Contents
----------------------------------------

1. What is DBConvert?
2. DBConvert requirements
3. How to install DBConvert?
4. How to remove DBConvert?
5. What is new?


============================================================
1. What is DBConvert?
============================================================

DBConvert is a database cross converter for PDA- and PC Databases, user definable textfiles (csv) and other file formats.
The current version of DBConvert can Import from:

Name            OS            
----------------------------------
Calc			All platforms
JFile 3         Palm
JFile 4         Palm
JFile 5+        Palm
List            Palm 
MobileDB        Palm/PocketPC
Pilot-DB        Palm  
MS-Access       Microsoft Windows / Mac OS X
MS-Excel        Microsoft Windows / Mac OS X
SQLite			Cross platform (Mac, Windows, Android, etc.)
DBase III       MS-DOS
DBase IV        MS-DOS
DBase V         Microsoft Windows
Foxpro          Microsoft Windows 
Visual Foxpro   Microsoft Windows
CSV Textfile    All platforms
XML File        All platforms


and Export to:

Name            OS            
----------------------------------
Calc			All platforms
JFile 5+        Palm
HanDBase        Palm/Pocket PC/iPhone/iPad
List            Palm 
MobileDB        Palm/PocketPC
Pilot-DB        Palm  
MS-Excel        Microsoft Windows / Mac OS X
DBase3          MS-DOS
DBase4          MS-DOS
DBase5          Microsoft Windows
Foxpro          Microsoft Windows 
CSV Textfile    All platforms
XML File        All platforms


DBConvert is compatible with all operating systems that are able to run Java (version 8 or higher).
DBConvert is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License 
as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

DBConvert is distributed in the hope that it will be useful,but WITHOUT ANY WARRANTY; without even the implied warranty
of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.


============================================================
2. DBConvert requirements
============================================================

To be able to use the DBConvert program you should have Java Runtime version 8.0 (or later) installed
 

Notes 
-----
  The Java runtime can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads


============================================================
3. How to install DBConvert?
============================================================

For Windows
-----------
1. Start the setup program

2. Follow the instructions on the screen. 
   When requested, specify the folder where you want to install DBConvert. Unless you specify a 
   different folder, the application will be installed in your program files folder 
   (example: C:\Program Files\DBConvert).


For Mac
-------
1. Unpack the DBConvert_Mac.zip file in your  Applications folder.


For Linux
---------
1. Unpack the DBConvert.zip file in your Applications folder

2. Grant DBConvert.jar or DBConvert.sh execute permissions. If you intend to run DBConvert in
   Batch mode than add execute permissions to DBConvert_batch.sh as well


============================================================
4. How to remove DBConvert
============================================================

On a Windows PC
---------------
1. From the Start button, choose Settings / Control Panel.

2. In Control Panel, double click the Add/Remove Programs icon.

3. Locate DBConvert in the program list.
   Click on it to select it.

4. Click the Add/Remove button.


On a Mac or Linux PC
--------------------
Delete the DBConvert installation folder from your applications folder.



============================================================
5. What is new?
============================================================

Version 6.6
-----------
Removed support for P. Referencer


Version 6.5
-----------
Added support for OpenOffice/LibreOffice Calc spreadsheet (ODS files).
Changed the order of the export files (moved legacy formats to the end of the list).
Updated external libraries.
 

Version 6.4.1
-------------
SQLite import improvements (better recognition of data types) and bug fixes
External libraries update.
Fixed check for new version each month could not be saved and other minor bugs.


Version 6.4
-----------
Updated help files. Added Delete button to the Sort dialog. Updated deprecated code.
Bug fix: Sort and Filter settings didn't load/save properly.


Version 6.3.1
-------------
Added bash scripts for running DBConvert in batch and standard mode.
Bug fix: under Linux buttons were shown without an icon.
 

Version 6.3
-----------
Cleaned up General Config dialog.
Used SonarCube to improve code quality
Fixed MS-Excel empty cells bug
Improved Textfile export
Used Restful Service API to perform SourceForge version check
The rather confusing conversion settings for booleans, times
and dates has disappeared. It is now possible to change the
export for these values on a field by field basis.


Version 6.2.2
-------------
Removed "Replace Records in Database" feature for non HanDBase exports
Added field name, type and size checks for DBase and Foxpro exports
Replaced "Save" by "Apply" button in the sort and filter dialogs. 
Added an application icon


Version 6.2.1
-------------
Fixed a DBase/Foxpro export error


Version 6.2
-----------
Removed deprecated Apple Java extensions
Updated external libraries
More consistent GUI design
Converted program to Java 8


Version 6.1
-----------
Fixed a date bug in the Excel export
Excel export new checks the number format, so that integers don’t have a .0 at the end. 


Version 6
---------
DBConvert can now check every x. no of days for a new version.
Added support for SQLite export
Updated 3rd party libraries
Migrated from Java 6 to Java 7 
Removed support for SmartList to Go
