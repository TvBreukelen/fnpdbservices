package application.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.table.AbstractTableModel;

import application.preferences.Profiles;
import application.utils.GUIFactory;
import application.utils.General;

public class ProjectModel extends AbstractTableModel {
	private static final long serialVersionUID = -540575298352100318L;

	public static final int HEADER_EDIT = 0;
	public static final int HEADER_PROJECT = 1;
	public static final int HEADER_PROFILE = 2;
	public static final int HEADER_EXPORTFILE = 3;
	public static final int HEADER_IMPORT_PROGRAM = 4;
	public static final int HEADER_IMPORT_SOURCE = 5;
	public static final int HEADER_TABLENAME = 6;
	public static final int HEADER_LASTEXPORT = 7;
	public static final int HEADER_NOTES = 8;

	private List<ProfileObject> tableData = new ArrayList<>();

	private JComponent parent;
	private Profiles objectData;
	private ProfileObject profile;

	private String[] columnNames = GUIFactory.getArray("mainTableHeaders");

	private boolean[] editable = { true, true, true, true, false, false, false, true, true };

	public ProjectModel(Profiles data) {
		objectData = data;

		for (String projectID : objectData.getProjects()) {
			objectData.setProject(projectID);
			for (String profileID : objectData.getProfiles(projectID)) {
				objectData.setProfile(profileID);
				tableData.add(new ProfileObject(projectID, profileID, data));
			}
		}
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return editable[col];
	}

	@Override
	public int getColumnCount() {
		return editable.length;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public int getRowCount() {
		return tableData.size();
	}

	public void setParent(JComponent parent) {
		this.parent = parent;
	}

	public void removeRecord(int row) {
		if (row != -1 && tableData.size() > row) {
			tableData.remove(row);
			fireTableRowsDeleted(row, row);
		}
	}

	public void removeRecord(ProfileObject obj) {
		tableData.remove(obj);
		fireTableDataChanged();
	}

	public void addRecord(String projectID, String profileID) {
		ProfileObject obj = new ProfileObject(projectID, profileID, objectData);
		tableData.add(obj);
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int row, int col) {
		profile = tableData.get(row);
		switch (col) {
		case HEADER_EDIT:
			return "...";
		case HEADER_PROJECT:
			return profile.getProjectID();
		case HEADER_PROFILE:
			return profile.getProfileID();
		case HEADER_EXPORTFILE:
			return profile.getExportFile();
		case HEADER_IMPORT_PROGRAM:
			return profile.getImportFileProgram();
		case HEADER_IMPORT_SOURCE:
			return profile.getImportFile();
		case HEADER_TABLENAME:
			return profile.getTableName();
		case HEADER_LASTEXPORT:
			return General.convertTimestamp2DB(profile.getLastModified(), General.sdInternalTimestamp);
		case HEADER_NOTES:
			return profile.getNotes();
		default:
			return "";
		}
	}

	public ProfileObject getProfileObject(int row) {
		profile = tableData.get(row);
		return profile;
	}

	public ProfileObject getProfileObject() {
		return profile;
	}

	public int getProfileIndex(String projectID, String profileID) {
		int result = 0;
		for (ProfileObject obj : tableData) {
			if (obj.getProjectID().equals(projectID) && obj.getProfileID().equals(profileID)) {
				return result;
			}
			result++;
		}
		return -1;
	}

	@Override
	public void setValueAt(Object object, int row, int col) {
		profile = tableData.get(row);
		profile.alignProfiles();

		String s = object == null ? "" : object.toString();
		switch (col) {
		case HEADER_PROFILE:
			if (!s.isEmpty() && !s.equals(objectData.getProfileID())) {
				try {
					objectData.renameCurrentNode(s);
					profile.setProfileID(s);
				} catch (Exception e) {
					General.errorMessage(parent, e, GUIFactory.getTitle("profileError"), null);
				}
				return;
			}
			break;
		case HEADER_LASTEXPORT:
			objectData.setLastModified(
					s.isEmpty() ? "" : General.convertTimestamp((LocalDateTime) object, General.sdInternalTimestamp));
			break;
		case HEADER_NOTES:
			objectData.setNotes(s);
			break;
		default:
			break;
		}

		profile.alignTable();
	}
}
