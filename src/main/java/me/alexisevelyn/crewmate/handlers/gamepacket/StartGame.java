package me.alexisevelyn.crewmate.handlers.gamepacket;

import me.alexisevelyn.crewmate.GameCodeHelper;
import me.alexisevelyn.crewmate.PacketHelper;
import me.alexisevelyn.crewmate.enums.Language;
import me.alexisevelyn.crewmate.enums.Map;
import me.alexisevelyn.crewmate.enums.TerminalColors;
import me.alexisevelyn.crewmate.enums.hazel.SendOption;
import me.alexisevelyn.crewmate.exceptions.InvalidBytesException;
import me.alexisevelyn.crewmate.exceptions.InvalidGameCodeException;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class StartGame {
	public static byte[] getNewGameSettings(DatagramPacket packet) {
		// 0000   01 00 02 2b 00 00 2a 02 0a 00 01 00 00 00 00 00   ...+..*.........
		// 0010   80 3f 00 00 40 3f 00 00 80 3f 00 00 f0 41 01 01   .?..@?...?...A..
		// 0020   03 01 00 00 00 02 00 00 00 00 00 87 00 00 00 00   ................
		// 0030   0f                                                .

		// 0000   01 00 12 05 00 01 00 00 00 00 07                  ...........

		byte[] buffer = packet.getData();

		// Data
		int maxPlayers = buffer[8];
		int map = buffer[13];
		int imposterCount = buffer[37];
		Language language = Language.getLanguage(Language.convertToInt(buffer[9], buffer[10]));

		String mapName = Map.getMapName(Map.getMap(map));
		String languageName = Language.getLanguageName(language);

		String extraData = "Max Players: " + maxPlayers + "\n" +
				"Map: " + mapName + "\n" +
				"Imposters: " + imposterCount + "\n" +
				"Language: " + languageName;

		System.out.println(extraData);

		return getRandomGameCode();
	}

	// For S->C
	private static byte[] getRandomGameCode() {
		// Game Code - AMLQTQ (89:5a:2a:80) - Purple - Goggles - Private - 1/10 Players
		// S->C - 0000   01 00 01 04 00 00 89 5a 2a 80                     .......Z*.

		// Game Code - SZGEYQ (c3:41:38:80) - Purple - Goggles - Private - 1/10 Players
		// S->C - 0000   01 00 01 04 00 00 c3 41 38 80                     .......A8.

		// Game Code - SIXLXQ (45:9a:17:80) - Red - Goggles - Private - 1/10 Players
		// S->C - 0000   01 00 01 04 00 00 45 9a 17 80                     ......E...

		byte[] header = new byte[] {SendOption.RELIABLE.getSendOption(), 0x00, 0x01, 0x04, 0x00, 0x00};
		byte[] message = new byte[] {0x3b, (byte) 0xbe, 0x25, (byte) 0x8c}; // Game Code - ABCDEF (3b:be:25:8c)

		// The client will respond with a packet that triggers handleJoinPrivateGame(DatagramPacket);
		return PacketHelper.getCombinedReply(header, message);
	}

	// This gets called when the client either tries to join a game or create a game.
	// For C->S
	public static byte[] getClientGameCode(DatagramPacket packet) {
		// Game Code - AMLQTQ (89:5a:2a:80) - Purple - Goggles - Private - 1/10 Players
		// C->S - 0000   01 00 03 05 00 01 89 5a 2a 80 07                  .......Z*..

		// Game Code - SZGEYQ (c3:41:38:80) - Purple - Goggles - Private - 1/10 Players
		// C->S - 0000   01 00 03 05 00 01 c3 41 38 80 07                  .......A8..

		// Game Code - SIXLXQ (45:9a:17:80) - Red - Goggles - Private - 1/10 Players
		// C->S - 0000   01 00 03 05 00 01 45 9a 17 80 07                  ......E....

		if (packet.getLength() != 11)
			return new byte[0];

		byte[] buffer = packet.getData();
		byte[] gameCodeBytes = new byte[4];

		// Extract Game Code Bytes From Buffer
		System.arraycopy(buffer, 6, gameCodeBytes, 0, 4);

		String gameCode;
		try {
			gameCode = GameCodeHelper.parseGameCode(gameCodeBytes);
		} catch (InvalidBytesException e) {
			System.out.println("Game Code Exception: " + e.getMessage());
			e.printStackTrace();

			return PacketHelper.closeWithMessage("Server side error with reading game code!!!");
		} catch (InvalidGameCodeException e) {
			return PacketHelper.closeWithMessage(e.getMessage());
		}

		// System.out.println("Game Code (Byte Form): " + Arrays.toString(gameCodeBytes));
		System.out.println("Game Code (Integer Form): " + gameCode);

		// Game Code - NPGWQQ (cd:98:00:80) - Red - Goggles - Private - 1/10 Players
		// C->S - 0000   01 00 03 05 00 01 cd 98 00 80 07                  ...........
		// S->C - 0000   01 00 02 0d 00 07 cd 98 00 80 f5 e9 1e 00 f5 e9   ................
		// S->C - 0010   1e 00 00 06 00 0a cd 98 00 80 01 00               ............

		// Game Code - TVJUXQ (0c:0e:1b:80) - Red - Goggles - Private - 1/10 Players
		// C->S - 0000   01 00 03 05 00 01 0c 0e 1b 80 07                  ...........
		// S->C - 0000   01 00 02 0d 00 07 0c 0e 1b 80 94 04 02 00 94 04   ................
		// S->C - 0010   02 00 00 06 00 0a 0c 0e 1b 80 01 00               ............

		byte unknown = 0x00;

		byte[] header = new byte[] {SendOption.RELIABLE.getSendOption(), 0x00, 0x02, 0x0d, 0x00, 0x07};
		byte[] headerWithGameCode = PacketHelper.getCombinedReply(header, gameCodeBytes);

		byte[] messagePartOne = new byte[] {unknown, unknown, unknown, 0x00, unknown, unknown, unknown, 0x00, 0x00, 0x06, 0x00, 0x0a};
		byte[] messagePartTwo = new byte[] {0x01, 0x00};

		byte[] messagePartThree = PacketHelper.getCombinedReply(gameCodeBytes, messagePartTwo);
		byte[] message = PacketHelper.getCombinedReply(messagePartOne, messagePartThree);

		// This is enough to get to the lobby
		return PacketHelper.getCombinedReply(headerWithGameCode, message);
	}

	public static byte[] getInitialGameSettings(DatagramPacket packet) {
		// TODO: Figure out what this packet is for!!!

		// Username Hi
		// C->S - 0000   01 00 04 ac 00 05 00 00 00 00 0c 00 04 02 fe ff   ................
		// C->S - 0010   ff ff 0f 00 01 01 00 00 01 12 00 04 03 fe ff ff   ................
		// C->S - 0020   ff 0f 00 02 02 01 00 01 00 03 01 00 01 00 1c 00   ................
		// C->S - 0030   04 04 00 01 03 04 02 00 01 01 00 05 00 00 01 06   ................
		// C->S - 0040   0a 00 01 00 00 ff 7f ff 7f ff 7f ff 7f 31 00 02   .............1..
		// C->S - 0050   04 02 2e 04 0a 01 00 00 00 00 00 00 80 3f 00 00   .............?..
		// C->S - 0060   80 3f 00 00 c0 3f 00 00 34 42 01 01 02 01 00 00   .?...?..4B......
		// C->S - 0070   00 01 01 0f 00 00 00 78 00 00 00 01 0f 01 01 00   .......x........
		// C->S - 0080   00 05 00 02 04 06 02 48 69 03 00 02 04 08 00 03   .......Hi.......
		// C->S - 0090   00 02 04 11 00 03 00 02 04 09 2b 03 00 02 04 0a   ..........+.....
		// C->S - 00a0   00 0e 00 02 02 1e 09 00 00 02 48 69 00 00 00 00   ..........Hi....
		// C->S - 00b0   00 00                                             ..

		// Username Alexis
		// C->S - 0000   01 00 04 b4 00 05 00 00 00 00 0c 00 04 02 fe ff   ................
		// C->S - 0010   ff ff 0f 00 01 01 00 00 01 12 00 04 03 fe ff ff   ................
		// C->S - 0020   ff 0f 00 02 02 01 00 01 00 03 01 00 01 00 1c 00   ................
		// C->S - 0030   04 04 00 01 03 04 02 00 01 01 00 05 00 00 01 06   ................
		// C->S - 0040   0a 00 01 00 00 ff 7f ff 7f ff 7f ff 7f 31 00 02   .............1..
		// C->S - 0050   04 02 2e 04 0a 01 00 00 00 00 00 00 80 3f 00 00   .............?..
		// C->S - 0060   80 3f 00 00 c0 3f 00 00 34 42 01 01 02 01 00 00   .?...?..4B......
		// C->S - 0070   00 01 01 0f 00 00 00 78 00 00 00 01 0f 01 01 00   .......x........
		// C->S - 0080   00 09 00 02 04 06 06 41 6c 65 78 69 73 03 00 02   .......Alexis...
		// C->S - 0090   04 08 00 03 00 02 04 11 00 03 00 02 04 09 2b 03   ..............+.
		// C->S - 00a0   00 02 04 0a 00 12 00 02 02 1e 0d 00 00 06 41 6c   ..............Al
		// C->S - 00b0   65 78 69 73 00 00 00 00 00 00                     exis......

		// Username Alexis - Anonymous Votes On and Recommended Off
		// C->S - 0000   01 00 04 b4 00 05 00 00 00 00 0c 00 04 02 fe ff   ................
		// C->S - 0010   ff ff 0f 00 01 01 00 00 01 12 00 04 03 fe ff ff   ................
		// C->S - 0020   ff 0f 00 02 02 01 00 01 00 03 01 00 01 00 1c 00   ................
		// C->S - 0030   04 04 00 01 03 04 02 00 01 01 00 05 00 00 01 06   ................
		// C->S - 0040   0a 00 01 00 00 ff 7f ff 7f ff 7f ff 7f 31 00 02   .............1..
		// C->S - 0050   04 02 2e 04 0a 01 00 00 00 00 00 00 80 3f 00 00   .............?..
		// C->S - 0060   80 3f 00 00 c0 3f 00 00 34 42 01 01 02 01 00 00   .?...?..4B......
		// C->S - 0070   00 01 01 0f 00 00 00 78 00 00 00 00 0f 01 01 01   .......x........
		// C->S - 0080   00 09 00 02 04 06 06 41 6c 65 78 69 73 03 00 02   .......Alexis...
		// C->S - 0090   04 08 08 03 00 02 04 11 00 03 00 02 04 09 2c 03   ..............,.
		// C->S - 00a0   00 02 04 0a 00 12 00 02 02 1e 0d 00 00 06 41 6c   ..............Al
		// C->S - 00b0   65 78 69 73 08 00 00 00 00 00                     exis......

		// Username Alexis - Everything Changed From Default
		// C->S - 0000   01 00 04 b4 00 05 00 00 00 00 0c 00 04 02 fe ff   ................
		// C->S - 0010   ff ff 0f 00 01 01 00 00 01 12 00 04 03 fe ff ff   ................
		// C->S - 0020   ff 0f 00 02 02 01 00 01 00 03 01 00 01 00 1c 00   ................
		// C->S - 0030   04 04 00 01 03 04 02 00 01 01 00 05 00 00 01 06   ................
		// C->S - 0040   0a 00 01 00 00 ff 7f ff 7f ff 7f ff 7f 31 00 02   .............1..
		// C->S - 0050   04 02 2e 04 0a 01 00 00 00 00 00 00 40 40 00 00   ............@@..
		// C->S - 0060   a0 40 00 00 a0 40 00 00 70 42 02 03 05 09 00 00   .@...@..pB......
		// C->S - 0070   00 01 02 78 00 00 00 2c 01 00 00 00 3c 00 00 01   ...x...,....<...
		// C->S - 0080   01 09 00 02 04 06 06 41 6c 65 78 69 73 03 00 02   .......Alexis...
		// C->S - 0090   04 08 08 03 00 02 04 11 00 03 00 02 04 09 2c 03   ..............,.
		// C->S - 00a0   00 02 04 0a 00 12 00 02 02 1e 0d 00 00 06 41 6c   ..............Al
		// C->S - 00b0   65 78 69 73 08 00 00 00 00 00                     exis......

		if (packet.getLength() < 4)
			return new byte[0];

		byte[] buffer = packet.getData();

		// Must Equal 01 00 03 (Join Game Via Code) or 01 00 04 (Create Game)
		if (!(buffer[0] == SendOption.RELIABLE.getSendOption() && buffer[1] == 0x00) || !(buffer[2] == 0x04 || buffer[2] == 0x03))
			return new byte[0];

		byte unknown = buffer[3]; // 180 for Alexis and 172 for Hi - +8
		byte unknownTwo = buffer[129]; // 9 for Alexis and 5 for Hi - +4

		byte nameLength = buffer[134];

		// Extract Name Bytes From Buffer
		byte[] nameBytes = new byte[nameLength];
		System.arraycopy(buffer, 135, nameBytes, 0, nameLength);

		byte unknownThree = buffer[159 + nameLength]; // Probably Associated With nameLength (Is 18 for Alexis and 14 for Hi) - +4
		byte unknownFour = buffer[164 + nameLength]; // Is 13 for Alexis and 9 for Hi - +4

		byte nameLengthTwo = buffer[167 + nameLength];
		byte[] nameBytesTwo = new byte[nameLength];
		System.arraycopy(buffer, 168 + nameLength, nameBytesTwo, 0, nameLengthTwo);

		System.out.println(TerminalColors.ANSI_TEXT_RED);
		System.out.println("Unknown: " + unknown);
		System.out.println("Unknown 2: " + unknownTwo);
		System.out.println("Unknown 3: " + unknownThree);
		System.out.println("Unknown 4: " + unknownFour);

		System.out.println("Name: " + new String(nameBytes, StandardCharsets.UTF_8));
		System.out.println("Name 2: " + new String(nameBytesTwo, StandardCharsets.UTF_8));
		System.out.println(TerminalColors.ANSI_RESET);

		return new byte[0];
	}
}
