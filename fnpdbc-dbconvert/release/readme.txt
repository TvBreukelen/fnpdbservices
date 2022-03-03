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
The current version of DBConvert can

Import from:

Name            OS            
----------------------------------
Calc            All platforms
CSV             All platforms
DBase           Microsoft Windows
Foxpro          Microsoft Windows 
JFile           Palm
List            Palm 
JSON            All Platforms
MobileDB        Palm/PocketPC
MS-Access       Microsoft Windows / Mac OS X
MS-Excel        Microsoft Windows / Mac OS X
MariaDB         All platforms 
Pilot-DB        Palm  
SQLite          All Platforms
TSV             All platforms
Visual Foxpro   Microsoft Windows
XML             All platforms
YAML            All platforms


and Export to:

Name            OS            
----------------------------------
Calc            All platforms
CSV             All platforms
DBase           MS-DOS
Foxpro          Microsoft Windows 
JFile           Palm
JSON            All Platforms
HanDBase        Palm/Pocket PC/iPhone/iPad
List            Palm 
MobileDB        Palm/PocketPC
MS-Excel        Microsoft Windows / Mac OS X
Pilot-DB        Palm  
TSV             All platforms
XML	            All platforms
YAML            All platforms


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

Version 7.1
-----------
Added support for MariaDB


Version 7
---------
Added Group By function to XML, JSON and YAML exports
Replaced local CSV code by Jackson CSV library
Fixed an age old XML "export from" and Textfile "export to bug"
Made some code refactoring to improve readabilty
Reinstated build of Mac App
