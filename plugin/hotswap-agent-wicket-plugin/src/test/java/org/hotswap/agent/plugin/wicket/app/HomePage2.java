package org.hotswap.agent.plugin.wicket.app;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hotswap.agent.plugin.wicket.app.services.TestService;

public class HomePage2 extends WebPage {
	private static final long serialVersionUID = 1L;
	@SpringBean
	private TestService testService;
	
	public HomePage2() {
		add(new Label("pageLabel", new PropertyModel<>(this, "labelValue")));
		add(new HomePanel("panel"));
	}
	
	public Object getLabelValue() {
		return testService.getNumber();
	}
}