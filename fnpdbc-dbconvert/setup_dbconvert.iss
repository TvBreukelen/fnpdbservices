; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
AppName=DBConvert
AppVerName=DBConvert 6.5
AppPublisher=TvBSoftware
AppPublisherURL=http://sourceforge.net/projects/dbconvert
AppSupportURL=http://sourceforge.net/projects/dbconvert
AppUpdatesURL=http://sourceforge.net/projects/dbconvert
DefaultDirName={commonpf}\DBConvert
DefaultGroupName=DBConvert
LicenseFile=release\license.txt
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes
OutputDir=release
OutputBaseFilename=setup_dbconvert

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"

[Files]
Source: "release\*.exe"; DestDir: "{app}"; Flags: ignoreversion; BeforeInstall: myCleanupBefore('libs')
Source: "release\*.cmd"; DestDir: "{app}"; Flags: ignoreversion
Source: "release\readme.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "target\libs\*.*"; DestDir: "{app}\libs"; Flags: ignoreversion

[CODE]
procedure myCleanupBefore(Directory: String);
var
  DirName: String;
begin
  DirName := ExpandConstant('{app}\' + Directory);
  if (FileOrDirExists(DirName)) then
     DelTree(DirName, False, True, True);
end;

[INI]
Filename: "{app}\DBConvert.url"; Section: "InternetShortcut"; Key: "URL"; String: "http://sourceforge.net/projects/dbconvert"

[Icons]
Name: "{group}\DBConvert"; Filename: "{app}\DBConvert.exe"; WorkingDir: "{app}"
Name: "{userdesktop}\DBConvert"; Filename: "{app}\DBConvert.exe"; WorkingDir: "{app}"; Tasks: desktopicon
Name: "{group}\{cm:ProgramOnTheWeb,DBConvert}"; Filename: "{app}\DBConvert.url"
Name: "{group}\{cm:UninstallProgram,DBConvert}"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\DBConvert.exe"; Description: "Launch DBConvert"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\DBConvert.url"
