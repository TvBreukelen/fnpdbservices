package fnprog2pda.dialog;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import application.dialog.BasicDialog;
import application.interfaces.FNPSoftware;
import application.utils.GUIFactory;
import fnprog2pda.model.MiscellaneousData;

public class ConfigMiscellaneous extends BasicDialog {
	/**
	 * Title: ScMiscellaneous Description: FNProgramvare Software Miscellaneous
	 * Configuration parms 
	 * Copyright (c) 2006-2020
	 *
	 * @author Tom van Breukelen
	 * @version 5.1
	 */
	private static final long serialVersionUID = -5470122199126408967L;
	private JCheckBox[] booleanFields;
	private FNPSoftware _software;
	private MiscellaneousData _data;
	boolean isSaved = false;

	public ConfigMiscellaneous(FNPSoftware softwareID, MiscellaneousData data) {
		super();
		_software = softwareID;
		_data = data;
		init();
	}

	@Override
	protected void init() {
		init(_data.getProfileID() + " " + GUIFactory.getTitle("miscSettings"));
		setHelpFile("miscellaneous_settings");
		buildDialog();
		pack();
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridLayout(3, 2));
		result.setBorder(BorderFactory.createEtchedBorder());

		final String[][] miscText = {
				{ "inclContentsAuthor", "inclAuthorRoles", "inclContentsOrigTitle", "useOrigTitle",
						"inclContentsItemTitle" },
				{ "inclTrackArtist", "inclArtistRoles", "inclTrackItemTitle", "inclTrackSide", "inclTrackIndex",
						"inclTrackLength" },
				{ "inclContentsItemTitle", "inclContentsSide", "inclContentsIndex", "inclContentsLength" } };
		final boolean[][] miscValues = {
				{ _data.isUseContentsPerson(), _data.isUseRoles(), _data.isUseContentsOrigTitle(),
						_data.isUseOriginalTitle(), _data.isUseContentsItemTitle() },
				{ _data.isUseContentsPerson(), _data.isUseRoles(), _data.isUseContentsItemTitle(),
						_data.isUseContentsSide(), _data.isUseContentsIndex(),
						_data.isUseContentsLength() },
				{ _data.isUseContentsSide(), _data.isUseContentsItemTitle(),
						_data.isUseContentsIndex(), _data.isUseContentsLength() } };

		int index = _software.ordinal() - 1;
		booleanFields = new JCheckBox[miscText[index].length];

		for (int i = 0; i < booleanFields.length; i++) {
			booleanFields[i] = GUIFactory.getJCheckBox(miscText[index][i], miscValues[index][i]);
			result.add(booleanFields[i]);
		}

		String table = _data.getTableName();
		switch (_software) {
		case BOOKCAT:
			if (table.equals("Contents")) {
				int[] exclContents = { 0, 2, 4 };
				for (int i : exclContents) {
					booleanFields[i].setEnabled(false);
					booleanFields[i].setSelected(false);
				}
			}
			break;
		case CATRAXX:
			boolean exclude = table.equals("Track") || table.equals("Playlist");
			if (!exclude) {
				break;
			}

			int[] exclContents = { 0, 2, 3, 4, 5 };
			if (table.equals("Playlist")) {
				exclContents = new int[] { 2, 3 };
			}

			for (int i : exclContents) {
				booleanFields[i].setEnabled(false);
				booleanFields[i].setSelected(false);
			}
		default:
			break;
		}

		return result;
	}

	@Override
	protected void save() throws Exception {
		switch (_software) {
		case BOOKCAT:
			_data.setUseContentsPerson(booleanFields[0].isSelected());
			_data.setUseRoles(booleanFields[1].isSelected());
			_data.setUseContentsOrigTitle(booleanFields[2].isSelected());
			_data.setUseOriginalTitle(booleanFields[3].isSelected());
			_data.setUseContentsItemTitle(booleanFields[4].isSelected());
			_data.setUseContentsSide(false);
			_data.setUseContentsIndex(false);
			_data.setUseContentsLength(false);
			break;
		case CATRAXX:
			_data.setUseContentsPerson(booleanFields[0].isSelected());
			_data.setUseRoles(booleanFields[1].isSelected());
			_data.setUseContentsItemTitle(booleanFields[2].isSelected());
			_data.setUseContentsSide(booleanFields[3].isSelected());
			_data.setUseContentsIndex(booleanFields[4].isSelected());
			_data.setUseContentsLength(booleanFields[5].isSelected());
			_data.setUseContentsOrigTitle(false);
			_data.setUseOriginalTitle(false);
			break;
		case CATVIDS:
			_data.setUseContentsItemTitle(booleanFields[0].isSelected());
			_data.setUseContentsSide(booleanFields[1].isSelected());
			_data.setUseContentsIndex(booleanFields[2].isSelected());
			_data.setUseContentsLength(booleanFields[3].isSelected());
			_data.setUseRoles(false);
			_data.setUseContentsOrigTitle(false);
			_data.setUseOriginalTitle(false);
			_data.setUseContentsPerson(false);
		default:
			break;
		}
	}
}
