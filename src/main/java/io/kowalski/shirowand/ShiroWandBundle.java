package io.kowalski.shirowand;

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

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ShiroWandBundle<T> implements ConfiguredBundle<T> {

	private final Injector injector;
	private final Class<? extends Realm>[] realmClasses;

	@SafeVarargs
	public ShiroWandBundle(final Injector injector, final Class<? extends Realm>... realmClasses) {
		this.injector = injector;
		this.realmClasses = realmClasses;
	}

	@Override
	public void run(final T configuration, final Environment environment) throws Exception {
		final ResourceConfig resourceConfig = environment.jersey().getResourceConfig();

		resourceConfig.register(new AuthorizationFilterFeature());
		resourceConfig.register(new SubjectFactory());
		resourceConfig.register(new AuthInjectionBinder());

		final Filter shiroFilter = buildFilter(configuration);

		// TODO allow the url pattern to be configurable.
		environment.servlets().addFilter("ShiroFilter", shiroFilter)
		.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

	}

	@Override
	public void initialize(final Bootstrap<?> bootstrap) {

	}

	protected Filter buildFilter(final T configuration) {
		final AbstractShiroFilter shiroFilter = new AbstractShiroFilter() {
			@Override
			public void init() throws Exception {
				final Collection<Realm> realms = createRealms();
				final DefaultWebEnvironment shiroEnv = new DefaultWebEnvironment();
				final WebSecurityManager securityManager = new DefaultWebSecurityManager(realms);
				setSecurityManager(securityManager);
				setFilterChainResolver(shiroEnv.getFilterChainResolver());
			}
		};
		return shiroFilter;
	}

	protected Collection<Realm> createRealms() {
		final Set<Realm> realms = new HashSet<Realm>();

		for (final Class<? extends Realm> clazz : realmClasses) {
			final Realm injectedRealm = injector.getInstance(clazz);
			realms.add(injectedRealm);
		}

		return realms;
	}

}
