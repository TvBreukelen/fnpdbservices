package application.model;

import java.time.LocalDateTime;
import java.util.Vector;

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
	public static final int HEADER_IMPORTFILEPROGRAM = 3;
	public static final int HEADER_TABLENAME = 4;
	public static final int HEADER_IMPORTFILE = 5;
	public static final int HEADER_EXPORTFILE = 6;
	public static final int HEADER_LASTEXPORT = 7;
	public static final int HEADER_NOTES = 8;

	private Vector<ProfileObject> _tableData = new Vector<>();

	private JComponent _parent;
	private Profiles _objectData;
	private ProfileObject _profile;

	private String[] columnNames = GUIFactory.getArray("mainTableHeaders");

	private boolean[] editable = { true, false, true, false, false, false, true, true, true };

	public ProjectModel(Profiles data) {
		_objectData = data;

		for (String projectID : _objectData.getProjects()) {
			_objectData.setProject(projectID);
			for (String profileID : _objectData.getProfiles(projectID)) {
				_objectData.setProfile(profileID);
				_tableData.add(new ProfileObject(projectID, profileID, data));
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
		return _tableData.size();
	}

	public void setParent(JComponent parent) {
		_parent = parent;
	}

	public void removeRecord(int row) {
		if (row != -1 && _tableData.size() > row) {
			_tableData.remove(row);
			fireTableRowsDeleted(row, row);
		}
	}

	public void removeRecord(ProfileObject obj) {
		_tableData.remove(obj);
		fireTableDataChanged();
	}

	public void addRecord(String projectID, String profileID) {
		ProfileObject obj = new ProfileObject(projectID, profileID, _objectData);
		_tableData.add(obj);
		fireTableDataChanged();
	}

	@Override
	public Object getValueAt(int row, int col) {
		_profile = _tableData.get(row);
		switch (col) {
		case HEADER_EDIT:
			return "...";
		case HEADER_PROJECT:
			return _profile.getProjectID();
		case HEADER_PROFILE:
			return _profile.getProfileID();
		case HEADER_IMPORTFILEPROGRAM:
			return _profile.getImportFileProgram();
		case HEADER_TABLENAME:
			return _profile.getTableName();
		case HEADER_IMPORTFILE:
			return _profile.getImportFile();
		case HEADER_EXPORTFILE:
			return _profile.getExportFile();
		case HEADER_LASTEXPORT:
			return General.convertDB2Timestamp(_profile.getLastModified());
		case HEADER_NOTES:
			return _profile.getNotes();
		}
		return "";
	}

	public ProfileObject getProfileObject(int row) {
		_profile = _tableData.get(row);
		return _profile;
	}

	public ProfileObject getProfileObject() {
		return _profile;
	}

	public int getProfileIndex(String projectID, String profileID) {
		int result = 0;
		for (ProfileObject obj : _tableData) {
			if (obj.getProjectID().equals(projectID) && obj.getProfileID().equals(profileID)) {
				return result;
			}
			result++;
		}
		return -1;
	}

	@Override
	public void setValueAt(Object object, int row, int col) {
		_profile = _tableData.get(row);
		_profile.alignProfiles();

		String s = object == null ? "" : object.toString();
		switch (col) {
		case HEADER_PROFILE:
			if (!s.isEmpty() && !s.equals(_objectData.getProfileID())) {
				try {
					_objectData.renameCurrentNode(s);
					_profile.setProfileID(s);
				} catch (Exception e) {
					General.errorMessage(_parent, e, GUIFactory.getTitle("profileError"), null);
				}
				return;
			}
			break;
		case HEADER_LASTEXPORT:
			_objectData.setLastModified(s.isEmpty() ? "" : General.convertTimestamp2DB((LocalDateTime) object));
			break;
		case HEADER_NOTES:
			_objectData.setNotes(s);
			break;
		}

		_profile.alignTable();
	}
}
