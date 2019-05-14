# Configuration

Configuration is provided by the [application.conf](src/test/resources/application.conf) file.
You can also override configuration values by making the relevant environment variable available.

```bash
GIT_HTTP_PASSWORD="foo" \
GIT_HTTP_USERNAME="bar" \
TMP_BASE_PATH="/tmp" \
GIT_HTTP_SERVER_URI="http://localhost:8080" \
GIT_SSH_PRIVATE_KEY_PATH="/path/to/ssh/id_rsa" \
GIT_SSH_SERVER_URI="ssh://admin@localhost:29418" \
sbt "gatling:test"
```

## Configurable properties

### http.password [GIT_HTTP_PASSWORD]
password to be used when performing git operations over HTTP

Default: `default_password`

### http.username [GIT_HTTP_USERNAME]
user to be used when performing git operations over HTTP

Default: `default_username`

### http.server_uri [GIT_HTTP_SERVER_URI]
URI for git operations over HTTP (fetch, push, clone)

Default: `http://localhost:8080`

### tmpFiles.basePath [TMP_BASE_PATH]
base path where to persist work on disk (i.e. clones)

Default: `/tmp`

### ssh.server_uri [GIT_SSH_SERVER_URI]
URI for git operations over SSH (fetch, push, clone)

Default: `ssh://admin@localhost:29418`

### ssh.private_ssh_key_path [GIT_SSH_PRIVATE_KEY_PATH]
Path to the ssh private key to be used for git operations over SSH

Default: `/tmp/ssh-keys/id_rsa`