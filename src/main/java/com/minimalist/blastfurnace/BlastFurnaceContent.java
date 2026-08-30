// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.blastfurnace;

import com.minimalist.ContentArea;
import com.minimalist.IdSets;
import com.minimalist.MinimalistConfig;
import com.minimalist.ObjectActions;
import com.minimalist.PlayerHiding;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;

/**
 * Everything Minimalist hides at the Blast Furnace: operator dwarves, machinery,
 * smoke, optional equipment and fixtures, and other players and their pets.
 *
 * All fields are volatile because hidesObject is also called from the maploader
 * thread during scene upload.
 */
public class BlastFurnaceContent implements ContentArea
{
	private final MinimalistConfig config;
	private final PlayerHiding playerHiding;

	private volatile Set<Integer> hiddenObjectIds = Set.of();
	private volatile Set<Integer> hiddenNpcIds = Set.of();
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
		Set<Integer> previousObjectIds = hiddenObjectIds;

		hiddenObjectIds = IdSets.union(
			IdSets.toggled(config.blastFurnaceMachinery(), BlastFurnace.MACHINERY_OBJECTS),
			IdSets.toggled(config.blastFurnaceSmoke(), BlastFurnace.SMOKE_OBJECTS),
			IdSets.toggled(config.blastFurnaceManualEquipment(), BlastFurnace.MANUAL_EQUIPMENT_OBJECTS),
			IdSets.toggled(config.blastFurnaceCoffer(), BlastFurnace.COFFER_OBJECTS),
			IdSets.toggled(config.blastFurnaceSink(), BlastFurnace.SINK_OBJECTS));

		hiddenNpcIds = IdSets.union(
			IdSets.toggled(config.blastFurnaceOperatorDwarves(), BlastFurnace.OPERATOR_DWARF_NPCS),
			IdSets.toggled(config.blastFurnaceMerchants(), BlastFurnace.MERCHANT_NPCS),
			IdSets.toggled(config.blastFurnaceDeliveryMiners(), BlastFurnace.DELIVERY_MINER_NPCS));

		playerHiding.rebuild(
			config.blastFurnaceOtherPlayers(),
			config.blastFurnaceOtherPlayers2d(),
			config.blastFurnaceOtherPlayersPets(),
			config.blastFurnaceShowFriends());

		return !previousObjectIds.equals(hiddenObjectIds);
	}

	@Override
	public void reset()
	{
		hiddenObjectIds = Set.of();
		hiddenNpcIds = Set.of();
		playerHiding.reset();
	}

	@Override
	public void onSceneLoaded(Scene scene)
	{
		sceneIsBlastFurnace = BlastFurnace.isInScene(scene);
	}

	@Override
	public boolean hidesAnyScenery()
	{
		return !hiddenObjectIds.isEmpty();
	}

	@Override
	public boolean hidesObject(Scene scene, int objectId)
	{
		return BlastFurnace.isInScene(scene) && hiddenObjectIds.contains(objectId);
	}

	@Override
	public boolean hidesNpc(NPC npc)
	{
		if (sceneIsBlastFurnace && hiddenNpcIds.contains(npc.getId()))
		{
			return true;
		}

		return playerHiding.hidesPet(npc, sceneIsBlastFurnace);
	}

	@Override
	public boolean hidesPlayer(Player player, boolean drawingUi)
	{
		return playerHiding.hidesPlayer(player, drawingUi, sceneIsBlastFurnace);
	}

	/** Hidden equipment keeps its click box, so its menu entries are stripped to prevent misclicks. */
	@Override
	public boolean hidesMenuEntry(MenuEntry entry)
	{
		return sceneIsBlastFurnace
			&& hiddenObjectIds.contains(entry.getIdentifier())
			&& ObjectActions.isObjectAction(entry.getType());
	}
}
