// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.blastfurnace;

import java.util.Set;
import net.runelite.api.Scene;

/**
 * Curated IDs for the Blast Furnace room in Keldagrim. Sourced from the game cache.
 *
 * Never hidden, in any category: the conveyor belt, the bar dispenser (which the scene
 * stores as varbit base id 9092, not its named 9093-9096 states), bars, melting pot,
 * temperature gauge, and the Blast Furnace Foreman, since every player's core loop
 * needs them.
 */
public final class BlastFurnace
{
	/** The map region of the Blast Furnace room. */
	public static final int FURNACE_REGION = 7757;

	/** Dumpy, Stumpy, Pumpy, Numpty, and Thumpy, who work the machinery on official worlds. */
	public static final Set<Integer> OPERATOR_DWARF_NPCS = Set.of(
		5454 /* Thumpy */,
		6602 /* Numpty */,
		7384 /* Stumpy */,
		7385 /* Pumpy */,
		7386 /* Dumpy */,
		7387 /* Dumpy */);

	/** Ordan (ore shop) and Jorzik (armour shop); optional because their shops are real. */
	public static final Set<Integer> MERCHANT_NPCS = Set.of(
		1560 /* Ordan */,
		1561 /* Jorzik */);

	/**
	 * The Dwarven Miners who wander in delivering ore to Ordan's shop; optional because
	 * some players watch the deliveries, for example for gold ore restocks.
	 */
	public static final Set<Integer> DELIVERY_MINER_NPCS = Set.of(
		2434, 2435, 2436, 2437, 2438, 2439, 2440, 2441,
		2442, 2443, 2444, 2445, 2446, 2447, 2448);

	/**
	 * Intact machinery only. The broken variants (9103, 9105, 9117, 9121, all carrying a
	 * Repair op) are deliberately absent so breakage pops into view when it matters.
	 */
	public static final Set<Integer> MACHINERY_OBJECTS = Set.of(
		9102 /* Drive belt */,
		9104 /* Cogs */,
		9106 /* Gear box */,
		9107 /* Drive belt */,
		9108 /* Cogs */,
		8919 /* wooden platform parts */,
		8920 /* wooden platform parts */,
		8922 /* wooden platform parts */,
		8923 /* wooden platform parts */,
		9109 /* unnamed machinery parts */,
		9110 /* unnamed machinery parts */,
		9111 /* unnamed machinery parts */,
		9112 /* unnamed machinery parts */,
		9113 /* unnamed machinery parts */,
		9114 /* unnamed machinery parts */,
		9116 /* Pipes */,
		9118 /* unnamed machinery parts */,
		9119 /* unnamed machinery parts */,
		9120 /* Pipes */,
		9122 /* unnamed machinery parts */,
		9124 /* unnamed machinery parts */,
		9125 /* unnamed machinery parts */);

	/** Smoke billowing from the machinery. */
	public static final Set<Integer> SMOKE_OBJECTS = Set.of(9099, 9115, 9123);

	/**
	 * Equipment only needed when running the furnace manually on non-official worlds:
	 * it also trains Strength (pump), Agility (pedals), and Firemaking (stove).
	 */
	public static final Set<Integer> MANUAL_EQUIPMENT_OBJECTS = Set.of(
		9085 /* Stove */,
		9086 /* Stove */,
		9087 /* Stove */,
		9088 /* Coke */,
		9090 /* Pump */,
		9097 /* Pedals */);

	/**
	 * The coffer only official-world players need. The scene stores the varbit-swapped
	 * base id (29330), never the named "Coffer" transform targets, so this is the id
	 * the renderer asks about; all of its states are the coffer, hidden deliberately.
	 */
	public static final Set<Integer> COFFER_OBJECTS = Set.of(29330);

	/** The Fill-bucket sink, unnecessary with ice or smiths gloves. */
	public static final Set<Integer> SINK_OBJECTS = Set.of(9143);

	/** Shelves, boxes, crates, the table, and lamps around the room. */
	public static final Set<Integer> ROOM_DECORATION_OBJECTS = Set.of(
		6154, 6155, 6156, 6157 /* Shelves */,
		6176 /* Boxes, searchable, hidden deliberately */,
		6177 /* unnamed box decor */,
		6178 /* Crates */,
		6196 /* Table */,
		6203 /* Lamp */,
		14395 /* Boxes */);

	/** The anvils and the fence and gate around the smithing corner. */
	public static final Set<Integer> SMITHING_AREA_OBJECTS = Set.of(
		6150 /* Anvil, Smith op, hidden deliberately */,
		9139 /* fence segments */,
		9141 /* Gate, Open op, hidden deliberately */);

	/** The wooden plank ramp up to the conveyor belt, drawn as ground objects. */
	public static final Set<Integer> CONVEYOR_RAMP_OBJECTS = Set.of(799);

	/** The bank deposit box; the bank chest always shows. */
	public static final Set<Integer> DEPOSIT_BOX_OBJECTS = Set.of(10529);

	/** The bucket, spade, and hammer ground spawns, tracked per spawn tile. */
	public static final Set<Integer> ITEM_SPAWN_IDS = Set.of(
		952 /* Spade */,
		1925 /* Bucket */,
		2347 /* Hammer */);

	/**
	 * True when the loaded scene spans the Blast Furnace. Reads the passed Scene, never
	 * client state, so it is correct even while statics are filtered mid-upload.
	 */
	public static boolean isInScene(Scene scene)
	{
		int[] sceneRegions = scene.getMapRegions();
		if (sceneRegions == null)
		{
			// no scene is loaded yet (login screen)
			return false;
		}

		for (int regionId : sceneRegions)
		{
			if (regionId == FURNACE_REGION)
			{
				return true;
			}
		}

		return false;
	}

	private BlastFurnace()
	{
	}
}
