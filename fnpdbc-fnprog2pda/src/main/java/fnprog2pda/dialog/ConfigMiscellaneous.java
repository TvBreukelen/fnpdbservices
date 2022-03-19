package fnprog2pda.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import application.dialog.BasicDialog;
import application.interfaces.FNPSoftware;
import application.utils.GUIFactory;
import fnprog2pda.model.MiscellaneousData;

public class ConfigMiscellaneous extends BasicDialog {
	/**
	 * Title: ScMiscellaneous Description: FNProgramvare Software Miscellaneous
	 * Configuration parms Copyright (c) 2006-2020
	 *
	 * @author Tom van Breukelen
	 * @version 5.1
	 */
	private static final long serialVersionUID = -5470122199126408967L;
	private JCheckBox[] booleanFields;
	private FNPSoftware software;

	transient MiscellaneousData data;
	boolean isSaved = false;

	public ConfigMiscellaneous(FNPSoftware softwareID, MiscellaneousData data) {
		super();
		software = softwareID;
		this.data = data;
		init();
	}

	private void init() {
		init(data.getProfileID() + " " + GUIFactory.getTitle("miscSettings"));
		setHelpFile("miscellaneous_settings");
		buildDialog();
		pack();
	}

	@Override
	protected void buildDialog() {
		btSave.setVisible(false);
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		getContentPane().add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		getContentPane().add(createCenterPanel(), BorderLayout.CENTER);
		getContentPane().add(createBottomPanel(), BorderLayout.SOUTH);
	}

	@Override
	protected Component addToToolbar() {
		return GUIFactory.createToolBarButton(GUIFactory.getToolTip("funcRemoveMisc"), "Delete.png",
				e -> Arrays.stream(booleanFields).forEach(cb -> cb.setSelected(false)));
	}

	@Override
	protected Component createCenterPanel() {
		JPanel result = new JPanel(new GridLayout(3, 2));
		result.setBorder(BorderFactory.createEtchedBorder());

		final String[][] miscText = {
				{ "inclContentsAuthor", "inclAuthorRoles", "inclContentsOrigTitle", "useOrigTitle",
						"inclContentsItemTitle", "inclReleaseNo" },
				{ "inclTrackArtist", "inclArtistRoles", "inclTrackItemTitle", "inclTrackSide", "inclTrackIndex",
						"inclTrackLength" },
				{ "inclContentsItemTitle", "inclContentsSide", "inclContentsIndex", "inclContentsLength" } };
		final boolean[][] miscValues = {
				{ data.isUseContentsPerson(), data.isUseRoles(), data.isUseContentsOrigTitle(),
						data.isUseOriginalTitle(), data.isUseContentsItemTitle(), data.isUseReleaseNo() },
				{ data.isUseContentsPerson(), data.isUseRoles(), data.isUseContentsItemTitle(),
						data.isUseContentsSide(), data.isUseContentsIndex(), data.isUseContentsLength() },
				{ data.isUseContentsSide(), data.isUseContentsItemTitle(), data.isUseContentsIndex(),
						data.isUseContentsLength() } };

		int index = software.ordinal() - 1;
		booleanFields = new JCheckBox[miscText[index].length];

		for (int i = 0; i < booleanFields.length; i++) {
			booleanFields[i] = GUIFactory.getJCheckBox(miscText[index][i], miscValues[index][i]);
			result.add(booleanFields[i]);
		}

		String table = data.getTableName();
		switch (software) {
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
			break;
		default:
			break;
		}

		return result;
	}

	protected Component createBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(GUIFactory.getJButton("apply", funcSave));
		return panel;
	}

	@Override
	protected void save() throws Exception {
		switch (software) {
		case BOOKCAT:
			data.setUseContentsPerson(booleanFields[0].isSelected());
			data.setUseRoles(booleanFields[1].isSelected());
			data.setUseContentsOrigTitle(booleanFields[2].isSelected());
			data.setUseOriginalTitle(booleanFields[3].isSelected());
			data.setUseContentsItemTitle(booleanFields[4].isSelected());
			data.setUseReleaseNo(booleanFields[5].isSelected());
			data.setUseContentsSide(false);
			data.setUseContentsIndex(false);
			data.setUseContentsLength(false);
			break;
		case CATRAXX:
			data.setUseContentsPerson(booleanFields[0].isSelected());
			data.setUseRoles(booleanFields[1].isSelected());
			data.setUseContentsItemTitle(booleanFields[2].isSelected());
			data.setUseContentsSide(booleanFields[3].isSelected());
			data.setUseContentsIndex(booleanFields[4].isSelected());
			data.setUseContentsLength(booleanFields[5].isSelected());
			data.setUseContentsOrigTitle(false);
			data.setUseOriginalTitle(false);
			break;
		case CATVIDS:
			data.setUseContentsItemTitle(booleanFields[0].isSelected());
			data.setUseContentsSide(booleanFields[1].isSelected());
			data.setUseContentsIndex(booleanFields[2].isSelected());
			data.setUseContentsLength(booleanFields[3].isSelected());
			data.setUseRoles(false);
			data.setUseContentsOrigTitle(false);
			data.setUseOriginalTitle(false);
			data.setUseContentsPerson(false);
			break;
		default:
			break;
		}
	}
}
