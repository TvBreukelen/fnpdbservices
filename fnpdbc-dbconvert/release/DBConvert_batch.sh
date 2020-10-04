#! /bin/bash
clear

<<COMMENTS
 *---------------------------------------------------------------*
 * DBConvert Batch file                                          *
 *                                                               *
 * Parameters                                                    *
 *    Profile1     = First DBConvert Profilename                 *
 *    Profile2..n  = Second till last DBConvert Profilename      *
 *    ExportType   = Export File Type                            *
 *                                                               *
 * Examples                                                      *
 *    java -jar DBConvert.jar Albums MS-Excel                    *
 *    java -jar DBConvert.jar Albums Books JFile5                *
 *---------------------------------------------------------------*
COMMENTS

java -jar DBConvert.jar Albums Books "Text File"

read -p "Press any key to continue..."
