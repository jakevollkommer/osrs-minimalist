// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist.diagnostics;

import com.minimalist.content.ContentArea;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.api.Client;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.client.RuneLite;

/**
 * Writes a text report of everything in the loaded scene, and what Minimalist would
 * hide, to .runelite/minimalist-scene-report.txt. Triggered by the ::minimalist chat
 * command so players can attach it to bug reports. Local file only, nothing is sent
 * anywhere.
 */
public final class SceneReport
{
	public static final String FILE_NAME = "minimalist-scene-report.txt";

	/** Returns the number of distinct object entries written. */
	public static int write(Client client, ContentArea[] contentAreas) throws IOException
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		Map<String, Integer> objectCounts = new TreeMap<>();
		Map<Integer, Integer> groundItemCounts = new TreeMap<>();
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					recordTileContents(objectCounts, groundItemCounts, tile);
				}
			}
		}

		List<String> lines = new ArrayList<>();
		objectCounts.forEach((key, count) ->
		{
			int objectId = Integer.parseInt(key.split(":")[0]);
			lines.add("OBJECT " + key
				+ " count=" + count
				+ " name=" + client.getObjectDefinition(objectId).getName()
				+ " hidden=" + isHiddenByAnyArea(contentAreas, scene, objectId));
		});
		groundItemCounts.forEach((itemId, count) ->
			lines.add("GROUND-ITEM id=" + itemId + " count=" + count
				+ " name=" + client.getItemDefinition(itemId).getName()));

		Files.write(new File(RuneLite.RUNELITE_DIR, FILE_NAME).toPath(), lines);
		return objectCounts.size();
	}

	private static void recordTileContents(Map<String, Integer> objectCounts, Map<Integer, Integer> groundItemCounts, Tile tile)
	{
		recordObject(objectCounts, tile.getWallObject(), "wall");
		recordObject(objectCounts, tile.getDecorativeObject(), "decor");
		recordObject(objectCounts, tile.getGroundObject(), "ground");
		if (tile.getGameObjects() != null)
		{
			for (TileObject gameObject : tile.getGameObjects())
			{
				recordObject(objectCounts, gameObject, "game");
			}
		}
		if (tile.getGroundItems() != null)
		{
			for (TileItem groundItem : tile.getGroundItems())
			{
				groundItemCounts.merge(groundItem.getId(), 1, Integer::sum);
			}
		}
	}

	private static void recordObject(Map<String, Integer> objectCounts, TileObject object, String kind)
	{
		if (object != null)
		{
			objectCounts.merge(object.getId() + ":" + kind + ":plane" + object.getPlane(), 1, Integer::sum);
		}
	}

	private static boolean isHiddenByAnyArea(ContentArea[] contentAreas, Scene scene, int objectId)
	{
		for (ContentArea area : contentAreas)
		{
			if (area.hidesObject(scene, objectId))
			{
				return true;
			}
		}
		return false;
	}

	private SceneReport()
	{
	}
}
