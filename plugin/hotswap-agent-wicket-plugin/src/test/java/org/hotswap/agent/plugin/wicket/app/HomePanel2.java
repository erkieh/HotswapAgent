package org.hotswap.agent.plugin.wicket.app;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hotswap.agent.plugin.wicket.app.services.TestService;

public class HomePanel2 extends Panel {
	@SpringBean
	private TestService testService;
	
	public HomePanel2(String id) {
		super(id);
		add(new Label("panelLabel", new PropertyModel<>(this, "labelValue")));
	}
	
	public Object getLabelValue() {
		return testService.getNumber();
	}
	
}
