package org.hotswap.agent.plugin.wicket.app;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

public class HomePage extends WebPage {
	private static final long serialVersionUID = 1L;
	
	public HomePage() {
		add(new Label("pageLabel", new PropertyModel<>(this, "labelValue")));
		add(new HomePanel("panel"));
	}
	
	public Object getLabelValue() {
		return 0;
	}
}