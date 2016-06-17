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

#e.g. sandbox.hortonworks.com
export HIVE_METASTORE_HOST=$2

#e.g. 9083
export HIVE_METASTORE_PORT=$3

#e.g. 10000
export HIVE_SERVER_PORT=$4

export ZEPPELIN_HOST=$5

export ZEPPELIN_PORT=$6

#if true, will setup Ambari view and import notebooks
export SETUP_VIEW=$7

export PACKAGE_DIR=$8
export java64_home=$9

SETUP_VIEW=${SETUP_VIEW,,}
echo "SETUP_VIEW is $SETUP_VIEW"

SetupZeppelin () {

  echo "Setting up zeppelin at $INSTALL_DIR"
  cd $INSTALL_DIR

  rm -rf notebook/*

  if [ "$HIVE_METASTORE_HOST" != "0.0.0.0" ]
  then
    echo "Hive metastore detected: $HIVE_METASTORE_HOST. Setting up conf/hive-site.xml"
    echo "<configuration>" > conf/hive-site.xml
    echo "<property>" >> conf/hive-site.xml
    echo "   <name>hive.metastore.uris</name>" >> conf/hive-site.xml
    echo "   <value>thrift://$HIVE_METASTORE_HOST:$HIVE_METASTORE_PORT</value>" >> conf/hive-site.xml
    echo "</property>" >> conf/hive-site.xml
    echo "<property>" >> conf/hive-site.xml
    echo "   <name>hive.server2.thrift.http.port</name>" >> conf/hive-site.xml
    echo "   <value>$HIVE_SERVER_PORT</value>" >> conf/hive-site.xml
    echo "</property>" >> conf/hive-site.xml
    echo "</configuration>" >> conf/hive-site.xml
  else
    echo "HIVE_METASTORE_HOST is $HIVE_METASTORE_HOST: Skipping hive-site.xml setup as Hive does not seem to be installed"
  fi

  if [[ $SETUP_VIEW == "true" ]]
  then
    echo "Importing notebooks"
    mkdir -p notebook
    cd notebook
    wget https://codeload.github.com/hortonworks-gallery/zeppelin-notebooks/zip/master -O notebooks.zip
    unzip notebooks.zip

    if [ -d "zeppelin-notebooks-master" ]; then
      mv zeppelin-notebooks-master/* .
      rm -rf zeppelin-notebooks-master
    fi

    cd ..
  else
    echo "Skipping import of sample notebooks"
  fi

}

SetupZeppelin
echo "Setup complete"
