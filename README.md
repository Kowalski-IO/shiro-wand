# shiro-wand

A bundle for securing [Dropwizard](http://dropwizard.io) with [Apache Shiro](http://shiro.apache.org) with [Guice](https://github.com/google/guice) sprinkled on top!

## Why do I want this?

With this bundle installed all of your Shiro Realms will have JSR-330 support via Guice. 

This gets you quite a bit of functionality.

1. You can now use @Inject in your Realms such as a UserService to query your database.
2. Services that are injected follow the @UnitOfWork annotation (i.e. if a resource calls a SecurityService that uses a service that accesses a database it will use the connection provided by @UnitOfWork).

You will lose some flexibility as all of the Realms will be configured programmatically as this bundle does not support shiro.ini or properties passed in via the Dropwizard server configuration yaml file. While not optimal, having JSR-330 support in realms at the cost of some configuration flexibility is worth it.

## How to add shiro-wand to your project.

Add the following dependency to `pom.xml`.

```xml
<dependency>
  <groupId>io.kowalski</groupId>
  <artifactId>shiro-wizard</artifactId>
  <version>LATEST_VERSION_HERE</version>
</dependency>
```

Currently the latest version of shiro-wand is <b>0.1.0</b>.

Note: This plugin comes with Guice 4.0, Shiro Core and Shiro Web 1.2.4 and finally [Shiro-Jersey](https://github.com/silb/shiro-jersey) 0.2.0.

Additionally, this can be used alongside [dropwizard-guice](https://github.com/HubSpot/dropwizard-guice) (which is the original reason for building this bundle).

Depending on your Dropwizard project layout, this should be all you need to get started.

# Bundle Usage

After adding the above Maven dependency, open the class in your project that extends io.dropwizard.Application.

All you have to do is add an additional bundle in the overridden initializate method as such:

### Non-Dropwizard Guice Example

```java
@Override
public void initialize(final Bootstrap<ServerConfiguration> bootstrap) {
	bootstrap.addBundle(new ShiroWandBundle<ServerConfiguration>(yourGuiceInjectorInstance, 
	  YourRealm1.class, YourRealm2.class....));
}
```

The injector that your pass must obviously "know" how to create the necessary objects needed to instantiate your realms.

Also as the above example demostrates you may pass in one or many classes to the bundle's constructor. 

### Dropwizard Guice Example

If you wish to use this alongside dropwizard-guice do the following:

```java
@Override
public void initialize(final Bootstrap<ServerConfiguration> bootstrap) {
  bootstrap.addBundle(guiceBundle);
	bootstrap.addBundle(new ShiroWandBundle<ServerConfiguration>(yourGuiceInjectorInstance, 
	  YourRealm1.class, YourRealm2.class....));
}
```

1. Instantiate the Guice Bundle per the dropwizard-guice README.
2. Add the Guice Bundle to your Dropwizard application bootstrap as shown above.
3. Add the ShiroWandBundle to your Dropwizard application bootstrap as shown above.

<b>You must add the Guice Bundle to your Dropwizard application before instantiating the ShiroWebBundle.</b>

Failing to do so will pass in a null Injector as it is not fully instantiated until it is added to the application.

# Questions, comments, issues, hooplah?
For questions, comments & hooplah reach out on Twitter to [@BrandonKowalski](https://twitter.com/BrandonKowalski]

For issues please make a note of it in the Github issues tracker.
