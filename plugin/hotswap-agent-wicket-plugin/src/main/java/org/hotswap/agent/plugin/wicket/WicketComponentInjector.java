package org.hotswap.agent.plugin.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hotswap.agent.logging.AgentLogger;

/**
 * Handles the injection of values into Injector supported fields. IVisitor visits all components and calls
 * Injector.inject on them,
 * 
 * @author Erki Ehtla
 * 
 */
public class WicketComponentInjector implements IVisitor<Component, Void> {
	private static AgentLogger LOGGER = AgentLogger.getLogger(WicketComponentInjector.class);
	private final Injector injector;
	
	/**
	 * 
	 * @param injector
	 *            Injector used for injection
	 */
	public WicketComponentInjector(Injector injector) {
		if (injector == null) {
			throw new IllegalArgumentException("injector may not be null");
		}
		this.injector = injector;
	}
	
	@Override
	public void component(Component object, IVisit<Void> visit) {
		injector.inject(object);
	}
	
	/**
	 * Injects values intosupplied argument and its children if argument is a MarkupContainer.
	 * 
	 * @param o
	 *            MarkupContainer to inject into
	 */
	public static void inject(java.lang.Object o) {
		if (o != null && o instanceof MarkupContainer) {
			try {
				Injector injector = Injector.get();
				if (injector != null) {
					injector.inject(o);
					((MarkupContainer) o).visitChildren(new WicketComponentInjector(injector));
				}
			} catch (Exception e) {
				LOGGER.error("Unable to inject Spring beans", e);
			}
		}
	}
}
