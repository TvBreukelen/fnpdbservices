package fnprog2pda.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import application.dialog.BasicDialog;
import application.utils.GUIFactory;
import fnprog2pda.model.MiscellaneousData;
import fnprog2pda.software.FNPSoftware;

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
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
		result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		boolean isTrack = false;
		boolean isContents = false;

		JPanel panel = new JPanel(new GridLayout(3, 2));
		panel.setBorder(BorderFactory.createTitledBorder(GUIFactory.getTitle("Include")));

		final String[][] miscText = {
				{ "inclContentsAuthor", "inclAuthorRoles", "inclContentsOrigTitle", "useOrigTitle",
						"inclContentsItemTitle", "inclReleaseNo" },
				{ "inclTrackArtist", "inclArtistRoles", "inclTrackItemTitle", "inclTrackSide", "inclTrackIndex",
						"inclTrackLength" },
				{ "inclContentsItemTitle", "inclContentsSide", "inclContentsSeason", "inclContentsIndex",
						"inclContentsLength", "inclCastRoles", "inclEntireCast" } };

		final boolean[][] miscValues = {
				{ data.isUseContentsPerson(), data.isUseRoles(), data.isUseContentsOrigTitle(),
						data.isUseOriginalTitle(), data.isUseContentsItemTitle(), data.isUseReleaseNo() },
				{ data.isUseContentsPerson(), data.isUseRoles(), data.isUseContentsItemTitle(),
						data.isUseContentsSide(), data.isUseContentsIndex(), data.isUseContentsLength() },
				{ data.isUseContentsSide(), data.isUseContentsItemTitle(), data.isUseSeason(),
						data.isUseContentsIndex(), data.isUseContentsLength(), data.isUseRoles(),
						data.isUseEntireCast() } };

		int index = software.ordinal() - 1;
		booleanFields = new JCheckBox[miscText[index].length];

		for (int i = 0; i < booleanFields.length; i++) {
			booleanFields[i] = GUIFactory.getJCheckBox(miscText[index][i], miscValues[index][i]);
			panel.add(booleanFields[i]);
		}

		String table = data.getTableName();
		int[] exclContents = new int[0];

		switch (software) {
		case BOOKCAT:
			if (table.equals("Contents")) {
				exclContents = new int[] { 0, 2, 4 };
			}
			break;
		case CATRAXX:
			isTrack = table.equals("Track");
			if (isTrack) {
				break;
			}

			if (table.equals("Playlist")) {
				exclContents = new int[] { 2, 3 };
			}
			break;
		case CATVIDS:
			isContents = table.equals("Contents");
			if (isContents) {
				exclContents = new int[] { 0, 1 };
				break;
			}
			exclContents = new int[] { 5, 6 };
			break;
		default:
			break;
		}

		for (int i : exclContents) {
			booleanFields[i].setEnabled(false);
			booleanFields[i].setSelected(false);
		}

		result.add(panel);
		return result;
	}

	protected Component createBottomPanel() {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		result.add(GUIFactory.getJButton("apply", funcSave));
		result.add(Box.createHorizontalStrut(4));
		return result;
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
			data.setUseSeason(false);
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
			data.setUseSeason(false);
			break;
		case CATVIDS:
			data.setUseContentsItemTitle(booleanFields[0].isSelected());
			data.setUseContentsSide(booleanFields[1].isSelected());
			data.setUseSeason(booleanFields[2].isSelected());
			data.setUseContentsIndex(booleanFields[3].isSelected());
			data.setUseContentsLength(booleanFields[4].isSelected());
			data.setUseRoles(booleanFields[5].isSelected());
			data.setUseEntireCast(booleanFields[6].isSelected());
			data.setUseContentsOrigTitle(false);
			data.setUseOriginalTitle(false);
			data.setUseContentsPerson(false);
			break;
		default:
			break;
		}
	}
}
