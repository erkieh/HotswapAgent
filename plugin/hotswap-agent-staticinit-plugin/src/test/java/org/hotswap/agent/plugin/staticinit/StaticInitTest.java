package org.hotswap.agent.plugin.staticinit;

import static org.junit.Assert.*;

import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.junit.Test;

public class StaticInitTest {
	private static final int SLEEP_TIME = StaticInitWorkerThread.SLEEP_TIME + 100;
	
	public static class A {
		static int a;
	}
	
	public static class A2 {
		static int a = 1;
	}
	
	@Test
	public void staticInitChanged() throws Exception {
		assertEquals("Wrong value for field A.a before swap", 0, A.a);
		HotSwapper.swapClasses(A.class, A2.class.getName());
		Thread.sleep(SLEEP_TIME);
		assertEquals("Wrong value for field A.a after swap", 1, A.a);
	}
}
