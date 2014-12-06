//package org.hotswap.agent.plugin.wicket;
//
//Did not work because wickettester "requests" are processed differently
//
//import org.apache.wicket.util.tester.WicketTester;
//import org.hotswap.agent.plugin.hotswapper.HotSwapper;
//import org.hotswap.agent.plugin.wicket.app.HomePage;
//import org.hotswap.agent.plugin.wicket.app.HomePage2;
//import org.hotswap.agent.plugin.wicket.app.HomePanel;
//import org.hotswap.agent.plugin.wicket.app.HomePanel2;
//import org.hotswap.agent.plugin.wicket.app.services.WicketApplication;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = "classpath:wicketPluginApplicationContext.xml")
//public class TestHomePageWithWicketTester {
//	private WicketTester tester;
//	@Autowired
//	protected WicketApplication wicketApplication;
//	
//	@Before
//	public void setUp() {
//		tester = new WicketTester(wicketApplication);
//	}
//	
//	@Test
//	public void homepageRendersSuccessfully() throws Exception {
//		HomePage page = tester.startPage(HomePage.class);
//		tester.assertRenderedPage(HomePage.class);
//		tester.assertNoErrorMessage();
//		HotSwapper.swapClasses(HomePage.class, HomePage2.class.getName());
//		HotSwapper.swapClasses(HomePanel.class, HomePanel2.class.getName());
//		tester.startPage(page);
//		tester.assertNoErrorMessage();
//	}
// }
