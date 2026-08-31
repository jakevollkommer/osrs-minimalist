// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.content;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;

/**
 * Hiding of other players and their pets, shared by every content area that offers it.
 * Each area owns its own config toggles and scene gate; this class owns the decision
 * logic, including the friends exemption.
 *
 * Fields are volatile because addEntity consults them every frame.
 */
public class PlayerHiding
{
	private final Client client;

	private volatile boolean hideOtherPlayers;
	private volatile boolean hideOtherPlayers2d;
	private volatile boolean hidePets;
	private volatile boolean showFriends;

	public PlayerHiding(Client client)
	{
		this.client = client;
	}

	public void rebuild(boolean hideOtherPlayers, boolean hideOtherPlayers2d, boolean hidePets, boolean showFriends)
	{
		this.hideOtherPlayers = hideOtherPlayers;
		this.hideOtherPlayers2d = hideOtherPlayers2d;
		this.hidePets = hidePets;
		this.showFriends = showFriends;
	}

	public void reset()
	{
		hideOtherPlayers = false;
		hideOtherPlayers2d = false;
		hidePets = false;
	}

	/** atContent is the calling area's own scene gate. */
	public boolean hidesPlayer(Player player, boolean drawingUi, boolean atContent)
	{
		boolean isOtherPlayerAtContent = atContent && player != client.getLocalPlayer();
		if (!isOtherPlayerAtContent)
		{
			return false;
		}

		if (showFriends && player.isFriend())
		{
			return false;
		}

		return drawingUi ? hideOtherPlayers2d : hideOtherPlayers;
	}

	public boolean hidesPet(NPC npc, boolean atContent)
	{
		return hidePets && atContent && isSomeoneElsesPet(npc);
	}

	private boolean isSomeoneElsesPet(NPC npc)
	{
		return npc.getComposition().isFollower() && npc != client.getFollower();
	}
}
