# MirrorGate collector for Jira

![MirrorGate](./media/images/logo-ae.png)

## Running the collector with docker

you can execute the collector using Docker

```sh
docker run -e "JIRA_URL=http://my.jira.corp" -e "JIRA_USERNAME=admin" -e "JIRA_PASSWORD=aaaa" -e "MIRRORGATE_URL=http://mirrorgate.corp/mirrorgate" bbvaae/mirrorgate-jira-stories-collector
```

You can also specify MIRRORGATE_USERNAME and MIRRORGATE_PASSWORD if it's secured.

## Configuring the webhook

When configured to use the webserver (default is true), the collector exposes `/webhook/` endpoint that can be used to
avoid polling Jira every time and update the data when ever an issue edition happens.

To use this feature ensure to disable batch execution (see configuring) and keep web-environment enabled.
Then add the webhook to the jira server (you will need admin priviledges to do so) with the `issue_created`,
`issue_updated`, `issue_deleted` and `sprint_*` events enabled.
 
In the first execution the collector will attempt to gather the configured history.
  
## Polling instead of webhook

Alternatively you can set the `SPRING_PROFILES_ACTIVE=scheduled` env variable so that
the collector polls every configured amount of time (by default every minute) for changes.
  
Note that some inconsistencies can happen if you use the polling approach because issue deletions
and sprint changes will not be captured by JQL's that the collector executes against Jira.

## Configuring

Check [application.properties](./src/main/resources/application.properties) file to check for other configuration options.

Note you can change the property names so that it's letters are uppercase and using underscore instead of dots to override them with env vars. For example `jira.url` in the `application.properties` can be overriden with `JIRA_URL` env var.

# Development instructions

Run build and test

```sh
./gradlew clean build
```

You will get a jar file generated in `build/libs/`
