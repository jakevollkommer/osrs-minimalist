// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Jake Vollkommer
package com.minimalist;

import com.google.inject.Provides;
import com.minimalist.altars.AltarContent;
import com.minimalist.blastfurnace.BlastFurnaceContent;
import com.minimalist.gotr.GotrContent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemLayer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(
	name = "Minimalist",
	description = "Hide non-interactable scenery objects, NPCs, and HUD elements at supported content",
	tags = {"jake", "hide", "hider", "scenery", "object", "objects", "npc", "entity", "declutter", "clean", "minimal", "gotr", "guardians", "rift", "runecrafting", "runecraft", "altar", "altars", "minigame", "hud", "blast", "furnace", "smithing"}
)
public class MinimalistPlugin extends Plugin implements RenderCallback
{
	private static final String SUGGESTIONS_URL = "https://github.com/jakevollkommer/osrs-minimalist/issues";
	private static final String SUPPORT_URL = "https://ko-fi.com/jakevollkommer";

	// Draw suppression happens through RenderCallback: static scenery is filtered when
	// the scene uploads, and animated objects are filtered every frame, so nothing is
	// ever removed from the scene, everything stays hoverable, clickable, and visible
	// to other plugins.
	//
	// The plugin itself knows nothing about any specific activity: each supported one
	// is a ContentArea that owns its regions, IDs, and rules. A plain array because
	// addEntity and drawObject run every frame (and drawObject on the maploader thread
	// during scene upload, hence volatile).
	private volatile ContentArea[] contentAreas = new ContentArea[0];

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private MinimalistConfig config;

