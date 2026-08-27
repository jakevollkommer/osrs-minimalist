// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.gotr;

import com.minimalist.ContentArea;
import com.minimalist.MinimalistConfig;
import com.minimalist.altars.Altars;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InventoryID;

/**
 * Everything Minimalist hides at Guardians of the Rift: arena scenery, statues,
 * NPCs, other players and their pets, projectiles, and HUD elements.
 *
 * All fields are written on the client thread and volatile because hidesObject
 * is also called from the maploader thread during scene upload.
 */
public class GotrContent implements ContentArea
{
	private final Client client;
	private final MinimalistConfig config;

	private volatile Set<Integer> hiddenObjectIds = Set.of();
	private volatile Set<Integer> hiddenNpcIds = Set.of();
	private volatile Set<Integer> hiddenWidgets = Set.of();
	private volatile Set<Integer> heldTalismanStatues = Set.of();
	private volatile boolean hideInactiveStatues;
	private volatile boolean hideProjectiles;
	private volatile boolean hideOtherPlayers;
	private volatile boolean hideOtherPlayers2d;
	private volatile boolean hideOtherPlayersPets;
	private volatile boolean showFriends;
	private volatile boolean hideArenaGenericScenery;
	private volatile boolean sceneIsGotr;
	private volatile boolean sceneHasAltar;
	private volatile int activeElementalStatue = -1;
	private volatile int activeCatalyticStatue = -1;

	public GotrContent(Client client, MinimalistConfig config)
	{
		this.client = client;
		this.config = config;
	}

	@Override
	public boolean isInScene(Scene scene)
	{
		return GotrArena.isInScene(scene);
	}

	@Override
	public boolean rebuildFromConfig()
	{
		Set<Integer> previousObjectIds = hiddenObjectIds;
		boolean previousArenaGenerics = hideArenaGenericScenery;

		hideInactiveStatues = config.gotrGuardianStatues();
		hideProjectiles = config.gotrProjectiles();
		hideOtherPlayers = config.gotrOtherPlayers();
		hideOtherPlayers2d = config.gotrOtherPlayers2d();
		hideOtherPlayersPets = config.gotrOtherPlayersPets();
		showFriends = config.gotrShowFriends();
		hideArenaGenericScenery = config.gotrAbyssScenery();

		hiddenObjectIds = union(
			toggled(config.gotrAbyssScenery(), GotrArena.ABYSS_SCENERY_OBJECTS),
			toggled(config.gotrGuardianRemains(), GotrArena.GUARDIAN_REMAINS_OBJECTS),
			toggled(config.gotrEssencePiles(), GotrArena.ESSENCE_PILE_OBJECTS),
			toggled(config.gotrBarriersAndCells(), GotrArena.BARRIER_AND_CELL_OBJECTS),
			toggled(config.gotrEntranceScenery(), GotrArena.ENTRANCE_SCENERY_OBJECTS),
			toggled(config.gotrRain(), GotrArena.RAIN_OBJECTS));

		hiddenNpcIds = union(
			toggled(config.gotrAbyssalCreatures(), GotrNpcs.ABYSSAL_CREATURES),
			toggled(config.gotrSummonedGuardians(), GotrNpcs.SUMMONED_GUARDIANS),
			toggled(config.gotrApprentices(), GotrNpcs.APPRENTICES),
			toggled(config.gotrRick(), GotrNpcs.RICK),
			toggled(config.gotrBarrierHitsplats(), GotrNpcs.BARRIER_HITPOINT_HOLDERS));

		hiddenWidgets = union(
			toggled(config.gotrHudPortalTimer(), Set.of(GotrHud.PORTAL_TIMER_COMPONENT)),
			toggled(config.gotrHudGuardianCounter(), Set.of(GotrHud.GUARDIAN_COUNTER_COMPONENT)),
			toggled(config.gotrHudPortalLocation(), Set.of(GotrHud.PORTAL_LOCATION_COMPONENT)));

		return !previousObjectIds.equals(hiddenObjectIds)
			|| previousArenaGenerics != hideArenaGenericScenery;
	}

	@Override
	public void reset()
	{
		hiddenObjectIds = Set.of();
		hiddenNpcIds = Set.of();
		hiddenWidgets = Set.of();
		hideInactiveStatues = false;
		hideProjectiles = false;
		hideOtherPlayers = false;
		hideOtherPlayers2d = false;
		hideOtherPlayersPets = false;
		hideArenaGenericScenery = false;
	}

	@Override
	public void onSceneLoaded(Scene scene)
	{
		sceneIsGotr = GotrArena.isInScene(scene);
		// entity hiding extends into the runecrafting altars reached from the arena
		sceneHasAltar = Altars.hasAltarInScene(scene);
		refreshHeldTalismans();
	}

