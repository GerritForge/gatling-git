# Configuration

Configuration is provided by the [application.conf](src/test/resources/application.conf) file.
You can also override configuration values by making the relevant environment variable available.

```bash
GIT_HTTP_PASSWORD="foo" \
GIT_HTTP_USERNAME="bar" \
TMP_BASE_PATH="/tmp" \
HTTP_SERVER_URI="http://localhost:8080" \
GIT_SSH_PRIVATE_KEY_PATH="/path/to/ssh/id_rsa" \
SSH_SERVER_URI="ssh://admin@localhost:29418" \
sbt "gatling:test"
```
