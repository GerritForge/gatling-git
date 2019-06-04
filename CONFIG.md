# Configuration

Configuration is provided by the [application.conf](src/test/resources/application.conf) file.
You can also override configuration values by making the relevant environment variable available.

```bash
GIT_HTTP_PASSWORD="foo" \
GIT_HTTP_USERNAME="bar" \
TMP_BASE_PATH="/tmp" \
GIT_SSH_PRIVATE_KEY_PATH="/path/to/ssh/id_rsa" \
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
test data (i.e. clones) used by the running scenario is stored in `TMP_BASE_PATH/TEST_DATA_DIRECTORY`.
`TMP_BASE_PATH` defines the base path of the location on filesystem

Default: `/tmp`

### tmpFiles.testDataDirectory [TEST_DATA_DIRECTORY]
test data (i.e. clones) used by the running scenario is stored in `TMP_BASE_PATH/TEST_DATA_DIRECTORY`.
`TEST_DATA_DIRECTORY` defines the directory of the location on filesystem

Default: `System.currentTimeMillis`

### ssh.private_key_path [GIT_SSH_PRIVATE_KEY_PATH]
Path to the ssh private key to be used for git operations over SSH

Default: `/tmp/ssh-keys/id_rsa`