	@Override
	public boolean hidesAnyScenery()
	{
		return !hiddenObjectIds.isEmpty() || hideInactiveStatues;
	}

	@Override
	public boolean hidesObject(Scene scene, int objectId)
	{
		if (!GotrArena.isInScene(scene))
		{
			return false;
		}

		if (hiddenObjectIds.contains(objectId))
		{
			return true;
		}

		if (GuardianStatues.STATUE_OBJECTS.contains(objectId))
		{
			// animated objects pass through here every frame, so statue visibility
			// follows rotations and inventory instantly
			return isHiddenStatue(objectId);
		}

		return hideArenaGenericScenery
			&& GotrArena.ARENA_GENERIC_SCENERY_OBJECTS.contains(objectId);
	}

	@Override
	public boolean hidesNpc(NPC npc)
	{
		if (sceneIsGotr && hiddenNpcIds.contains(npc.getId()))
		{
			return true;
		}

		return hideOtherPlayersPets && isAtGotrContent() && isSomeoneElsesPet(npc);
	}

	private boolean isSomeoneElsesPet(NPC npc)
	{
		return npc.getComposition().isFollower() && npc != client.getFollower();
	}

	@Override
	public boolean hidesPlayer(Player player, boolean drawingUi)
	{
		boolean isOtherPlayerAtGotr = isAtGotrContent() && player != client.getLocalPlayer();
		if (!isOtherPlayerAtGotr)
		{
			return false;
		}

		if (showFriends && player.isFriend())
		{
			return false;
		}

		return drawingUi ? hideOtherPlayers2d : hideOtherPlayers;
	}

	@Override
	public boolean hidesProjectiles()
	{
		return hideProjectiles && isAtGotrContent();
	}

	@Override
	public Set<Integer> hiddenWidgetComponents()
	{
		return hiddenWidgets;
	}

	/**
	 * Visually hidden statues stay clickable (they are still in the scene), so their
	 * menu entries are stripped to prevent misclicks. Active and talisman-held statues
	 * keep their menus, matching their visibility.
	 */
	@Override
	public boolean hidesMenuEntry(MenuEntry entry)
	{
		return hideInactiveStatues
			&& sceneIsGotr
			&& GuardianStatues.STATUE_OBJECTS.contains(entry.getIdentifier())
			&& isObjectAction(entry.getType())
			&& isHiddenStatue(entry.getIdentifier());
	}

	@Override
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != GotrHud.UPDATE_SCRIPT)
		{
			return;
		}

		Object[] arguments = event.getScriptEvent().getArguments();
		if (arguments == null || arguments.length <= GotrHud.ARG_ACTIVE_CATALYTIC)
		{
			return;
		}

		int elementalIndex = asInt(arguments[GotrHud.ARG_ACTIVE_ELEMENTAL]);
		int catalyticIndex = asInt(arguments[GotrHud.ARG_ACTIVE_CATALYTIC]);
		activeElementalStatue = GuardianStatues.STATUE_BY_ELEMENTAL_INDEX.getOrDefault(elementalIndex, -1);
		activeCatalyticStatue = GuardianStatues.STATUE_BY_CATALYTIC_INDEX.getOrDefault(catalyticIndex, -1);
	}

	@Override
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			refreshHeldTalismans();
		}
	}

	/** Entity hiding applies at GOTR and inside the runecrafting altars reached from it. */
	private boolean isAtGotrContent()
	{
		return sceneIsGotr || sceneHasAltar;
	}

	private boolean isHiddenStatue(int statueObjectId)
	{
		return hideInactiveStatues
			&& statueObjectId != activeElementalStatue
			&& statueObjectId != activeCatalyticStatue
			&& !heldTalismanStatues.contains(statueObjectId);
	}

	private void refreshHeldTalismans()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			heldTalismanStatues = Set.of();
			return;
		}

		Set<Integer> held = new HashSet<>();
		GuardianStatues.TALISMAN_BY_STATUE.forEach((statueId, talismanId) ->
		{
			if (inventory.contains(talismanId))
			{
				held.add(statueId);
			}
		});
		heldTalismanStatues = Set.copyOf(held);
	}

	private static boolean isObjectAction(MenuAction action)
	{
		switch (action)
		{
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case EXAMINE_OBJECT:
				return true;
			default:
				return false;
		}
	}

	private static Set<Integer> toggled(boolean enabled, Set<Integer> curatedIds)
	{
		return enabled ? curatedIds : Set.of();
	}

	@SafeVarargs
	private static Set<Integer> union(Set<Integer>... sets)
	{
		return Stream.of(sets)
			.flatMap(Set::stream)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static int asInt(Object argument)
	{
		return argument instanceof Integer ? (Integer) argument : Integer.parseInt(argument.toString());
	}
}
