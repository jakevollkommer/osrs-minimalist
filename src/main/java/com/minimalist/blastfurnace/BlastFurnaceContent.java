// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.blastfurnace;

import com.minimalist.ContentArea;
import com.minimalist.IdSets;
import com.minimalist.MinimalistConfig;
import com.minimalist.ObjectActions;
import com.minimalist.PlayerHiding;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;

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
	private volatile Set<WorldPoint> hiddenItemSpawnTiles = Set.of();
	private volatile boolean hideItemSpawns;
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
		boolean on = config.blastFurnaceEnabled();

		hiddenObjectIds = IdSets.union(
			IdSets.toggled(on && config.blastFurnaceMachinery(), BlastFurnace.MACHINERY_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceBrokenMachinery(), BlastFurnace.BROKEN_MACHINERY_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceSmoke(), BlastFurnace.SMOKE_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceManualEquipment(), BlastFurnace.MANUAL_EQUIPMENT_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceCoffer(), BlastFurnace.COFFER_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceSink(), BlastFurnace.SINK_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceRoomDecoration(), BlastFurnace.ROOM_DECORATION_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceSmithingArea(), BlastFurnace.SMITHING_AREA_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceDepositBox(), BlastFurnace.DEPOSIT_BOX_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceConveyorRamp(), BlastFurnace.CONVEYOR_RAMP_OBJECTS),
			IdSets.toggled(on && config.blastFurnaceStaircase(), BlastFurnace.STAIRCASE_OBJECTS));

		hideItemSpawns = on && config.blastFurnaceItemSpawns();

		hiddenNpcIds = IdSets.union(
			IdSets.toggled(on && config.blastFurnaceOperatorDwarves(), BlastFurnace.OPERATOR_DWARF_NPCS),
			IdSets.toggled(on && config.blastFurnaceForeman(), BlastFurnace.FOREMAN_NPCS),
			IdSets.toggled(on && config.blastFurnaceMerchants(), BlastFurnace.MERCHANT_NPCS),
			IdSets.toggled(on && config.blastFurnaceDeliveryMiners(), BlastFurnace.DELIVERY_MINER_NPCS));

		playerHiding.rebuild(
			on && config.blastFurnaceOtherPlayers(),
			on && config.blastFurnaceOtherPlayers2d(),
			on && config.blastFurnaceOtherPlayersPets(),
			config.blastFurnaceShowFriends());

		return !previousObjectIds.equals(hiddenObjectIds);
	}

	@Override
	public void reset()
	{
		hiddenObjectIds = Set.of();
		hiddenNpcIds = Set.of();
		hiddenItemSpawnTiles = Set.of();
		hideItemSpawns = false;
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
		if (!sceneIsBlastFurnace)
		{
			return false;
		}

		if (hiddenObjectIds.contains(entry.getIdentifier()) && ObjectActions.isObjectAction(entry.getType()))
		{
			return true;
		}

		return hideItemSpawns
			&& BlastFurnace.ITEM_SPAWN_IDS.contains(entry.getIdentifier())
			&& ObjectActions.isGroundItemAction(entry.getType());
	}

	@Override
	public boolean hidesItemPile(WorldPoint pileLocation)
	{
		return hideItemSpawns && sceneIsBlastFurnace && hiddenItemSpawnTiles.contains(pileLocation);
	}

	@Override
	public void onItemSpawned(ItemSpawned event)
	{
		refreshSpawnTile(event.getTile());
	}

	@Override
	public void onItemDespawned(ItemDespawned event)
	{
		refreshSpawnTile(event.getTile());
	}

	/**
	 * A tile is hidden only while every item on it is one of the tracked spawns, so a
	 * real drop landing on a spawn tile makes the whole pile visible again. Tracking is
	 * bounded to the furnace region, so these common items never disappear elsewhere.
	 */
	private void refreshSpawnTile(Tile tile)
	{
		WorldPoint tileLocation = tile.getWorldLocation();
		if (tileLocation.getRegionID() != BlastFurnace.FURNACE_REGION)
		{
			return;
		}

		boolean pileIsOnlyTrackedSpawns = tile.getGroundItems() != null
			&& !tile.getGroundItems().isEmpty()
			&& tile.getGroundItems().stream().map(TileItem::getId).allMatch(BlastFurnace.ITEM_SPAWN_IDS::contains);

		Set<WorldPoint> updated = new HashSet<>(hiddenItemSpawnTiles);
		boolean changed = pileIsOnlyTrackedSpawns ? updated.add(tileLocation) : updated.remove(tileLocation);
		if (changed)
		{
			hiddenItemSpawnTiles = Set.copyOf(updated);
		}
	}
}
