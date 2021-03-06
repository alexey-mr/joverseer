package org.joverseer.ui.listviews;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.joverseer.game.Game;
import org.joverseer.metadata.domain.Nation;
import org.joverseer.support.GameHolder;
import org.joverseer.support.infoSources.InfoSource;
import org.joverseer.support.infoSources.MetadataSource;
import org.joverseer.support.infoSources.XmlTurnInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromLocateArtifactInfoSource;
import org.joverseer.support.infoSources.spells.DerivedFromLocateArtifactTrueInfoSource;
import org.joverseer.tools.infoCollectors.artifacts.ArtifactInfoCollector;
import org.joverseer.tools.infoCollectors.artifacts.ArtifactWrapper;
import org.joverseer.ui.listviews.filters.AllegianceFilter;
import org.joverseer.ui.listviews.filters.HexFilter;
import org.joverseer.ui.listviews.filters.NationFilter;
import org.joverseer.ui.listviews.filters.TextFilter;
import org.joverseer.ui.listviews.filters.TurnFilter;
import org.joverseer.ui.listviews.renderers.InfoSourceTableCellRenderer;
import org.joverseer.ui.support.controls.JLabelButton;
import org.joverseer.ui.support.controls.PopupMenuActionListener;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.command.CommandGroup;
import org.springframework.richclient.image.ImageSource;
import org.springframework.richclient.table.SortableTableModel;

/**
 * The advanced artifact information tab Shows ArtifactWrappers from the
 * ArtifactInfoCollector
 * 
 * @author Marios Skounakis
 */
public class AdvancedArtifactListView extends BaseItemListView {

	/**
	 * Filter based no the artifact power
	 * 
	 * @author Marios Skounakis
	 */
	class ArtifactPowerFilter extends AbstractListViewFilter {

		String powerStr;

		public ArtifactPowerFilter(String descr, String power) {
			super(descr);
			this.powerStr = power;
		}

		@Override
		public boolean accept(Object obj) {
			if (this.powerStr == null)
				return true;
			ArtifactWrapper aw = (ArtifactWrapper) obj;
			return (aw.getPower1().indexOf(this.powerStr) > -1 || aw.getPower2().indexOf(this.powerStr) > -1);
		}
	}

	class CopyToClipboardCommand extends ActionCommand implements ClipboardOwner {

		String DELIM = "\t";
		String NL = "\n";
		Game game;

		@Override
		protected void doExecuteCommand() {
			this.game = GameHolder.instance().getGame();
			String txt = "";
			for (int j = 0; j < AdvancedArtifactListView.this.tableModel.getDataColumnCount(); j++) {
				txt += (txt.equals("") ? "" : this.DELIM) + AdvancedArtifactListView.this.tableModel.getDataColumnHeaders()[j];
				if (j == 2) {
					// duplicate column "nation"
					txt += (txt.equals("") ? "" : this.DELIM) + AdvancedArtifactListView.this.tableModel.getDataColumnHeaders()[j];
				}
			}
			txt += this.NL;
			for (int i = 0; i < AdvancedArtifactListView.this.tableModel.getRowCount(); i++) {
				int idx = ((SortableTableModel) AdvancedArtifactListView.this.table.getModel()).convertSortedIndexToDataIndex(i);
				ArtifactWrapper aw = (ArtifactWrapper) AdvancedArtifactListView.this.tableModel.getRow(idx);
				txt += getRow(aw) + this.NL;
			}
			StringSelection stringSelection = new StringSelection(txt);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, this);
		}

		private String getRow(ArtifactWrapper aw) {
			Nation n = aw.getNationNo() == null ? null : this.game.getMetadata().getNationByNum(aw.getNationNo());
			String nationName = n == null || n.getNumber() == 0 ? "" : n.getShortName();
			return aw.getNumber() + this.DELIM + aw.getName() + this.DELIM + aw.getNationNo() + this.DELIM + nationName + this.DELIM + aw.getOwner() + this.DELIM + aw.getHexNo() + this.DELIM + aw.getAlignment() + this.DELIM + aw.getPower1() + this.DELIM + aw.getPower2() + this.DELIM + aw.getTurnNo() + this.DELIM + InfoSourceTableCellRenderer.getInfoSourceDescription(aw.getInfoSource()) + this.NL;
		}

