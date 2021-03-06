package org.daisy.streamline.api.validity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.daisy.streamline.api.media.FileDetails;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Simple factory for instantiating a Validator based on one of its identifiers.
 * @author Joel Håkansson
 */
@Component
public class ValidatorFactoryMaker implements ValidatorFactoryMakerService {
	private static final Logger logger = Logger.getLogger(ValidatorFactoryMaker.class.getCanonicalName());
	private final List<ValidatorFactory> providers;
	private final Map<String, ValidatorFactory> map;
	
	/**
	 * Creates a new empty instance. This method is public because it is required by OSGi.
	 * In an SPI context, use newInstance()
	 */
	public ValidatorFactoryMaker() {
		providers = new CopyOnWriteArrayList<>();
		map = Collections.synchronizedMap(new HashMap<String, ValidatorFactory>());
	}

	/**
	 * <p>
	 * Creates a new ValidatorFactoryMaker and populates it using the SPI
	 * (java service provider interface).
	 * </p>
	 * 
	 * <p>
	 * In an OSGi context, an instance should be retrieved using the service
	 * registry. It will be registered under the ValidatorFactoryMakerService
	 * interface.
	 * </p>
	 * 
	 * @return returns a new ValidatorFactoryMaker
	 */
	public static ValidatorFactoryMaker newInstance() {
		ValidatorFactoryMaker ret = new ValidatorFactoryMaker();
		for (ValidatorFactory f : ServiceLoader.load(ValidatorFactory.class)) {
			f.setCreatedWithSPI();
			ret.addFactory(f);
		}
		return ret;
	}

	@SuppressWarnings("javadoc")
	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	public void addFactory(ValidatorFactory factory) {
		synchronized (map) {
			verifyMapIntegrity();
			providers.add(factory);
			for (String factoryId : factory.listIdentifiers()) {
				map.put(factoryId, factory);
			}
		}
	}

	// Unbind reference added automatically from addFactory annotation
	@SuppressWarnings("javadoc")
	public void removeFactory(ValidatorFactory factory) {
		// this is to avoid adding items to the cache that were removed while
		// iterating
		synchronized (map) {
			providers.remove(factory);
			map.clear();
		}
	}
	
	private void verifyMapIntegrity() {
		synchronized (map) {
			if (map.isEmpty() && !providers.isEmpty()) { // cache has been cleared, restore
				for (ValidatorFactory p : providers) {			
					for (String factoryId : p.listIdentifiers()) {
						map.put(factoryId, p);
					}
				}
			}
		}
	}

	/**
	 * Obtains a new instance of a Validator with the given identifier
	 * @param identifier a string that identifies the desired implementation
	 * @return returns a Validator for the given identifier, or null if none is found
	 */
	@Override
	public Validator newValidator(String identifier) {
		if (identifier==null) {
			return null;
		}
		ValidatorFactory template;
		synchronized (map) {
			verifyMapIntegrity();
			template = map.get(identifier);
		}
		if (template!=null) {
			try {
				return template.newValidator(identifier);
			} catch (ValidatorFactoryException e) {
				logger.log(Level.WARNING, "Failed to create validator.", e);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Optional<Validator> newValidator(FileDetails details) {
		if (details==null) {
			return Optional.empty();
		}
		return providers.stream()
				.filter(v->v.supportsDetails(details).isPresent())
				.sorted((a, b)->Double.compare(b.supportsDetails(details).get(), a.supportsDetails(details).get()))
				.findFirst()
				.map(v->{
					try {
						return v.newValidator(details);
					} catch (ValidatorFactoryException e) {
						logger.log(Level.WARNING, "Failed to create validator.", e);
						return null;
					}
				});
	}

	@Override
	public Collection<String> listIdentifiers() {
		Set<String> ret = new HashSet<>();
		for (ValidatorFactory p : providers) {
			ret.addAll(p.listIdentifiers());
		}
		return ret;
	}

}