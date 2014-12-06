package org.hotswap.agent.plugin.wicket.app;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public class HomePanel extends Panel {
	
	public HomePanel(String id) {
		super(id);
		add(new Label("panelLabel", new PropertyModel<>(this, "labelValue")));
	}
	
	public Object getLabelValue() {
		return 0;
	}
	
}
