Release Notes


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
