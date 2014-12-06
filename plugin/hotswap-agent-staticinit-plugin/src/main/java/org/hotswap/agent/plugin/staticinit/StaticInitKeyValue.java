package org.hotswap.agent.plugin.staticinit;

/**
 * StaticInitKey paired with a code for a static init block
 * 
 * @author Erki Ehtla
 * 
 */
public class StaticInitKeyValue {
	public StaticInitKeyValue(ClassLoader loader, String name, int staticInitCode) {
		this.key = new StaticInitKey(loader, name);
		this.staticInitCode = staticInitCode;
	}
	
	public StaticInitKeyValue(ClassLoader loader, String name, Class<?> clazz, Integer staticInitCode) {
		this.key = new StaticInitKey(loader, name, clazz);
		this.staticInitCode = staticInitCode;
	}
	
	public StaticInitKeyValue(StaticInitKey key, Integer staticInitCode) {
		this.key = key;
		this.staticInitCode = staticInitCode;
	}
	
	private StaticInitKey key;
	private int staticInitCode;
	
	public StaticInitKey getKey() {
		return key;
	}
	
	public int getStaticInitCode() {
		return staticInitCode;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + staticInitCode;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		StaticInitKeyValue other = (StaticInitKeyValue) obj;
		if (staticInitCode != other.staticInitCode)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "StaticInitKeyValue [key=" + key + ", staticInitCode=" + staticInitCode + "]";
	}
}