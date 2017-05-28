# MirrorGate collector for Jira

![MirrorGate](./media/images/logo-ae.png)

## Running the collector with docker

you can execute the collector using Docker

```sh
docker run -e "JIRA_URL=http://my.jira.corp" -e "JIRA_USERNAME=admin" -e "JIRA_PASSWORD=aaaa" -e "MIRRORGATE_URL=http://mirrorgate.corp/mirrorgate" bbvaae/mirrorgate-jira-stories-collector
```

You can also specify MIRRORGATE_USERNAME and MIRRORGATE_PASSWORD if it's secured.

## Running in Amazon Lambda

Create a lambda with the folowing handler class `com.bbva.arq.devops.ae.mirrorgate.collectors.jira.LambdaHandler`. Note it will execute only once, so you will have to use a timed trigger to execute it eventually.

## Configuring

Check [application.properties](./src/main/resources/application.properties) file to check for other configuration options.

Note you can change the property names so that it's letters are uppercase and using underscore instead of dots to override them with env vars. For example `jira.url` in the `application.properties` can be overriden with `JIRA_URL` env var.

# Development instructions

Run build and test

```sh
./gradlew clean build
```

You will get a jar file generated in `build/libs/`
