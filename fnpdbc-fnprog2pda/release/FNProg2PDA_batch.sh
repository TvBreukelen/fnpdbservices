#!/bin/bash
<<COMMENTS
 *---------------------------------------------------------------*
 * FNProg2PDA Batch file                                         *
 *                                                               *
 * Parameters                                                    *
 *    Profile1     = First FNProg2PDA Profilename                *
 *    Profile2..n  = Second till last FNProg2PDA Profilename     *
 *    ExportType   = Export File Type                            *
 *                                                               *
 * Examples                                                      *
 *    java -jar FNProg2PDA.jar Albums MS-Excel                   *
 *    java -jar FNProg2PDA.jar Albums Books JFile5               *
 *---------------------------------------------------------------*
COMMENTS

java -jar FNProg2PDA.jar Albums Books "Text File"

read -p "Press any key to continue..."
