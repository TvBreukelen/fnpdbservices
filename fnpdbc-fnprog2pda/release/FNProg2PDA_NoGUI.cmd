@echo off
Rem *---------------------------------------------------------------*
Rem * FNProg2PDA Batch file                                         *
Rem *                                                               *
Rem * Parameters                                                    *
Rem *    Profile1     = First FNProg2PDA Profilename                *
Rem *    Profile2..n  = Second till last FNProg2PDA Profilename     *
Rem *    ExportType   = Export File Type                            *
Rem *                                                               *
Rem * Examples                                                      *
Rem *    FNProg2PDA_NoGUI.exe "BookCAT (vs 7)" MS-Excel             *
Rem *    FNProg2PDA_NoGUI.exe Albums Videos HanDBase                *
Rem *    FNProg2PDA_NoGUI.exe HisBooks HerAlbums OurVideos MobileDB *
Rem *---------------------------------------------------------------*
 
FNProg2PDA_NoGUI.exe BookCAT List
pause