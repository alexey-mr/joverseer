package org.joverseer.ui.economyCalculator;

import java.text.DecimalFormat;

import javax.swing.JTable;

import org.joverseer.domain.EconomyCalculatorData;
import org.joverseer.domain.NationEconomy;
import org.joverseer.domain.ProductEnum;
import org.joverseer.domain.ProductPrice;
import org.joverseer.game.Game;
import org.joverseer.game.Turn;
import org.joverseer.support.GameHolder;
import org.joverseer.ui.LifecycleEventsEnum;
import org.joverseer.ui.support.JOverseerEvent;
import org.joverseer.ui.support.Messages;
import org.joverseer.ui.support.UIUtils;
import org.springframework.richclient.application.Application;

/**
 * Table model for the market actions of a nation for the Economy Calculator
 * 
 * @author Marios Skounakis
 */
public class MarketTableModel extends BaseEconomyTableModel {
	String[] rowHeaderTags = new String[] {"stores","production","availableToSell","potentialProfit","sellPrice","sellCount","sellPercent","availableMarket","butPrice",
			"buyCount","bidPrice","bidCount","costOrProfit"
	};
	// row 0 of the table
	String[] columnHeaders = new String[] { "", "le", "br", "st", "mi", "fo", "ti", "mo" };

	// column widths
	int[] columnWidths = new int[] { 170, 64, 64, 64, 64, 64, 64, 64 };
	JTable table;
	EconomyTotalsTableModel ettm;

	// column 0 of the table
	String[] rowHeaders;
	String[] columnNames;

	public MarketTableModel() {
		this.rowHeaders = new String[rowHeaderTags.length];
		for (int i=0;i<rowHeaderTags.length;i++) {
			rowHeaders[i] = Messages.getString("EconomyCalculator.Market." +rowHeaderTags[i]);
		}
		this.columnNames = new String[columnHeaders.length];
		columnNames[0] = "";
		ProductEnum product;
		for (int i=1;i<columnHeaders.length;i++) {
			this.columnNames[i] = (ProductEnum.getFromCode(this.columnHeaders[i])).getLocalized();
		}

	}


