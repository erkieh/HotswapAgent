package org.hotswap.agent.plugin.wicket;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hotswap.agent.annotation.LoadEvent;
import org.hotswap.agent.annotation.OnClassLoadEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.javassist.CtClass;
import org.hotswap.agent.javassist.CtConstructor;
import org.hotswap.agent.javassist.CtMethod;
import org.hotswap.agent.javassist.CtPrimitiveType;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.PluginManagerInvoker;

/**
 * Reinjects Spring beans into @SpringBean annotated fields in Pages retrieved from the AbstractPageManager. Component
 * fields in these pages will also be reinjected. Other classes like IDataProvider-s which might be doing manual
 * injection in their constructors, will not be processed.
 * 
 * @author Erki Ehtla
 * 
 */
@Plugin(name = "Wicket", description = "Wicket spring bean field injector", testedVersions = { "6.0.0" }, expectedVersions = { "6.x, 1.5.x" })
public class WicketPlugin {
	private static AgentLogger LOGGER = AgentLogger.getLogger(WicketPlugin.class);
	private List<WeakReference<Object>> injectorClassMetaCaches = new ArrayList<>();
	
	@OnClassLoadEvent(classNameRegexp = "org.apache.wicket.Application")
	public static CtClass initPlugin(final CtClass ct) throws Exception {
		CtConstructor[] constructors = ct.getConstructors();
		for (CtConstructor ctConstructor : constructors) {
			ctConstructor.insertBeforeBody(PluginManagerInvoker.buildInitializePlugin(WicketPlugin.class));
		}
		return ct;
	}
	
	/**
	 * Intercepts pages retrieved from the AbstractPageManager.getPage. Fields in these instances will injected winth
	 * Injector
	 * 
	 * @param ct
	 * @return
	 * @throws Exception
	 */
	@OnClassLoadEvent(classNameRegexp = "org.apache.wicket.page.AbstractPageManager")
	public CtClass interceptPageLoad(final CtClass ct) throws Exception {
		CtMethod declaredMethod = ct.getDeclaredMethod("getPage", new CtClass[] { CtPrimitiveType.intType });
		declaredMethod.insertAfter(WicketComponentInjector.class.getName() + ". inject($_);");
		return ct;
	}
	
	/**
	 * Transform SpringComponentInjector so the SpringComponentInjector used in the WebApplication is processed in this
	 * plugin register method
	 * 
	 * @param ct
	 * @return
	 * @throws Exception
	 */
	@OnClassLoadEvent(classNameRegexp = "org.apache.wicket.spring.injection.annot.SpringComponentInjector")
	public CtClass recordSpringComponentInjector(final CtClass ct) throws Exception {
		CtConstructor[] constructors = ct.getConstructors();
		for (CtConstructor ctConstructor : constructors) {
			ctConstructor.insertAfter(PluginManagerInvoker.buildCallPluginMethod(WicketPlugin.class, "register",
					"this", "java.lang.Object"));
		}
		return ct;
	}
	
	/**
	 * Adds the SpringComponentInjector's ClassMetaCache to this plugin's injectorClassMetaCaches list
	 * 
	 * @param injector
	 *            SpringComponentInjector instance
	 * @throws Exception
	 */
	public void register(final Object injector) throws Exception {
		Field declaredField = injector.getClass().getSuperclass().getDeclaredField("cache");
		declaredField.setAccessible(true);
		Object object = declaredField.get(injector);
		synchronized (injectorClassMetaCaches) {
			injectorClassMetaCaches.add(new WeakReference<Object>(object));
		}
	}
	
	/**
	 * Clear field caches in the injector for redefined classes
	 * 
	 * @param clazz
	 * @param classLoader
	 * @throws Exception
	 */
	@OnClassLoadEvent(classNameRegexp = ".*", events = LoadEvent.REDEFINE)
	public void transformRedefinitions(final Class<?> clazz) throws Exception {
		synchronized (injectorClassMetaCaches) {
			for (int i = 0; i < injectorClassMetaCaches.size();) {
				WeakReference<Object> classMetaCacheReference = injectorClassMetaCaches.get(i);
				Object classMetaCache;
				if ((classMetaCache = classMetaCacheReference.get()) != null) {
					removeClassFromInjectorCache(clazz, classMetaCache);
					i++;
				} else {
					injectorClassMetaCaches.remove(i);
				}
			}
		}
	}
	
	/**
	 * Removes clazz cached Field[] from the ClassMetaCache
	 * 
	 * @param clazz
	 *            Class whose cached fields should be removed from ClassMetaCache
	 * @param classMetaCache
	 *            ClassMetaCache instance
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	private void removeClassFromInjectorCache(final Class<?> clazz, Object classMetaCache) throws NoSuchFieldException,
			IllegalAccessException {
		Field cacheField = classMetaCache.getClass().getDeclaredField("cache");
		cacheField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<ClassLoader, Map<String, Field[]>> cache = (Map<ClassLoader, Map<String, Field[]>>) cacheField
				.get(classMetaCache);
		synchronized (cache) {
			Map<String, Field[]> concurrentHashMap = cache.get(clazz.getClassLoader());
			if (concurrentHashMap != null) {
				Field[] removedFields = concurrentHashMap.remove(clazz.getName());
				if (removedFields != null)
					LOGGER.info("Field cache cleared for {}", clazz.getName());
			}
		}
	}
}
