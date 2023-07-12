Release Notes

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
separately. Both programs still use the same code base, but are now functionally separated. This makes
maintenance easier, because there is no need to release a new version of one program when only the
other program has changed.

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
