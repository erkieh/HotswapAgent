package org.hotswap.agent.plugin.staticinit;

import java.lang.reflect.Modifier;

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
 * length changes and the method is re-executed.
 * 
 * @author Erki Ehtla
 * 
 */
@Plugin(name = "StaticInit", description = "Static Init caller", testedVersions = { "" }, expectedVersions = { "all" })
public class StaticInitPlugin {
	public static final String INIT_METHOD_NAME = "HOTSWAP_AGENT_CLINIT$$METHOD";
	
	private static final StaticInitWorkerLock threadLock = new StaticInitWorkerLock();
	private static Thread thread;
	
	/**
	 * Start a static initializer executing thread
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
			thread = new StaticInitWorkerThread(threadLock);
			thread.start();
		}
	}
	
	/**
	 * If the moethod has a static initializer, then this method stores the length of the static initializer block and
	 * creates a new method containing the static initializer code
	 */
	@OnClassLoadEvent(classNameRegexp = "(?!(java/|javax/|sun/|oracle/)).*", events = LoadEvent.REDEFINE, skipAnonymous = false)
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
			String methodName = INIT_METHOD_NAME + initCode;
			CtMethod[] declaredMethods = ct.getDeclaredMethods();
			boolean hasmethod = false;
			for (CtMethod ctMethod : declaredMethods) {
				if (ctMethod.getName().equals(methodName)) {
					hasmethod = true;
					break;
				}
			}
			if (!hasmethod) {
				threadLock.newCodes.add(new StaticInitKeyValue(loader, name, clazz, initCode));
				CtMethod method = classInitializer.toMethod(methodName, ct);
				method.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
				ct.addMethod(method);
				startThread();
			}
		}
		return ct;
	}
	
	/**
	 * If the method has a static initializer, then this method stores the length of the static initializer block
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
			threadLock.executedCodes.add(new StaticInitKeyValue(loader, name, initCode));
		}
		if (!threadLock.classLoaderQueue.contains(loader)) {
			threadLock.classLoaderQueue.add(loader);
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
