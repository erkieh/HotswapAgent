package org.hotswap.agent.plugin.staticinit;

/**
 * Lock object used to share data between the StaticInitPlugin and StaticInitWorkerThread
 * 
 * @author Erki Ehtla
 * 
 */
public class StaticInitWorkerLock {
	public boolean threadExiting = true;
	public boolean threadCanExit = true;
}
