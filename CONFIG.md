# Configuration

Configuration is provided by the [application.conf](gatling-extension/src/test/resources/application.conf) file.
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
Password to be used when performing git operations over HTTP.

Default: `default_password`

### http.username [GIT_HTTP_USERNAME]
User to be used when performing git operations over HTTP.

Default: `default_username`

### tmpFiles.basePath [TMP_BASE_PATH]
Test data (i.e. clones) used by the running scenario is stored in `TMP_BASE_PATH/TEST_DATA_DIRECTORY`.
`TMP_BASE_PATH` defines the base path of the location on filesystem.

Default: `/tmp`

### tmpFiles.testDataDirectory [TEST_DATA_DIRECTORY]
Test data (i.e. clones) used by the running scenario is stored in `TMP_BASE_PATH/TEST_DATA_DIRECTORY`.
`TEST_DATA_DIRECTORY` defines the directory of the location on filesystem.

Default: `System.currentTimeMillis`

### ssh.private_key_path [GIT_SSH_PRIVATE_KEY_PATH]
Path to the ssh private key to be used for git operations over SSH.

Default: `/tmp/ssh-keys/id_rsa`

### commands.push

This configuration section allows to define how the code will synthesize the commits in the pushes.
Commits will be generated with `NUM_FILES` in it, each with dimension randomly included between `MIN_CONTENT_LENGTH` and `MAX_CONTENT_LENGTH`.

### commands.push.numFiles [NUM_FILES]
Number of files included in each push.

Default: `4`

### commands.push.minContentLength [MIN_CONTENT_LENGTH]
Minimum content length in bytes of each file contained in the push.

Default: `100`

### commands.push.maxContentLength [MAX_CONTENT_LENGTH]
Maximum content length in bytes of each file contained in the push.

Default: `10000`

### commands.push.commitPrefix [COMMIT_PREFIX]
Prefix added to the synthetic commit messages.

Default: `empty string`

### git.commandTimeout [COMMAND_TIMEOUT]
The timeout (in seconds) used to limit git commands duration.

Default: `30`

### git.showProgress [SHOW_PROGRESS]
Whether to report progress on standard output during git operations.

Default: `true`

## Git client configuration

JGit will honour the global Git client configurations of the user running Gatling.

See the official [Git documentation](https://git-scm.com/docs/git-config) for details
about it.

The config is stored in `$HOME/.gitconfig`. When running it in Docker, `$HOME` is
set to `/home/gatling`, hence the custom configuration will need to be injected in
the image when building it if needed.
