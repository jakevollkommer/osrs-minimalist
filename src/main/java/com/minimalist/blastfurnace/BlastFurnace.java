// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.blastfurnace;

import net.runelite.api.Scene;

/**
 * Region data for the Blast Furnace room in Keldagrim. Curated scenery IDs will
 * live here as Blast Furnace support grows.
 */
public final class BlastFurnace
{
	/** The map region of the Blast Furnace room. */
	public static final int FURNACE_REGION = 7757;

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
