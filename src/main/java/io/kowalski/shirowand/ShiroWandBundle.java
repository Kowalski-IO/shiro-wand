package io.kowalski.shirowand;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.env.DefaultWebEnvironment;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.secnod.shiro.jersey.AuthInjectionBinder;
import org.secnod.shiro.jersey.AuthorizationFilterFeature;
import org.secnod.shiro.jersey.SubjectFactory;

import com.google.inject.Injector;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * ShiroWandBundle - Dropwizard bundle for adding Guice injection support to Shiro programmatically.
 * @author Brandon Kowalski
 * @version 0.1.0
 */
public class ShiroWandBundle implements Bundle {

	private final static String DEFAULT_URL_PATTERN = "/*";

	private final Injector injector;
	private final Set<Class<? extends Realm>> realmClasses;
	private final Set<String> urlPatterns;

	@SafeVarargs
	/**
	 * Constructor that takes Injector and varargs of Realms. Binds filter automatically to /*
	 * @param injector that "knows" how to build realms
	 * @param realmClasses to be created by injector
	 */
	public ShiroWandBundle(final Injector injector, final Class<? extends Realm>... realmClasses) {
		this(injector, new HashSet<Class<? extends Realm>>(Arrays.asList(realmClasses)));
	}

	/**
	 * Constructor that takes Injector and Set of Realms. Binds filter automatically to /*
	 * @param injector that "knows" how to build realms
	 * @param realmClasses to be created by injector
	 */
	public ShiroWandBundle(final Injector injector, final Set<Class<? extends Realm>> realmClasses) {
		this(injector, new HashSet<String>(Arrays.asList(DEFAULT_URL_PATTERN)), realmClasses);
	}

	@SafeVarargs
	/**
	 * Constructor that takes Injector, Set of Strings to be used as url patterns for filter and varargs of Realms.
	 * @param injector that "knows" how to build realms
	 * @param urlPatterns to be used by the Jersey filter
	 * @param realmClasses to be created by injector
	 */
	public ShiroWandBundle(final Injector injector, final Set<String> urlPatterns, final Class<? extends Realm>... realmClasses) {
		this(injector, urlPatterns, new HashSet<Class<? extends Realm>>(Arrays.asList(realmClasses)));
	}

	/**
	 * Constructor that takes Injector, Set of Strings to be used as url patterns for filter and Set of Realms.
	 * @param injector that "knows" how to build realms
	 * @param urlPatterns to be used by the Jersey filter
	 * @param realmClasses to be created by injector
	 */
	public ShiroWandBundle(final Injector injector, final Set<String> urlPatterns, final Set<Class<? extends Realm>> realmClasses) {
		this.injector = injector;
		this.realmClasses = realmClasses;
		this.urlPatterns = urlPatterns;
	}

	@Override
	public void run(final Environment environment)  {
		final ResourceConfig resourceConfig = environment.jersey().getResourceConfig();

		resourceConfig.register(new AuthorizationFilterFeature());
		resourceConfig.register(new SubjectFactory());
		resourceConfig.register(new AuthInjectionBinder());


		final Collection<Realm> realms = createRealms();
		final DefaultWebEnvironment webEnv = buildWebEnvironment(realms);
		final Filter shiroFilter = buildFilter(webEnv);

		environment.servlets().addFilter("ShiroFilter", shiroFilter).addMappingForUrlPatterns(
				EnumSet.allOf(DispatcherType.class), false, urlPatterns.toArray(new String[urlPatterns.size()]));

	}

	@Override
	public void initialize(final Bootstrap<?> bootstrap) {

	}

	/**
	 * Builds the Web Environment and Security Manager
	 * @param realms used to create security manager
	 * @return Web environment containing security manager.
	 */
	protected DefaultWebEnvironment buildWebEnvironment(final Collection<Realm> realms) {
		final DefaultWebEnvironment shiroEnv = new DefaultWebEnvironment();
		final WebSecurityManager securityManager = new DefaultWebSecurityManager(realms);
		shiroEnv.setSecurityManager(securityManager);

		return shiroEnv;
	}

	/**
	 * Builds and binds the Jersey filter
	 * @param environment created by buildWebEnvironment
	 * @return the Jersey filter to be bound
	 */
	protected Filter buildFilter(final DefaultWebEnvironment environment) {
		final AbstractShiroFilter shiroFilter = new AbstractShiroFilter() {
			@Override
			public void init() throws Exception {
				setSecurityManager(environment.getWebSecurityManager());
				setFilterChainResolver(environment.getFilterChainResolver());
			}
		};
		return shiroFilter;
	}

	/**
	 * Builds all of the realms specified using the Injector.
	 * @return a collection of realms to be used by the security manager.
	 */
	protected Collection<Realm> createRealms() {
		final Set<Realm> realms = new HashSet<Realm>();

		for (final Class<? extends Realm> clazz : realmClasses) {
			final Realm injectedRealm = injector.getInstance(clazz);
			realms.add(injectedRealm);
		}

		return realms;
	}

}