		@Override
		public void lostOwnership(Clipboard arg0, Transferable arg1) {
		}
	}

	/**
	 * Filter based on the class of the info source
	 * 
	 * @author Marios Skounakis
	 */
	class InfoSourceClassFilter extends AbstractListViewFilter {

		Class<InfoSource>[] classes;

		public InfoSourceClassFilter(String descr, Class<InfoSource>[] classes) {
			super(descr);
			this.classes = classes;
		}

		@Override
		public boolean accept(Object obj) {
			if (this.classes == null)
				return true;
			InfoSource is = ((ArtifactWrapper) obj).getInfoSource();
			for (Class<InfoSource> c : this.classes) {
				if (c.isInstance(is))
					return true;
			}
			return false;
		}
	}

	/**
	 * Filter for owned/not owned artifacts
	 * 
	 * @author Marios Skounakis
	 */
	class OwnedArtifactFilter extends AbstractListViewFilter {

		Boolean owned;

		public OwnedArtifactFilter(String descr, Boolean owned) {
			super(descr);
			this.owned = owned;
		}

		@Override
		public boolean accept(Object obj) {
			ArtifactWrapper aw = (ArtifactWrapper) obj;
			if (this.owned == null)
				return true;
			if (this.owned) {
				return aw.getOwner() != null && !aw.getOwner().equals("");
			} else {
				return aw.getOwner() == null || aw.getOwner().equals("");
			}
		}
	}

	public AdvancedArtifactListView() {
		super(AdvancedArtifactTableModel.class);
	}

	@Override
	protected int[] columnWidths() {
		return new int[] { 32, 96, 48, 132, 48, 48, 120, 120, 48, 120 };
	}

	@Override
	protected JComponent[] getButtons() {
		ArrayList<JComponent> comps = new ArrayList<JComponent>();
		comps.addAll(Arrays.asList(super.getButtons()));
		JLabelButton popupMenu = new JLabelButton();
		ImageSource imgSource = (ImageSource) Application.instance().getApplicationContext().getBean("imageSource");
		Icon ico = new ImageIcon(imgSource.getImage("menu.icon"));
		popupMenu.setIcon(ico);
		popupMenu.addActionListener(new PopupMenuActionListener() {

			@Override
			public JPopupMenu getPopupMenu() {
				CommandGroup group = Application.instance().getActiveWindow().getCommandManager().createCommandGroup("advancedArtifactListViewCommandGroup", new Object[] { new CopyToClipboardCommand(), });
				return group.createPopupMenu();
			}
		});
		comps.add(popupMenu);
		return comps.toArray(new JComponent[] {});
	}

	@SuppressWarnings("unchecked")
	@Override
	protected AbstractListViewFilter[][] getFilters() {
		ArrayList<AbstractListViewFilter> filters1 = new ArrayList<AbstractListViewFilter>();
		filters1.addAll(Arrays.asList(NationFilter.createNationFilters()));
		filters1.addAll(Arrays.asList(AllegianceFilter.createAllegianceFilters()));
		filters1.add(new OwnedArtifactFilter("Owned", true));
		filters1.add(new OwnedArtifactFilter("Not Owned", false));
		return new AbstractListViewFilter[][] { filters1.toArray(new AbstractListViewFilter[] {}), TurnFilter.createTurnFiltersCurrentTurnAndAllTurns(), new AbstractListViewFilter[] { new InfoSourceClassFilter("All sources", null), new InfoSourceClassFilter("LA/LAT", new Class[] { DerivedFromLocateArtifactInfoSource.class, DerivedFromLocateArtifactTrueInfoSource.class }), new InfoSourceClassFilter("Xml/Pdf", new Class[] { XmlTurnInfoSource.class }), new InfoSourceClassFilter("Starting", new Class[] { MetadataSource.class }), },
				new AbstractListViewFilter[] { new ArtifactPowerFilter("All Powers", null), new ArtifactPowerFilter("Combat", "Combat "), new ArtifactPowerFilter("Agent", "Agent "), new ArtifactPowerFilter("Command", "Command "), new ArtifactPowerFilter("Stealth", "Stealth "), new ArtifactPowerFilter("Mage", "Mage "), new ArtifactPowerFilter("Emissary", "Emissary "), new ArtifactPowerFilter("Scrying", "Scry"), new ArtifactPowerFilter("Curse", "Spirit Mastery"), new ArtifactPowerFilter("Conjuring", "Conjuring Ways"), new ArtifactPowerFilter("Teleport", " Teleport") } };
	}

	@Override
	protected AbstractListViewFilter getTextFilter(String txt) {
		if (txt == null || txt.equals(""))
			return super.getTextFilter(txt);
		try {
			int hexNo = Integer.parseInt(txt.trim());
			return new HexFilter("", hexNo);
		} catch (Exception exc) {
			// do nothing
		}
		return new TextFilter("Name", "name", txt);
	}

	@Override
	protected boolean hasTextFilter() {
		return true;
	}

	@Override
	protected void setItems() {
		Game g = ((GameHolder) Application.instance().getApplicationContext().getBean("gameHolder")).getGame();
		if (!Game.isInitialized(g))
			return;
		ArrayList<ArtifactWrapper> aws = ArtifactInfoCollector.instance().getWrappersForTurn(g.getCurrentTurn());
		ArrayList<ArtifactWrapper> filteredItems = new ArrayList<ArtifactWrapper>();
		AbstractListViewFilter filter = getActiveFilter();
		for (ArtifactWrapper obj : aws) {
			if (filter == null || filter.accept(obj)) {
				filteredItems.add(obj);
			}
		}
		this.tableModel.setRows(filteredItems);
	}
}
