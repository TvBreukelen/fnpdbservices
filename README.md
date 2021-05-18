# fnpdbservices
FNProg2PDA and DBConvert are two separate projects on SourceForge (see links below) that share the same code base.

https://sourceforge.net/projects/fnprog2pda/

https://sourceforge.net/projects/dbconvert/


FNProg2PDA is an add-on tool for the FNProgramvare software (https://www.fnprg.com/) to transfer information from the AssetCAT, BookCAT, 
CATraxx, CATVids, SoftCAT or StampCAT databases to a PDA Database, Microsoft Excel or Calc spreadsheet, csv textfile, xBase database (DBase 3, 4, 5 and Foxpro) or XML file.

FYI: FNProgramvare has ceased operations, but their software is still regularly updated. 

DBConvert is a database cross converter for PDA- and PC Databases, user definable text files (csv) and other file formats. It supports the same file formats as FNProg2PDA. It is currently the more popular project. Users of FNProg2PDA don't need to download it, because it is included as tool in FNProg2PDA.

Notes
-----
Installing, building and running FNProg2PDA and DBConvert is straight forward with Maven. Maven builds them as jar files and as a MacOS 
App. When running on a Mac, it also creates the DMG file. For Windows, I use Launch4J to create exe files and InnoSetup to create the
Windows installation programs. For Linux I pack the jar files and lib folders in a zip file. That workflow is still done manually.

Best regards,

Tom van Breukelen
Bad Schwalbach, Germany
