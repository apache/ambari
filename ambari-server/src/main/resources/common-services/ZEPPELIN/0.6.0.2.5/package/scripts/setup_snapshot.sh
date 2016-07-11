#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

set -e
#e.g. /opt/incubator-zeppelin
export INSTALL_DIR=$1

#if true, will setup Ambari view and import notebooks
export SETUP_VIEW=$2


SETUP_VIEW=`awk '{ print tolower($0) }' <<< "$SETUP_VIEW"`
echo "SETUP_VIEW is $SETUP_VIEW"

SetupZeppelin () {

  echo "Setting up zeppelin at $INSTALL_DIR"
  cd $INSTALL_DIR

  if [[ $SETUP_VIEW == "true" ]]
  then
    echo "Importing notebooks"
    mkdir -p notebook
    cd notebook
    wget https://codeload.github.com/hortonworks-gallery/zeppelin-notebooks/zip/master -O notebooks.zip
    unzip -o notebooks.zip

    if [ -d "zeppelin-notebooks-master" ]; then
      yes | cp -rf zeppelin-notebooks-master/* .
      rm -rf zeppelin-notebooks-master
    fi

    cd ..
  else
    echo "Skipping import of sample notebooks"
  fi

}

SetupZeppelin
echo "Setup complete"
