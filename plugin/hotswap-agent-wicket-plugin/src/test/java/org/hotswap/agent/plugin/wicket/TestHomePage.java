package org.hotswap.agent.plugin.wicket;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.hotswap.agent.plugin.wicket.app.HomePage;
import org.hotswap.agent.plugin.wicket.app.HomePage2;
import org.hotswap.agent.plugin.wicket.app.HomePanel;
import org.hotswap.agent.plugin.wicket.app.HomePanel2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class TestHomePage {
	private HtmlUnitDriver driver;
	private Server server;
	
	@Before
	public void setUp() throws Exception {
		final CountDownLatch l = new CountDownLatch(1);
		new Thread() {
			
			@Override
			public void run() {
				
				try {
					server = Start.createServer();
					server.addLifeCycleListener(new AbstractLifeCycleListener() {
						@Override
						public void lifeCycleStarted(LifeCycle event) {
							l.countDown();
						}
					});
					server.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		l.await();
		driver = new HtmlUnitDriver();
	}
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
		server.stop();
	}
	
	@Test
	public void testReinjection() throws Exception {
		String className = "pageLabel";
		testLabel(className);
		className = "panelLabel";
		testLabel(className);
	}
	
	private void testLabel(String className) throws Exception {
		HotSwapper.swapClasses(HomePage.class, HomePage.class.getName());
		HotSwapper.swapClasses(HomePanel.class, HomePanel.class.getName());
		
		String url = "http://localhost:8080/app/";
		driver.get(url);
		WebElement pageLabel = driver.findElementByClassName(className);
		assertEquals("Startup page not showing expected value", "0", pageLabel.getText());
		
		HotSwapper.swapClasses(HomePage.class, HomePage2.class.getName());
		HotSwapper.swapClasses(HomePanel.class, HomePanel2.class.getName());
		
		driver.navigate().refresh();
		pageLabel = driver.findElementByClassName(className);
		assertEquals("Swapped page not showing service value", "1", pageLabel.getText());
	}
}
