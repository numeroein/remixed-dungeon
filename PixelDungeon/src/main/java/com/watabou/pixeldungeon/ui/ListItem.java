package com.watabou.pixeldungeon.ui;

import com.nyrds.android.util.GuiProperties;
import com.watabou.noosa.CompositeTextureImage;
import com.watabou.noosa.Text;
import com.watabou.noosa.ui.Component;
import com.watabou.pixeldungeon.scenes.PixelScene;

/**
 * Created by mike on 15.05.2017.
 * This file is part of Remixed Pixel Dungeon.
 */
public abstract class ListItem extends Component implements IClickable {

	protected CompositeTextureImage sprite    = new CompositeTextureImage();
	protected Text                  label     = PixelScene.createText(GuiProperties.regularFontSize());
	protected boolean               clickable = false;
	protected int                   align     = 24;

	protected ListItem() {
		super();
		add(sprite);
		add(label);
	}

	@Override
	protected void layout() {
		sprite.y = PixelScene.align(y + (height - sprite.height) / 2);

		label.x = Math.max(sprite.x + sprite.width, align);
		label.y = PixelScene.align(y + (height - label.baseLine()) / 2);
	}

	public boolean onClick(float x, float y) {
		if (clickable && inside(x, y)) {
			onClick();
			return true;
		} else {
			return false;
		}
	}

	abstract protected void onClick();
}
