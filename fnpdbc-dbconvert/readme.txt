Release Notes


Version 7.6
-----------
Added support for Paradox legacy databases.
Replaced local fork of JavaDBF with standard version.
Created a Settings option for the HanDBase Desktop program. 
Fixed the export to text file bug created in 7.5.1.


Version 7.5.1
-------------
Improved validation when databases files are of the wrong format or no longer exists. 
Fixed a bug that caused some files not to be loaded correctly.
Updated SQlite driver. 
 

Version 7.5
-----------
For SQL databases it is now possible to use existing table relationships (foreign keys ->
primary keys) or define your own. This allows you to join tables together for SQL queries.
MS-Access is now accessed via the UCanAccess java JDBC Driver.


Version 7.4
-----------
Added support for Firebase databases
Updated external libraries
Minor bug fixes


Version 7.3
-----------
Minimum Java version is now set to Java 11
Added support for Microsoft SQL Server databases
Added international languages support for exporting to Handbase and xBase databases
Switching the database or table has been improved.
Option to manually change the viewer content has been removed, due to high memory
consumption.
Minor bug fixes.


Version 7.2.3
-------------
Updated PostGreSQL driver, due to security issues
Updated SQLite driver
The tables of SQL databases are no longer completely loaded in memory, if a filter is set. 
Bug Fix: SQLite SELECT COUNT(*) statement doesn't return a Long value but an Integer
causing the program to abort.
Bug Fix: could not enter a PostGreSQL database if a MariaDB database with the same name has
already been defined (and vice versa).  


Version 7.2.2
-------------
Save the positions and size of the Help dialog
Added the slf4j-nop library so that the logging warning no longer appears in batch
Removed support for JFile 3 and 4
Minor code refactoring (SonarQube) 
Bug Fix: appending new records to Palm database files didn't function correctly


Version 7.2.1 
-------------
Updated external Jackson databinding libraries due to possible security vulnerabilities.
DBConvert now has its own General Settings. Settings are no longer shared between DBConvert
and FNProg2PDA.
The default Look and Feel is now set to Nimbus.
Bug Fix: when checking the DBConvert version, the program crashed when no older version
was found.
Bug Fix: CSV file export failed in case "Incl. Field Names" wasn't set


Version 7.2 
-----------
Added support for PostgreSQL databases and VCard files
Added option to connect MariaDB via SSH
Improved SSL option
New option to skip empty records
New option to select an already defined database or file
Minor bug fixes


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
