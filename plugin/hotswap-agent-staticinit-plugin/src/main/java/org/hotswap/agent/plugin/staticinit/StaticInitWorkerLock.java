package org.hotswap.agent.plugin.staticinit;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lock object used to share data between the StaticInitPlugin and StaticInitWorkerThread
 * 
 * @author Erki Ehtla
 * 
 */
public class StaticInitWorkerLock {
	public boolean threadExiting = true;
	public boolean threadCanExit = true;
	
	public final ConcurrentLinkedQueue<StaticInitKeyValue> newCodes = new ConcurrentLinkedQueue<>();
	public final ConcurrentLinkedQueue<StaticInitKeyValue> executedCodes = new ConcurrentLinkedQueue<>();
	public final Map<StaticInitKey, Integer> executedCodeMap = new HashMap<>();
	public final ConcurrentLinkedQueue<ClassLoader> classLoaderQueue = new ConcurrentLinkedQueue<>();
	public final List<WeakReference<ClassLoader>> classLoaderWeakList = new ArrayList<>();
	public final Map<ClassLoader, Object> classLoaderMap = new WeakHashMap<>();
}
