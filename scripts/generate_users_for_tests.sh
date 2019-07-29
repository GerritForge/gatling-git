# Copyright (C) 2019 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/usr/bin/env

# Set up virtual users with fresh clones of the repositories to test.
# Usage example:
# ./scripts/generate_users_for_tests.sh barbasa review.gerrithub.io 5 ./test-data GerritForge/gatling-git GerritForge/gerrit-etl
#
# The script clones the testing repositories in TEST_DATA_DIR/repos
# and then copy them across the virtual users directories.

USERNAME=$1
GERRIT_URL=$2
NUMBER_OF_USERS=$3
VIRTUAL_USERS_DATA=$4
REPOSITORIES=${@:5}

echo "==== Running script with following parameters ===="
echo "-USERNAME: $USERNAME"
echo "-GERRIT_URL: $GERRIT_URL"
echo "-NUMBER_OF_USERS: $NUMBER_OF_USERS"
echo "-VIRTUAL_USERS_DATA: $VIRTUAL_USERS_DATA"
echo "-REPOSITORIES: $REPOSITORIES"
echo "=================================================="
echo

rm -fr $VIRTUAL_USERS_DATA
mkdir -p $VIRTUAL_USERS_DATA/repos

pushd $VIRTUAL_USERS_DATA/repos
for project in $REPOSITORIES ; do
  # See explanation of project_dir extraction here: https://stackoverflow.com/questions/3162385
  project_dir=${project##*/}
  echo "Cloning $project ->" git clone ssh://$USERNAME@$GERRIT_URL:29418/$project
  git clone ssh://$USERNAME@$GERRIT_URL:29418/$project
  scp -p -P 29418 $USERNAME@$GERRIT_URL:hooks/commit-msg $project_dir/.git/hooks/
done
popd

for (( user=1; user<=$NUMBER_OF_USERS; user++ ))
do
  mkdir $VIRTUAL_USERS_DATA/$user
  cp -R $VIRTUAL_USERS_DATA/repos/* $VIRTUAL_USERS_DATA/$user/
done
