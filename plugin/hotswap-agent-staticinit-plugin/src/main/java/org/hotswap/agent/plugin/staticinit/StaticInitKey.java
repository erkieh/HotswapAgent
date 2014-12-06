package org.hotswap.agent.plugin.staticinit;

/**
 * if both instance have Class field initialized, then these are compared for equality. Otherwise ClassLoader and name
 * are used.
 * 
 * @author Erki Ehtla
 * 
 */
public class StaticInitKey {
	
	public StaticInitKey(ClassLoader loader, String name) {
		this.loader = loader;
		this.name = name;
	}
	
	public StaticInitKey(ClassLoader loader, String name, Class<?> clazz) {
		this.clazz = clazz;
		this.loader = loader;
		this.name = name;
	}
	
	private ClassLoader loader;
	private String name;
	private Class<?> clazz;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loader == null) ? 0 : loader.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StaticInitKey other = (StaticInitKey) obj;
		if (clazz == other.clazz || loader == other.loader
				&& (name == other.name || name != null && name.equals(other.name))) {
			return true;
		}
		return false;
	}
	
	public ClassLoader getLoader() {
		return loader;
	}
	
	public String getName() {
		return name;
	}
	
	public Class<?> getClazz() {
		return clazz;
	}
	
	@Override
	public String toString() {
		return "StaticInitKey [loader=" + loader + ", name=" + name + ", clazz=" + clazz + "]";
	}
	
}