	public void setTable(JTable table) {
		this.table = table;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		EconomyCalculatorData ecd = getEconomyCalculatorData();
		String productCode = this.columnHeaders[columnIndex];
		ProductEnum product = ProductEnum.getFromCode(productCode);
		if (rowIndex == 5) {
			ecd.setSellUnits(product, (Integer) aValue);
			fireTableDataChanged();
			Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.EconomyCalculatorUpdate.toString(), this, this));
			select(rowIndex, columnIndex);
		}
		if (rowIndex == 6) {
			ecd.setSellPct(product, (Integer) aValue);
			fireTableDataChanged();
			Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.EconomyCalculatorUpdate.toString(), this, this));
			select(rowIndex, columnIndex);
		}
		if (rowIndex == 9) {
			ecd.setBuyUnits(product, (Integer) aValue);
			fireTableDataChanged();
			Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.EconomyCalculatorUpdate.toString(), this, this));
			select(rowIndex, columnIndex);
		}
		if (rowIndex == 10) {
			ecd.setBidPrice(product, (Integer) aValue);
			fireTableDataChanged();
			Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.EconomyCalculatorUpdate.toString(), this, this));
			select(rowIndex, columnIndex);
		}
		if (rowIndex == 11) {
			if (aValue != null && aValue.toString().startsWith("-")) {
				String tr = aValue.toString().substring(1);
				Integer newTr = Integer.parseInt(tr);
				int amt = getTotalsModel().getBuyAmountForTaxIncrease(newTr);
				if (amt > 0 && ecd.getBidPrice(product) > 0) {
					aValue = (int) Math.round((double) amt / (double) ecd.getBidPrice(product));
				} else {
					aValue = 0;
				}
			}
			ecd.setBidUnits(product, (Integer) aValue);
			fireTableDataChanged();
			Application.instance().getApplicationContext().publishEvent(new JOverseerEvent(LifecycleEventsEnum.EconomyCalculatorUpdate.toString(), this, this));
			select(rowIndex, columnIndex);
		}
	}

	protected void select(int rowIndex, int columnIndex) {
		try {
			this.table.setColumnSelectionInterval(columnIndex, columnIndex);
			this.table.setRowSelectionInterval(rowIndex, rowIndex);
		} catch (Exception exc) {
			// do nothing
		}
	}

	@Override
	public int getColumnCount() {
		return 8;
	}

	@Override
	public int getRowCount() {
		return 13;
	}

	@Override
	public String getColumnName(int column) {
		return this.columnNames[column];
	}

	public int getColumnWidth(int column) {
		return this.columnWidths[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0)
			return String.class;
		return Integer.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex > 0 && (rowIndex == 5 || rowIndex == 6 || rowIndex == 9 || rowIndex == 10 || rowIndex == 11);
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0)
			return this.rowHeaders[rowIndex];
		if (!Game.isInitialized(getGame()))
			return "";
		if (getGame().getTurn() == null)
			return "";
		NationEconomy ne = getNationEconomy();
		if (ne == null)
			return "";
		EconomyCalculatorData ecd = getEconomyCalculatorData();
		if (ecd == null)
			return "";
		if (!ecd.isInitialized())
			return "";
		String productCode = this.columnHeaders[columnIndex];
		ProductEnum product = ProductEnum.getFromCode(productCode);
		if (rowIndex == 0) {
			return ecd.getStores(product);
		}
		if (rowIndex == 1) {
			return ecd.getProduction(product);
		}
		if (rowIndex == 2) {
			return ecd.getTotal(product);
		}
		if (rowIndex == 3) {
			// TODO include sell bonus
			return ecd.getTotal(product) * ecd.getSellPrice(product);
		}
		if (rowIndex == 4) {
			return ecd.getSellPrice(product);
		}
		if (rowIndex == 5) {
			return ecd.getSellUnits(product);
		}
		if (rowIndex == 6) {
			return ecd.getSellPct(product);
		}
		if (rowIndex == 7) {
			return ecd.getMarketTotal(product);
		}
		if (rowIndex == 8) {
			return ecd.getBuyPrice(product);
		}
		if (rowIndex == 9) {
			return ecd.getBuyUnits(product);
		}
		if (rowIndex == 10) {
			return ecd.getBidPrice(product);
		}
		if (rowIndex == 11) {
			return ecd.getBidUnits(product);
		}
		if (rowIndex == 12) {
			return ecd.getMarketProfits(product);
		}
		return "";
	}

	public String getPriceHistory(int columnIndex, int numberOfTurns) {
		if (columnIndex == 0)
			return "";
		String productCode = this.columnHeaders[columnIndex];
		ProductEnum product = ProductEnum.getFromCode(productCode);
		return getPriceHistory(product, numberOfTurns);
	}

	public String getPriceHistory(ProductEnum product, int numberOfTurns) {
		if (!GameHolder.hasInitializedGame())
			return null;
		Game g = GameHolder.instance().getGame();
		if (g.getCurrentTurn() == 0)
			return null;
		String ret = "<html><b>" + Messages.getString("EconomyCalculator.PriceHistory.title", new String[] {UIUtils.enumToString(product)})
				+ "</b><br/><table><tr><th>"
				+ Messages.getString("EconomyCalculator.PriceHistory.turn")
				+ "</th><th>"
				+ Messages.getString("EconomyCalculator.PriceHistory.sell")
				+ "</th><th>"
				+ Messages.getString("EconomyCalculator.PriceHistory.buy")
				+ "</th><th>"
				+ Messages.getString("EconomyCalculator.PriceHistory.available")
				+ "</th></tr>";
		int maxSellPrice = 0;
		int minSellPrice = 1000;
		int maxBuyPrice = 0;
		int minBuyPrice = 1000;
		int turnCount = 0;
		DecimalFormat df = new DecimalFormat("###,###,##0");
		for (int i = g.getCurrentTurn() - 1; i > 0; i--) {
			Turn t = g.getTurn(i);
			if (t == null)
				continue;
			ProductPrice pp = t.getProductPrice(product);
			if (turnCount < numberOfTurns) {
				ret += "<tr><td align=center>" + i + "</td><td align=center>" + pp.getSellPrice() + "</td><td align=center>" + pp.getBuyPrice() + "</td><td align=center>" + df.format(pp.getMarketTotal()) + "</td></tr>";
			}
			turnCount++;
			maxSellPrice = Math.max(maxSellPrice, pp.getSellPrice());
			minSellPrice = Math.min(minSellPrice, pp.getSellPrice());
			maxBuyPrice = Math.max(maxBuyPrice, pp.getBuyPrice());
			minBuyPrice = Math.min(minBuyPrice, pp.getBuyPrice());
		}
		if (turnCount > 0) {
			ret += "</tr></table>"
					+ Messages.getString("EconomyCalculator.PriceHistory.sellPriceRange",new Object[] {minSellPrice,maxSellPrice})
					+ "<br/>"
					+ Messages.getString("EconomyCalculator.PriceHistory.buyPriceRange",new Object[] {minBuyPrice,maxBuyPrice})
					+ "<br/></html>";
		}
		return ret;
	}

	public EconomyTotalsTableModel getTotalsModel() {
		return this.ettm;
	}

	public void setTotalsModel(EconomyTotalsTableModel ettm) {
		this.ettm = ettm;
	}

}
