@echo off
Rem *---------------------------------------------------------------*
Rem * DBConvert Batch file                                          *
Rem *                                                               *
Rem * Parameters                                                    *
Rem *    Profile1     = First DBConvert Profilename                 *
Rem *    Profile2..n  = Second till last DBConvert Profilename      *
Rem *    ExportType   = Export File Type                            *
Rem *                                                               *
Rem * Examples                                                      *
Rem *    DBConvert_batch.exe "Palm Db" MS-Excel                     *
Rem *    DBConvert_batch.exe myXml herXml HanDBase                  *
Rem *---------------------------------------------------------------*
 
DBConvert_batch.exe Albums MS-Excel
pause