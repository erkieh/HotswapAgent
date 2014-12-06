package org.hotswap.agent.plugin.staticinit;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	private final Map<StaticInitKey, Integer> executedCodeMap;
	/**
	 * Codes of new static initializers
	 */
	private final Map<StaticInitKey, Integer> changedCodeMap = new LinkedHashMap<>();
	private final ConcurrentLinkedQueue<StaticInitKeyValue> newCodes;
	private final ConcurrentLinkedQueue<StaticInitKeyValue> executedCodes;
	private final StaticInitWorkerLock lock;
	
	/**
	 * 
	 * @param executedCodes
	 *            Codes of executed static initializers
	 * @param newCodes
	 *            New codes of static initializers
	 * @param executedCodeMap
	 *            Codes of executed static initializers
	 * @param lock
	 *            Lock object used to share data between the StaticInitPlugin and StaticInitWorkerThread
	 */
	public StaticInitWorkerThread(ConcurrentLinkedQueue<StaticInitKeyValue> executedCodes,
			ConcurrentLinkedQueue<StaticInitKeyValue> newCodes, Map<StaticInitKey, Integer> executedCodeMap,
			StaticInitWorkerLock lock) {
		this.executedCodes = executedCodes;
		this.newCodes = newCodes;
		this.executedCodeMap = executedCodeMap;
		this.lock = lock;
	}
	
	@Override
	public void run() {
		int exceptionCount = 0;
		while (true) {
			try {
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
							executedCodes.add(new StaticInitKeyValue(entry.getKey(), entry.getValue()));
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
		while (!newCodes.isEmpty()) {
			StaticInitKeyValue newMethod = newCodes.poll();
			Integer executedCode = executedCodeMap.get(newMethod.getKey());
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
		while (!executedCodes.isEmpty()) {
			StaticInitKeyValue executedMethod = executedCodes.poll();
			executedCodeMap.put(executedMethod.getKey(), executedMethod.getStaticInitCode());
			Integer newCode = changedCodeMap.get(executedMethod.getKey());
			if (newCode != null && executedMethod.getStaticInitCode() == newCode) {
				changedCodeMap.remove(executedMethod.getKey());
			}
		}
	}
}