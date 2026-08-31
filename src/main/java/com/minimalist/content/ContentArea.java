// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.content;

import java.util.Set;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.ScriptPreFired;

/**
 * One supported in-game activity (Guardians of the Rift, the runecrafting altars, ...).
 * The plugin is a content-agnostic dispatcher over a list of these; everything an area
 * hides, and the regions it applies to, lives in the area's own package.
 *
 * Every hides* method must gate itself on its own content being present: IDs are reused
 * across the game, so an ungated match leaks hiding into unrelated areas. hidesObject is
 * also called on the maploader thread during scene upload, so it must read only the
 * passed Scene and volatile state.
 */
public interface ContentArea
{
	/** True when the loaded scene spans this content. Reads only the passed Scene. */
	boolean isInScene(Scene scene);

	/**
	 * Reread config. Returns true when static scenery hiding changed, which the plugin
	 * answers with a scene reload; live-applied hiding (NPCs, players, widgets) returns false.
	 */
	boolean rebuildFromConfig();

	/** Stop hiding everything, called on plugin shutdown before the final scene reload. */
	void reset();

	/** Refresh cached scene-scoped state; called on the client thread after each scene load. */
	default void onSceneLoaded(Scene scene)
	{
	}

	/** True when this area currently wants to hide any scenery (drives the GPU warning). */
	default boolean hidesAnyScenery()
	{
		return false;
	}

	default boolean hidesObject(Scene scene, int objectId)
	{
		return false;
	}

	default boolean hidesNpc(NPC npc)
	{
		return false;
	}

	/** Tests the ground-item pile at the given tile; areas track qualifying spawn tiles via the item events. */
	default boolean hidesItemPile(WorldPoint pileLocation)
	{
		return false;
	}

	default boolean hidesPlayer(Player player, boolean drawingUi)
	{
		return false;
	}

	default boolean hidesProjectiles()
	{
		return false;
	}

	/** Widget components to keep hidden; re-applied by the plugin every client tick. */
	default Set<Integer> hiddenWidgetComponents()
	{
		return Set.of();
	}

	/** Menu entries to strip so visually hidden but still-clickable things cannot be misclicked. */
	default boolean hidesMenuEntry(MenuEntry entry)
	{
		return false;
	}

	default void onScriptPreFired(ScriptPreFired event)
	{
	}

	default void onItemContainerChanged(ItemContainerChanged event)
	{
	}

	default void onItemSpawned(ItemSpawned event)
	{
	}

	default void onItemDespawned(ItemDespawned event)
	{
	}
}
