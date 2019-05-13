# Configuration

Configuration is provided by the [application.conf](src/test/resources/application.conf) file.
You can also override configuration values by making the relevant environment variable available.

```bash
GIT_HTTP_PASSWORD="foo" \
GIT_HTTP_USERNAME="bar" \
TMP_BASE_PATH="/tmp" \
sbt "gatling:test"
```

## Configurable properties

### http.password [GIT_HTTP_PASSWORD]
password to be used when performing git operations over HTTP

Default: `default_password`

### http.username [GIT_HTTP_USERNAME]
user to be used when performing git operations over HTTP

Default: `default_username`


### tmpFiles.basePath [TMP_BASE_PATH]
base path where to persist work on disk (i.e. clones)

Default: `/tmp`