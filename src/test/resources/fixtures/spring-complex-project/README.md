# Complex Spring regression fixture

This fixture is a static-analysis input for T-0903. It intentionally combines:

- multiple controllers and multiple/conditional MVC paths;
- confirmed Controller-to-Service and Service-to-Service dependencies;
- a confirmed service cycle;
- a configuration class and Spring Boot entry point;
- a uniquely selected `@Autowired` constructor among multiple constructors;
- ambiguous constructors, unsupported method injection, and an interface with
  multiple component implementations that must not be guessed as a target.

The fixture is analyzed only. Tests must not build or launch it, execute Maven,
or download its declared dependencies.
