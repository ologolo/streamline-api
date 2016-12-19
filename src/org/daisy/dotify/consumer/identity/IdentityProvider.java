package org.daisy.dotify.consumer.identity;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.daisy.dotify.api.identity.IdentificationFailedException;
import org.daisy.dotify.api.identity.IdentifierFactory;
import org.daisy.dotify.api.identity.IdentityProviderService;
import org.daisy.dotify.api.tasks.AnnotatedFile;
import org.daisy.dotify.api.tasks.DefaultAnnotatedFile;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

@Component
public class IdentityProvider implements IdentityProviderService {
	private final List<IdentifierFactory> filters;
	private static final Logger logger = Logger.getLogger(IdentityProvider.class.getCanonicalName());
	
	public IdentityProvider() {
		this.filters = new CopyOnWriteArrayList<>();
	}
	
	/**
	 * <p>
	 * Creates a new IdentityProviderService and populates it using the SPI (java
	 * service provider interface).
	 * </p>
	 * 
	 * <p>
	 * In an OSGi context, an instance should be retrieved using the service
	 * registry. It will be registered under the IdentityProviderService
	 * interface.
	 * </p>
	 * 
	 * @return returns a new IdentityProviderService
	 */
	public static final IdentityProviderService newInstance() {
		IdentityProvider ret = new IdentityProvider();
		Iterator<IdentifierFactory> i = ServiceLoader.load(IdentifierFactory.class).iterator();
		while (i.hasNext()) {
			IdentifierFactory factory = i.next();
			factory.setCreatedWithSPI();
			ret.addFactory(factory);
		}
		return ret;
	}
	
	@Reference(type = '*')
	public void addFactory(IdentifierFactory factory) {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Adding factory: " + factory);
		}
		filters.add(factory);
	}

	// Unbind reference added automatically from addFactory annotation
	public void removeFactory(IdentifierFactory factory) {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Removing factory: " + factory);
		}
		filters.remove(factory);
	}

	@Override
	public AnnotatedFile identify(File in) {
		AnnotatedFile f = DefaultAnnotatedFile.create(in);

		// get a list of factories
		List<IdentifierFactory> factories = new ArrayList<>(filters);		
		while (!factories.isEmpty()) {
			try {
				f = identify(f, factories);
			} catch (IdentificationFailedException e) {
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "No matching identifier factories.", e);
				}
				break;
			}
		}
		return f;
	}

	private AnnotatedFile identify(AnnotatedFile f, List<IdentifierFactory> factories) throws IdentificationFailedException {
		IdentificationFailedException ex = new IdentificationFailedException();
		for (IdentifierFactory id : factories) {
			if (id.accepts(f)) {
				try {
					AnnotatedFile x = id.newIdentifier().identify(f);
					// identification was successful, remove this from future iterations
					factories.remove(id);
					return x;
				} catch (IdentificationFailedException e) {
					ex.addSuppressed(e);
				}
			}
		}
		throw ex;
	}

}