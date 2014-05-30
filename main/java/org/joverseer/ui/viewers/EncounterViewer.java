package org.joverseer.ui.viewers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.joverseer.domain.Challenge;
import org.joverseer.domain.Encounter;
import org.joverseer.ui.support.Messages;
import org.joverseer.ui.support.commands.DialogsUtility;
import org.joverseer.ui.support.controls.PopupMenuActionListener;
import org.springframework.binding.form.FormModel;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.command.CommandGroup;
import org.springframework.richclient.image.ImageSource;
import org.springframework.richclient.layout.GridBagLayoutBuilder;

/**
 * Shows encounters in the Current Hex View
 * 
 * @author Marios Skounakis
 */
public class EncounterViewer extends ObjectViewer {

	public static final String FORM_PAGE = "encounterViewer"; //$NON-NLS-1$

	JTextField description;

	ActionCommand showDescriptionCommand = new ShowDescriptionCommand();

	public EncounterViewer(FormModel formModel) {
		super(formModel, FORM_PAGE);
	}

	@Override
	public boolean appliesTo(Object obj) {
		return Encounter.class.isInstance(obj) || Challenge.class.isInstance(obj);
	}

	@Override
	protected JComponent createFormControl() {
		GridBagLayoutBuilder glb = new GridBagLayoutBuilder();
		glb.setDefaultInsets(new Insets(0, 0, 0, 5));

		glb.append(this.description = new JTextField());
		this.description.setPreferredSize(new Dimension(200, 12));
		this.description.setBorder(null);

		ImageSource imgSource = (ImageSource) Application.instance().getApplicationContext().getBean("imageSource"); //$NON-NLS-1$
		JButton btnMenu = new JButton();
		Icon ico = new ImageIcon(imgSource.getImage("menu.icon")); //$NON-NLS-1$
		btnMenu.setPreferredSize(new Dimension(16, 16));
		btnMenu.setIcon(ico);
		glb.append(btnMenu);
		btnMenu.addActionListener(new PopupMenuActionListener() {

			@Override
			public JPopupMenu getPopupMenu() {
				return createEncounterPopupContextMenu();
			}
		});

		glb.nextLine();

		JPanel p = glb.getPanel();
		p.setBackground(Color.white);
		return p;
	}

	private JPopupMenu createEncounterPopupContextMenu() {
		CommandGroup group = Application.instance().getActiveWindow().getCommandManager().createCommandGroup("encounterCommandGroup", new Object[] { this.showDescriptionCommand }); //$NON-NLS-1$
		return group.createPopupMenu();
	}

	@Override
	public void setFormObject(Object obj) {
		super.setFormObject(obj);
		Encounter e = (Encounter) obj;
		this.description.setText(Messages.getString(Challenge.class.isInstance(obj) ? "EncounterViewer.ChallengeColon" :"EncounterViewer.EncounterColon",
				new Object [] {e.getCharacter()}));
		this.description.setCaretPosition(0);
	}

	private class ShowDescriptionCommand extends ActionCommand {
		@Override
		protected void doExecuteCommand() {
			Encounter e = (Encounter) getFormObject();
			DialogsUtility.showEncounterDescription(e);
		}

	}
}
