package me.alexisevelyn.crewmate.enums.cosmetic;

import me.alexisevelyn.crewmate.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.ResourceBundle;

public enum Skin {
	NONE(0),
	ASTRONAUT(1),
	CAPTAIN(2),
	MECHANIC(3),
	MILITARY(4),
	POLICE(5),
	SCIENTIST(6),
	SUIT_BLACK(7),
	SUIT_WHITE(8),
	THE_WALL(9),
	HAZMAT(10),
	SECURITY(11),
	TARMAC(12),
	MINER(13),
	WINTER(14),
	ARCHAEOLOGIST(15);

	private final int skin;

	Skin(int skin) {
		this.skin = skin;
	}

	private static final java.util.Map<Integer, Skin> skinSearch = new HashMap<>();

	public int getSkin() {
		return this.skin;
	}

	@Nullable
	public static Skin getSkin(int skinInteger) {
		return skinSearch.get(skinInteger);
	}

	@NotNull
	public static String getSkinName(@NotNull Skin skin) {
		ResourceBundle translation = Main.getTranslationBundle();

		if (skin == null)
			return translation.getString("unknown");

		switch (skin) {
			case NONE:
				return translation.getString("skin_none");
			case ASTRONAUT:
				return translation.getString("skin_astronaut");
			case CAPTAIN:
				return translation.getString("skin_captain");
			case MECHANIC:
				return translation.getString("skin_mechanic");
			case MILITARY:
				return translation.getString("skin_military");
			case POLICE:
				return translation.getString("skin_police");
			case SCIENTIST:
				return translation.getString("skin_scientist");
			case SUIT_BLACK:
				return translation.getString("skin_suit_black");
			case SUIT_WHITE:
				return translation.getString("skin_suit_white");
			case THE_WALL:
				return translation.getString("skin_the_wall");
			case HAZMAT:
				return translation.getString("skin_hazmat");
			case SECURITY:
				return translation.getString("skin_security");
			case TARMAC:
				return translation.getString("skin_tarmac");
			case MINER:
				return translation.getString("skin_miner");
			case WINTER:
				return translation.getString("skin_winter");
			case ARCHAEOLOGIST:
				return translation.getString("skin_archaeologist");
			default:
				return translation.getString("unknown");
		}
	}

	static {
		for (Skin skinKey : Skin.values()) {
			skinSearch.put(skinKey.skin, skinKey);
		}
	}
}
