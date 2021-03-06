package org.joverseer.domain;

import java.io.Serializable;

import org.joverseer.ui.support.UIUtils;

/**
 * Enumeration for population center sizes
 * 
 * @author Marios Skounakis
 */
public enum PopulationCenterSizeEnum implements Serializable {
	ruins(0), camp(1), village(2), town(3), majorTown(4), city(5);

	private final int size;

	PopulationCenterSizeEnum(int size) {
		this.size = size;
	}

	public Class<PopulationCenterSizeEnum> getType() {
		return PopulationCenterSizeEnum.class; // To change body of implemented
		// methods use File | Settings |
		// File Templates.
	}

	@SuppressWarnings("boxing")
	public Integer getCode() {
		return this.size;
	}

	public static PopulationCenterSizeEnum getFromCode(int code) {
		for (PopulationCenterSizeEnum p : values()) {
			if (p.getCode() == code)
				return p;
		}
		return null;
	}

	public static PopulationCenterSizeEnum getFromLabel(String label) {
		for (PopulationCenterSizeEnum v : values()) {
			if (v.getLabel().equals(label))
				return v;
		}
		return null;
	}

	public String getLabel() {
		switch (this.size) {
		case 0:
			return "Ruins";
		case 1:
			return "Camp";
		case 2:
			return "Village";
		case 3:
			return "Town";
		case 4:
			return "Major Town";
		case 5:
			return "City";
		}
		return "";
	}

	public String getRenderString() {
		return UIUtils.enumToString(this);
	}

}