	@Provides
	MinimalistConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MinimalistConfig.class);
	}

	@Override
	protected void startUp()
	{
		contentAreas = new ContentArea[]
		{
			new GotrContent(client, config),
			new AltarContent(config),
			new BlastFurnaceContent(client, config),
		};
		renderCallbackManager.register(this);
		clientThread.invokeLater(() ->
		{
			for (ContentArea area : contentAreas)
			{
				area.rebuildFromConfig();
			}
			refreshSceneState();
			reloadSceneIfLoggedIn();
		});
	}

	@Override
	protected void shutDown()
	{
		renderCallbackManager.unregister(this);
		clientThread.invokeLater(() ->
		{
			Set<Integer> widgetsToRestore = allHiddenWidgets();
			for (ContentArea area : contentAreas)
			{
				area.reset();
			}
			widgetsToRestore.forEach(component -> setWidgetHidden(component, false));
			reloadSceneIfLoggedIn();
		});
	}

	// --- RenderCallback: the single place visibility is decided ---

	// TEMPORARY dev diagnostic, removed before merge: log each distinct renderable type
	// once so we learn how ground items reach addEntity.
	private final Set<String> seenRenderableTypes = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private final java.util.Map<String, Long> drawObjectWatch = new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public boolean addEntity(Renderable renderable, boolean drawingUi)
	{
		seenRenderableTypes.add(renderable.getClass().getName());
		if (renderable instanceof NPC)
		{
			NPC npc = (NPC) renderable;
			for (ContentArea area : contentAreas)
			{
				if (area.hidesNpc(npc))
				{
					return false;
				}
			}
			return true;
		}

		if (renderable instanceof Player)
		{
			Player player = (Player) renderable;
			for (ContentArea area : contentAreas)
			{
				if (area.hidesPlayer(player, drawingUi))
				{
					return false;
				}
			}
			return true;
		}

		if (renderable instanceof Projectile)
		{
			for (ContentArea area : contentAreas)
			{
				if (area.hidesProjectiles())
				{
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		// TEMPORARY dev instrumentation, removed before merge: record what the renderer
		// asks about ids in the Blast Furnace band and what we answered.
		if (object.getId() >= 6150 && object.getId() <= 29330)
		{
			boolean hidden = false;
			for (ContentArea area : contentAreas)
			{
				hidden |= area.hidesObject(scene, object.getId());
			}
			String key = object.getId() + ":" + object.getClass().getSimpleName() + ":" + Thread.currentThread().getName() + ":hidden=" + hidden;
			drawObjectWatch.merge(key, 1L, Long::sum);
		}

		if (object instanceof ItemLayer)
		{
			WorldPoint pileLocation = object.getWorldLocation();
			for (ContentArea area : contentAreas)
			{
				if (area.hidesItemPile(pileLocation))
				{
					return false;
				}
			}
			return true;
		}

		int objectId = object.getId();
		for (ContentArea area : contentAreas)
		{
			if (area.hidesObject(scene, objectId))
			{
				return false;
			}
		}
		return true;
	}

	// --- game state tracking ---

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		for (ContentArea area : contentAreas)
		{
			area.onScriptPreFired(event);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		for (ContentArea area : contentAreas)
		{
			area.onItemContainerChanged(event);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		for (ContentArea area : contentAreas)
		{
			area.onItemSpawned(event);
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		for (ContentArea area : contentAreas)
		{
			area.onItemDespawned(event);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::refreshSceneState);
		}
	}

	private void refreshSceneState()
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		Scene scene = worldView.getScene();
		for (ContentArea area : contentAreas)
		{
			area.onSceneLoaded(scene);
		}
		warnIfSceneryHidingUnavailable(scene);
	}

	private boolean warnedAboutSoftwareRenderer;

	/**
	 * Scenery hiding works through the render callback, which only GPU renderers
	 * (GPU, GPU with region locker, 117HD) consult, the software renderer never asks.
	 * Warn once per session so players are not left wondering why nothing hides.
	 */
	private void warnIfSceneryHidingUnavailable(Scene scene)
	{
		if (warnedAboutSoftwareRenderer || client.getDrawCallbacks() != null)
		{
			return;
		}

		boolean hidingWantedHere = false;
		for (ContentArea area : contentAreas)
		{
			if (area.isInScene(scene) && area.hidesAnyScenery())
			{
				hidingWantedHere = true;
				break;
			}
		}
		if (!hidingWantedHere)
		{
			return;
		}

		warnedAboutSoftwareRenderer = true;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"<col=e00a19>Minimalist:</col> hiding scenery requires a GPU renderer - enable the GPU plugin (or 117HD).",
			null);
	}

	// --- widgets ---

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		// interfaces are rebuilt by clientscripts whenever their values update, which
		// resets the hidden flag, so re-hide every client tick
		for (ContentArea area : contentAreas)
		{
			area.hiddenWidgetComponents().forEach(component -> setWidgetHidden(component, true));
		}
	}

	private void setWidgetHidden(int componentId, boolean hidden)
	{
		Widget widget = client.getWidget(componentId);
		boolean alreadyInDesiredState = widget == null || widget.isHidden() == hidden;
		if (alreadyInDesiredState)
		{
			return;
		}

		widget.setHidden(hidden);
	}

	private Set<Integer> allHiddenWidgets()
	{
		Set<Integer> widgets = new HashSet<>();
		for (ContentArea area : contentAreas)
		{
			widgets.addAll(area.hiddenWidgetComponents());
		}
		return widgets;
	}

	// --- menus ---

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		MenuEntry[] entries = client.getMenuEntries();
		// cheap scan first: PostMenuSort runs every frame, so only allocate when needed
		boolean hasHiddenEntry = false;
		for (MenuEntry entry : entries)
		{
			if (isHiddenMenuEntry(entry))
			{
				hasHiddenEntry = true;
				break;
			}
		}

		if (!hasHiddenEntry)
		{
			return;
		}

		client.setMenuEntries(Arrays.stream(entries)
			.filter(entry -> !isHiddenMenuEntry(entry))
			.toArray(MenuEntry[]::new));
	}

	private boolean isHiddenMenuEntry(MenuEntry entry)
	{
		for (ContentArea area : contentAreas)
		{
			if (area.hidesMenuEntry(entry))
			{
				return true;
			}
		}
		return false;
	}

	// TEMPORARY dev diagnostic, removed before merge: ::minidump logs every object in the
	// loaded scene so curated ID sets can be built from ground truth.
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!"minidump".equals(event.getCommand()))
		{
			return;
		}

		Scene scene = client.getTopLevelWorldView().getScene();
		java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
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
					recordTyped(counts, tile.getWallObject(), "wall");
					recordTyped(counts, tile.getDecorativeObject(), "decor");
					recordTyped(counts, tile.getGroundObject(), "ground");
					if (tile.getGameObjects() != null)
					{
						for (TileObject gameObject : tile.getGameObjects())
						{
							recordTyped(counts, gameObject, "game");
						}
					}
				}
			}
		}
		java.util.List<String> lines = new java.util.ArrayList<>();
		counts.forEach((key, count) ->
		{
			int id = Integer.parseInt(key.split(":")[0]);
			lines.add("MINIDUMP id=" + key + " count=" + count + " name=" + client.getObjectDefinition(id).getName());
		});
		drawObjectWatch.forEach((key, asks) -> lines.add("MINIWATCH " + key + " asks=" + asks));
		java.util.Map<Integer, Integer> itemCounts = new java.util.TreeMap<>();
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null || tile.getGroundItems() == null)
					{
						continue;
					}
					for (net.runelite.api.TileItem groundItem : tile.getGroundItems())
					{
						itemCounts.merge(groundItem.getId(), 1, Integer::sum);
					}
				}
			}
		}
		itemCounts.forEach((id, count) ->
			lines.add("MINIDUMP-ITEM id=" + id + " count=" + count + " name=" + client.getItemDefinition(id).getName()));
		seenRenderableTypes.forEach(type -> lines.add("MINIREND " + type));
		try
		{
			java.nio.file.Files.write(new java.io.File(net.runelite.client.RuneLite.RUNELITE_DIR, "minidump.txt").toPath(), lines);
		}
		catch (java.io.IOException ex)
		{
			throw new RuntimeException(ex);
		}
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Minimalist: dumped " + counts.size() + " object ids to .runelite/minidump.txt", null);
	}

	private static void recordTyped(java.util.Map<String, Integer> counts, TileObject object, String kind)
	{
		if (object != null)
		{
			counts.merge(object.getId() + ":" + kind + ":plane" + object.getPlane() + ":" + object.getClass().getSimpleName(), 1, Integer::sum);
		}
	}

	// --- config ---

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!MinimalistConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (isFeedbackButtonPress(event))
		{
			openFeedbackLink(event.getKey());
			return;
		}

		clientThread.invokeLater(this::applyConfigChange);
	}

	// The config panel cannot host real buttons, so the feedback "buttons" are checkboxes
	// that act as buttons: any click of the box, tick or untick, opens the link.
	private static boolean isFeedbackButtonPress(ConfigChanged event)
	{
		boolean isFeedbackButton = MinimalistConfig.SUGGEST_BUTTON_KEY.equals(event.getKey())
			|| MinimalistConfig.SUPPORT_BUTTON_KEY.equals(event.getKey());
		return isFeedbackButton && event.getNewValue() != null;
	}

	private void openFeedbackLink(String buttonKey)
	{
		String url = MinimalistConfig.SUGGEST_BUTTON_KEY.equals(buttonKey)
			? SUGGESTIONS_URL
			: SUPPORT_URL;
		LinkBrowser.browse(url);
	}

	private void applyConfigChange()
	{
		Set<Integer> previousWidgets = allHiddenWidgets();

		boolean staticSceneryChanged = false;
		for (ContentArea area : contentAreas)
		{
			staticSceneryChanged |= area.rebuildFromConfig();
		}

		Set<Integer> currentWidgets = allHiddenWidgets();
		previousWidgets.stream()
			.filter(component -> !currentWidgets.contains(component))
			.forEach(component -> setWidgetHidden(component, false));

		// static scenery filtering is applied when the scene uploads, so changes to it
		// take one reload; NPC, player, projectile, and widget changes apply live
		if (staticSceneryChanged)
		{
			reloadSceneIfLoggedIn();
		}
	}

	private void reloadSceneIfLoggedIn()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.setGameState(GameState.LOADING);
		}
	}
}
