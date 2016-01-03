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
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.secnod.shiro.jersey.AuthInjectionBinder;
import org.secnod.shiro.jersey.AuthorizationFilterFeature;
import org.secnod.shiro.jersey.SubjectFactory;

import com.google.inject.Injector;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * ShiroWandBundle - Dropwizard bundle for adding Guice injection support to
 * Shiro programmatically.
 *
 * @author Brandon Kowalski
 * @version 0.1.0
 * @param <T>
 *            Configuration Type from Dropwizard.
 */
public class ShiroWandBundle<T extends Configuration> implements ConfiguredBundle<T> {

	private final static String DEFAULT_URL_PATTERN = "/*";

	private final Injector injector;
	private final Set<Class<? extends Realm>> realmClasses;
	private final Set<String> urlPatterns;
	private final long sessionTimeout;
	private final String cookieName;

	/**
	 * Constructor that takes Injector, Set of Strings to be used as url
	 * patterns for filter and Set of Realms.
	 *
	 * @param injector
	 *            that "knows" how to build realms
	 * @param urlPatterns
	 *            to be used by the Jersey filter
	 * @param realmClasses
	 *            to be created by injector
	 */
	private ShiroWandBundle(final Injector injector, final Set<String> urlPatterns,
			final Set<Class<? extends Realm>> realmClasses, final long sessionTimeout, final String cookieName) {
		this.injector = injector;
		this.realmClasses = realmClasses;
		this.urlPatterns = urlPatterns;
		this.sessionTimeout = sessionTimeout;
		this.cookieName = cookieName;
	}

	public static class Builder<T extends Configuration> {

		private final Set<String> urlPatterns;
		private final Set<Class<? extends Realm>> realmClasses;

		private Injector injector;
		private long sessionTimeout;
		private String cookieName;

		private Builder() {
			urlPatterns = new HashSet<String>();
			realmClasses = new HashSet<Class<? extends Realm>>();
		}

		public final Builder<T> bindInjector(final Injector injector) {
			this.injector = injector;
			return this;
		}

		public final Builder<T> addRealm(final Class<? extends Realm> realmClass) {
			this.realmClasses.add(realmClass);
			return this;
		}

		@SafeVarargs
		public final Builder<T> addRealms(final Class<? extends Realm>... realmVarargs) {
			this.realmClasses.addAll(Arrays.asList(realmVarargs));
			return this;
		}

		public final Builder<T> addRealms(final Set<Class<? extends Realm>> realmSet) {
			this.realmClasses.addAll(realmSet);
			return this;
		}

		public final Builder<T> addUrlPattern(final String urlPattern) {
			this.urlPatterns.add(urlPattern);
			return this;
		}

		@SafeVarargs
		public final Builder<T> addUrlPatterns(final String... urlPatternsVarArg) {
			this.urlPatterns.addAll(Arrays.asList(urlPatternsVarArg));
			return this;
		}

		public final Builder<T> addUrlPatterns(final Set<String> urlPatternSet) {
			this.urlPatterns.addAll(urlPatternSet);
			return this;
		}

		public final Builder<T> setSessionTimeout(final long sessionTimeout) {
			this.sessionTimeout = sessionTimeout;
			return this;
		}

		public final Builder<T> setCookieName(final String cookieName) {
			this.cookieName = cookieName;
			return this;
		}

		public final ShiroWandBundle<T> build() {
			if (urlPatterns.isEmpty()) {
				urlPatterns.add(DEFAULT_URL_PATTERN);
			}

			if (cookieName == null || cookieName.isEmpty()) {
				cookieName = "JSESSIONID";
			}

			return new ShiroWandBundle<T>(injector, urlPatterns, realmClasses, sessionTimeout, cookieName);
		}

	}

	public static <T extends Configuration> Builder<T> newBuilder() {
		return new Builder<>();
	}

	@Override
	public void run(final T configuration, final Environment environment) throws Exception {
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
	 *
	 * @param realms
	 *            used to create security manager
	 * @return Web environment containing security manager.
	 */
	protected DefaultWebEnvironment buildWebEnvironment(final Collection<Realm> realms) {

		final DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
		sessionManager.setGlobalSessionTimeout(sessionTimeout);
		sessionManager.getSessionIdCookie().setName(cookieName);

		final DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realms);
		securityManager.setSessionManager(sessionManager);


		final DefaultWebEnvironment shiroEnv = new DefaultWebEnvironment();
		shiroEnv.setSecurityManager(securityManager);

		return shiroEnv;
	}

	/**
	 * Builds and binds the Jersey filter
	 *
	 * @param environment
	 *            created by buildWebEnvironment
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
	 *
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
