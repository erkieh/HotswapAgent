package org.hotswap.agent.plugin.staticinit;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;

/**
 * Re-executes the static initializer method, if the length of it's body is changed. Note: All the initialization
 * expressions of static fields get compiled into the static initializer (<clinit>) block. If you change one
 * initializing method call to another, then the length won't change. If you add static field like "int t = 1" then the
 * length changes and it is re-executed.
 * 
 * @author Erki Ehtla
 * 
 */
@Plugin(name = "StaticInit", description = "Static Init caller", testedVersions = { "" }, expectedVersions = { "all" })
public class StaticInitPlugin {
	public static final String INIT_METHOD_NAME = "HOTSWAP_AGENT_CLINIT$$METHOD";
	
	private static final StaticInitWorkerLock threadLock = new StaticInitWorkerLock();
	private static final ConcurrentLinkedQueue<StaticInitKeyValue> newCodes = new ConcurrentLinkedQueue<>();
	private static final ConcurrentLinkedQueue<StaticInitKeyValue> executedCodes = new ConcurrentLinkedQueue<>();
	private static final Map<StaticInitKey, Integer> executedCodeMap = new WeakHashMap<>();
	private static Thread thread;
	
	/**
	 * Start a static block exectuing thread
	 */
	private static void startThread() {
		boolean createNewThread = false;
		synchronized (threadLock) {
			if (threadLock.threadExiting) {
				threadLock.threadExiting = false;
				createNewThread = true;
			} else {
				threadLock.threadCanExit = false;
			}
		}
		if (createNewThread) {
			thread = new StaticInitWorkerThread(executedCodes, newCodes, executedCodeMap, threadLock);
			thread.start();
		}
	}
	
	/**
	 * If the moethod has a static initializer, then this method stores the length of the static initializer block and
	 * creates a new method containing the static initializer code
	 */
	@OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE, skipAnonymous = false)
	public static CtClass transformRedefinitions(final CtClass ct, String name, final ClassLoader loader, Class<?> clazz)
			throws Exception {
		if (name.contains("$$") || name.startsWith("com/sun/proxy/$Proxy") || clazz.isSynthetic()) {
			return ct;
		}
		CtConstructor classInitializer = ct.getClassInitializer();
		Integer initCode = null;
		if (classInitializer != null) {
			initCode = getInitBlockCode(classInitializer);
		}
		if (initCode != null) {
			newCodes.add(new StaticInitKeyValue(loader, name, clazz, initCode));
			CtMethod method = classInitializer.toMethod(INIT_METHOD_NAME + initCode, ct);
			method.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
			ct.addMethod(method);
			startThread();
		}
		return ct;
	}
	
	/**
	 * If the moethod has a static initializer, then this method stores the length of the static initializer block
	 */
	@OnClassLoadEvent(classNameRegexp = "(?!(java/|javax/|sun/|oracle/)).*", events = LoadEvent.DEFINE, skipAnonymous = false)
	public static void transformDefinitions(final CtClass ct, String name, final ClassLoader loader) throws Exception {
		if (name.contains("$$") || name.startsWith("com/sun/proxy/$Proxy")) {
			return;
		}
		CtConstructor classInitializer = ct.getClassInitializer();
		Integer initCode = null;
		if (classInitializer != null) {
			initCode = getInitBlockCode(classInitializer);
		}
		if (initCode != null) {
			executedCodes.add(new StaticInitKeyValue(loader, name, initCode));
		}
	}
	
	/**
	 * 
	 * @param classInitializer
	 * @return the length of the init block
	 */
	private static Integer getInitBlockCode(CtConstructor classInitializer) {
		// Can not use simple hashcode because constant pool references can change when modifyng code outside the static
		// init block
		// byte[] code = classInitializer.getMethodInfo().getCodeAttribute().getCode();
		// System.out.println(Arrays.hashCode(code));
		// ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// DataOutputStream w = new DataOutputStream(baos);
		// try {
		// classInitializer.getMethodInfo().getConstPool().write(w);
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
		// System.out.println(Arrays.hashCode(baos.toByteArray()));
		return classInitializer.getMethodInfo().getCodeAttribute().getCodeLength();
	}
}
