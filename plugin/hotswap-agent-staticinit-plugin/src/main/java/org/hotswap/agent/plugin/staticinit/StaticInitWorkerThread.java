package org.hotswap.agent.plugin.staticinit;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hotswap.agent.logging.AgentLogger;

/**
 * Executes Static initializer method copies when their code has changed from previous execution
 * 
 * @author Erki Ehtla
 * 
 */
public final class StaticInitWorkerThread extends Thread {
	public static final int SLEEP_TIME = 100;
	private static final AgentLogger LOGGER = AgentLogger.getLogger(StaticInitPlugin.class);
	/**
	 * Codes of new static initializers
	 */
	private final Map<StaticInitKey, Integer> changedCodeMap = new LinkedHashMap<>();
	private final StaticInitWorkerLock lock;
	
	/**
	 * 
	 * @param lock
	 *            Lock object used to share data between the StaticInitPlugin and StaticInitWorkerThread
	 */
	public StaticInitWorkerThread(StaticInitWorkerLock lock) {
		this.lock = lock;
	}
	
	@Override
	public void run() {
		int exceptionCount = 0;
		while (true) {
			try {
				processClassLoaders();
				processExecutedCodes();
				processNewCodes();
				invokeInitializers();
				if (!needMoreIterations())
					return;
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					return;
				}
				if (exceptionCount > 0)
					exceptionCount = 0;
			} catch (Throwable e) {
				LOGGER.error("Error running StaticInitWorkerThread", e);
				exceptionCount++;
				// Some wierd exception have been known to come from there. Ignore the first 2
				if (exceptionCount < 3)
					continue;
				lock.threadExiting = true;
				throw e;
			}
		}
	}
	
	/**
	 * Checks for ClassLoaders that have been discardded and removes them from executedCodeMap
	 */
	private void processClassLoaders() {
		while (!lock.classLoaderQueue.isEmpty()) {
			ClassLoader loader = lock.classLoaderQueue.poll();
			if (!lock.classLoaderMap.containsKey(loader)) {
				lock.classLoaderWeakList.add(new WeakReference<ClassLoader>(loader));
				lock.classLoaderMap.put(loader, null);
			}
		}
		
		boolean foundDiscardedLoader = false;
		List<WeakReference<ClassLoader>> classLoaders = lock.classLoaderWeakList;
		for (int i = 0; i < classLoaders.size();) {
			WeakReference<ClassLoader> weakReference = classLoaders.get(i);
			if (weakReference.get() == null) {
				foundDiscardedLoader = true;
				classLoaders.remove(i);
			} else {
				i++;
			}
		}
		if (foundDiscardedLoader) {
			List<StaticInitKey> keysToBeRemoved = new ArrayList<>();
			for (StaticInitKey key : changedCodeMap.keySet()) {
				if (key.getLoader() == null) {
					keysToBeRemoved.add(key);
				}
			}
			for (StaticInitKey key : keysToBeRemoved) {
				changedCodeMap.remove(key);
			}
		}
	}
	
	private boolean needMoreIterations() {
		if (changedCodeMap.isEmpty()) {
			synchronized (lock) {
				if (lock.threadCanExit) {
					lock.threadExiting = true;
					return false;
				} else {
					lock.threadCanExit = true;
					return true;
				}
			}
		}
		return true;
	}
	
	/**
	 * Invokes static initializer copies in changedInitializers
	 */
	private void invokeInitializers() {
		for (Entry<StaticInitKey, Integer> entry : changedCodeMap.entrySet()) {
			Method[] methods = entry.getKey().getClazz().getDeclaredMethods();
			for (Method method : methods) {
				if (method.getName().startsWith(StaticInitPlugin.INIT_METHOD_NAME)) {
					if (method.getName().endsWith(entry.getValue().toString())) {
						try {
							method.invoke(null);
							LOGGER.info("{} initializer has been invoked", entry.getKey().getClazz().getName());
							lock.executedCodes.add(new StaticInitKeyValue(entry.getKey(), entry.getValue()));
						} catch (Throwable e) {
							LOGGER.error("Error executing static init method copy of {}", e, entry);
						}
					} else
						break;
				}
			}
		}
	}
	
	/**
	 * If the new codes are not null and differ from previous ones, then they are added to the changedCodeMap
	 */
	private void processNewCodes() {
		while (!lock.newCodes.isEmpty()) {
			StaticInitKeyValue newMethod = lock.newCodes.poll();
			Integer executedCode = lock.executedCodeMap.get(newMethod.getKey());
			if (executedCode == null || newMethod.getStaticInitCode() != executedCode) {
				changedCodeMap.put(newMethod.getKey(), newMethod.getStaticInitCode());
			}
		}
	}
	
	/**
	 * Adds executedCodes to the executedCodeMap. If the same code exists in the changedCodeMap, then it is removed from
	 * there.
	 */
	private void processExecutedCodes() {
		while (!lock.executedCodes.isEmpty()) {
			StaticInitKeyValue executedMethod = lock.executedCodes.poll();
			lock.executedCodeMap.put(executedMethod.getKey(), executedMethod.getStaticInitCode());
			Integer newCode = changedCodeMap.get(executedMethod.getKey());
			if (newCode != null && executedMethod.getStaticInitCode() == newCode) {
				changedCodeMap.remove(executedMethod.getKey());
			}
		}
	}
}