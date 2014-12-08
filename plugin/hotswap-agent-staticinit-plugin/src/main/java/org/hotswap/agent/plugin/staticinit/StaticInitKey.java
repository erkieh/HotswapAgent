package org.hotswap.agent.plugin.staticinit;

import java.lang.ref.WeakReference;

/**
 * if both instance have Class field initialized, then these are compared for equality. Otherwise ClassLoader and name
 * are used.
 * 
 * @author Erki Ehtla
 * 
 */
public class StaticInitKey {
	private WeakReference<ClassLoader> loader;
	private String name;
	private WeakReference<Class<?>> clazz;
	private int hashCode;
	
	public StaticInitKey(ClassLoader loader, String name) {
		this.loader = new WeakReference<>(loader);
		this.name = name;
		this.hashCode = calculateHashCode(loader, name);
	}
	
	public StaticInitKey(ClassLoader loader, String name, Class<?> clazz) {
		this.clazz = new WeakReference<Class<?>>(clazz);
		this.loader = new WeakReference<>(loader);
		this.name = name;
		this.hashCode = calculateHashCode(loader, name);
	}
	
	public int calculateHashCode(ClassLoader loader, String name) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loader == null) ? 0 : loader.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		ClassLoader thisLoader = getLoader();
		if (thisLoader == null)
			return false;
		Class<?> thisClazz = getClazz();
		StaticInitKey other = (StaticInitKey) obj;
		
		ClassLoader otherLoader;
		if ((otherLoader = other.getLoader()) == null)
			return false;
		Class<?> otherClazz = other.getClazz();
		if (thisClazz == otherClazz || thisLoader == otherLoader
				&& (name == other.name || name != null && name.equals(other.name))) {
			return true;
		}
		return false;
	}
	
	public ClassLoader getLoader() {
		return loader.get();
	}
	
	public String getName() {
		return name;
	}
	
	public Class<?> getClazz() {
		return clazz.get();
	}
	
	@Override
	public String toString() {
		return "StaticInitKey [loader=" + getLoader() + ", name=" + name + ", clazz=" + getClazz() + "]";
	}
	
}