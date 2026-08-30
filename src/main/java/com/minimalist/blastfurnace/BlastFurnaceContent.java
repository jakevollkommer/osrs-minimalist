// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.blastfurnace;

import com.minimalist.ContentArea;
import com.minimalist.MinimalistConfig;
import com.minimalist.PlayerHiding;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;

/**
 * Everything Minimalist hides at the Blast Furnace. Entity hiding for now;
 * curated scenery sets will follow.
 */
public class BlastFurnaceContent implements ContentArea
{
	private final MinimalistConfig config;
	private final PlayerHiding playerHiding;

	private volatile boolean sceneIsBlastFurnace;

	public BlastFurnaceContent(Client client, MinimalistConfig config)
	{
		this.config = config;
		this.playerHiding = new PlayerHiding(client);
	}

	@Override
	public boolean isInScene(Scene scene)
	{
		return BlastFurnace.isInScene(scene);
	}

	@Override
	public boolean rebuildFromConfig()
	{
		playerHiding.rebuild(
			config.blastFurnaceOtherPlayers(),
			config.blastFurnaceOtherPlayers2d(),
			config.blastFurnaceOtherPlayersPets(),
			config.blastFurnaceShowFriends());
		// no static scenery hiding yet, so nothing needs a scene reload
		return false;
	}

	@Override
	public void reset()
	{
		playerHiding.reset();
	}

	@Override
	public void onSceneLoaded(Scene scene)
	{
		sceneIsBlastFurnace = BlastFurnace.isInScene(scene);
	}

	@Override
	public boolean hidesNpc(NPC npc)
	{
		return playerHiding.hidesPet(npc, sceneIsBlastFurnace);
	}

	@Override
	public boolean hidesPlayer(Player player, boolean drawingUi)
	{
		return playerHiding.hidesPlayer(player, drawingUi, sceneIsBlastFurnace);
	}
}
