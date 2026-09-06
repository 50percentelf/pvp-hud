package com.pvphud;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PvpHudOverlay extends Overlay
{
	private static final Color PANEL_BG = new Color(30, 30, 30, 220);

	private final Client client;
	private final PvpHudPlugin plugin;

	@Inject
	PvpHudOverlay(Client client, PvpHudPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		PvpHudState state = plugin.getHudState();
		if (!state.getContext().isPvpActive())
		{
			return null;
		}

		Rectangle bounds = getChatboxBounds();
		if (bounds == null)
		{
			return null;
		}

		updateLayoutIfChanged(state, bounds);
		drawPanel(g, bounds);
		return null;
	}

	private void updateLayoutIfChanged(PvpHudState state, Rectangle bounds)
	{
		com.pvphud.state.HudLayoutState layout = state.getLayout();
		if (!bounds.equals(layout.getChatboxBounds()))
		{
			layout.setChatboxBounds(new Rectangle(bounds));
			layout.markDirty();
		}
	}

	private void drawPanel(Graphics2D g, Rectangle b)
	{
		g.setColor(PANEL_BG);
		g.fillRect(b.x, b.y, b.width, b.height);
	}

	/**
	 * Returns the screen-space bounds of the chatbox root widget,
	 * or null if it is hidden or not yet loaded.
	 */
	private Rectangle getChatboxBounds()
	{
		Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (chatbox == null || chatbox.isHidden())
		{
			return null;
		}
		return chatbox.getBounds();
	}
}
