// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.altars;

import com.minimalist.ContentArea;
import com.minimalist.MinimalistConfig;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.Scene;

/**
 * Decorative scenery and NPCs inside the twelve runecrafting altars.
 *
 * Altar decoration uses generic world IDs, so everything gates on the loaded scene
 * spanning altar regions. hidesObject reads only the passed Scene because it also
 * runs on the maploader thread during scene upload; the volatile fields are written
 * on the client thread.
 */
public class AltarContent implements ContentArea
{
	private final MinimalistConfig config;

	private volatile boolean hideAltarScenery;
	private volatile Set<Integer> sceneHiddenNpcIds = Set.of();

	public AltarContent(MinimalistConfig config)
	{
		this.config = config;
	}

	@Override
	public boolean isInScene(Scene scene)
	{
		return Altars.hasAltarInScene(scene);
	}

	@Override
	public boolean rebuildFromConfig()
	{
		boolean previous = hideAltarScenery;
		hideAltarScenery = config.gotrEnabled() && config.gotrAltarScenery();
		return previous != hideAltarScenery;
	}

	@Override
	public void reset()
	{
		hideAltarScenery = false;
		sceneHiddenNpcIds = Set.of();
	}

	@Override
	public void onSceneLoaded(Scene scene)
	{
		sceneHiddenNpcIds = Altars.hiddenNpcsForScene(scene);
	}

	@Override
	public boolean hidesAnyScenery()
	{
		return hideAltarScenery;
	}

	@Override
	public boolean hidesObject(Scene scene, int objectId)
	{
		return hideAltarScenery && Altars.isAltarSceneryInScene(scene, objectId);
	}

	@Override
	public boolean hidesNpc(NPC npc)
	{
		return hideAltarScenery && sceneHiddenNpcIds.contains(npc.getId());
	}
}
