package org.joverseer.support.readers.orders;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joverseer.game.Game;
import org.joverseer.game.TurnElementsEnum;
import org.joverseer.domain.*;
import org.joverseer.domain.Character;
import org.springframework.core.io.Resource;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.dialog.MessageDialog;


public class OrderFileReader {
	String orderFile;
	
	Game game;

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public String getOrderFile() {
		return orderFile;
	}

	public void setOrderFile(String orderFile) {
		this.orderFile = orderFile;
	}
	
	public void readOrders() {
		try {
			Resource resource = Application.instance().getApplicationContext().getResource(getOrderFile());
			BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
			String line;
			String charName = null;
			int orderI = 0;
			int i = 0;
			Pattern turnFileInfo = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+),([\\w ]+),.*");
			
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("ENDMEAUTOINPUT")) break;
				if (line.startsWith("BEGINMEAUTOINPUT")) continue;
				Matcher m = turnFileInfo.matcher(line);
				if (m.matches()) {
					int gameNo = Integer.parseInt(m.group(1));
					//TODO verify game number
					continue;
				}
				if (!line.equals("")) {
					String[] parts = line.split(",");
					if (!parts[0].equals(charName)) {
						orderI = 0;
					} else {
						orderI++;
					}
					charName = parts[0];
					String parameters = "";
					int orderNo = -1;
					if (!parts[1].equals("")) {
						orderNo = Integer.parseInt(parts[1]);
					
						for (int j=2; j<parts.length; j++) {
							String part = parts[j];
							if (!part.equals("--")) {
								parameters = parameters + (parameters.equals("") ? "" : " ") + part; 
							}
						}
					}
					Character c = (Character)getGame().getTurn().getContainer(TurnElementsEnum.Character).findFirstByProperty("id", charName);
					Order[] orders = c.getOrders();
					orders[orderI].setOrderNo(orderNo);
					orders[orderI].setParameters(parameters);
				}
				i++;
			}
		}
		catch (Exception exc) {
			MessageDialog dlg = new MessageDialog("Error", exc.getMessage());
			dlg.showDialog();
		}

	}
	
}
