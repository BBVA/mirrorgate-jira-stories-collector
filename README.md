# MirrorGate collector for Jira

![MirrorGate](./media/images/logo-ae.png)

## Running the collector

you can execute the collector using Docker

```sh
docker run -e "JIRA_URL=http://my.jira.corp" -e "JIRA_USERNAME=admin" -e "JIRA_PASSWORD=aaaa" -e "MIRRORGATE_URL=http://mirrorgate.corp/mirrorgate" bbvaae/mirrorgate-jira-stories-collector
```

You can also specify MIRRORGATE_USERNAME and MIRRORGATE_PASSWORD if it's secured.

## Configuring

Check [application.properties](./src/main/resources/application.properties) file to check for other configuration options.

Note you can change the property names so that it's letters are uppercase and using underscore instead of dots to override them with env vars. For example `jira.url` in the `application.properties` can be overriden with `JIRA_URL` env var.

# Development instructions

Run unit tests

    ./gradlew test

Create an HPI file to install in Jenkins (HPI file will be in `target/mirrorgate-publisher.hpi`).

    ./gradlew clean build
