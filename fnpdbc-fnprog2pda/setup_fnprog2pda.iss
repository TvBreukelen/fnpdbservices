; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
AppName=FNProg2PDA
AppVerName=FNProg2PDA 9.5
AppPublisher=TvBSoftware
AppPublisherURL=http://sourceforge.net/projects/fnprog2pda
AppSupportURL=http://sourceforge.net/projects/fnprog2pda
AppUpdatesURL=http://sourceforge.net/projects/fnprog2pda
DefaultDirName={commonpf}\FNProg2PDA
DefaultGroupName=FNProg2PDA
LicenseFile=release\license.txt
AllowNoIcons=yes
Compression=lzma
SolidCompression=yes
OutputDir=release
OutputBaseFilename=setup_fnprog2pda

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
Filename: "{app}\FNProg2PDA.url"; Section: "InternetShortcut"; Key: "URL"; String: "http://sourceforge.net/projects/fnprog2pda"

[Icons]
Name: "{group}\FNProg2PDA"; Filename: "{app}\FNProg2PDA.exe"; WorkingDir: "{app}"
Name: "{userdesktop}\FNProg2PDA"; Filename: "{app}\FNProg2PDA.exe"; WorkingDir: "{app}"; Tasks: desktopicon
Name: "{group}\{cm:ProgramOnTheWeb,FNProg2PDA}"; Filename: "{app}\FNProg2PDA.url"
Name: "{group}\{cm:UninstallProgram,FNProg2PDA}"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\FNProg2PDA.exe"; Description: "Launch FNProg2PDA"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\FNProg2PDA.url"
