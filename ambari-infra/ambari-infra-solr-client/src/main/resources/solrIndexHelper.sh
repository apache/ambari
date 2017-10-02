#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

: ${JAVA_HOME:?"Please set the JAVA_HOME variable!"}

JVM="java"
sdir="`dirname \"$0\"`"
ldir="`dirname "$(readlink -f "$0")"`"

DIR="$sdir"
if [ "$sdir" != "$ldir" ]; then
  DIR="$ldir"
fi

function print_help() {
  cat << EOF

   Usage: [<command>] [<arguments with flags>]

   commands:
     upgrade-index            Check and upgrade solr index data in core directories.
     run-check-index-tool     call 'java -cp ... org.apache.lucene.index.IndexUpgrader' directly
     run-upgrade-index-tool   call 'java -cp ... org.apache.lucene.index.CheckIndex' directly
     help                     print usage


   upgrade-index command arguments:
     -d, --index-data-dir <DIRECTORY>        Location of the solr cores (e.g.: /opt/ambari_infra_solr/data)
     -c, --core-filter <FILTER1,FILTER2>     Comma separated name filters of core directoies (default: hadoop_logs,audit_logs,history)
     -f, --force                             Force to start index upgrade, even is the version is at least 6.

EOF
}

function upgrade_core() {
  local INDEX_DIR=${1:?"usage: <index_base_dir> e.g.: /opt/ambari_infra_solr/data"}
  local FORCE_UPDATE=${2:?"usage <force_update_flag> e.g.: true"}
  local SOLR_CORE_FILTERS=${3:?"usage: <comma separated core filters> e.g.: hadoop_logs,audit_logs,history"}

  SOLR_CORE_FILTER_ARR=$(echo $SOLR_CORE_FILTERS | sed "s/,/ /g")

  for coll in $SOLR_CORE_FILTER_ARR; do
    if [[ "$1" == *"$coll"* ]]; then
      echo "Core '$1' dir name contains $coll (core filter)'";
      version=$(PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/libs/lucene-core-6.6.0.jar:$DIR/libs/lucene-backward-codecs-6.6.0.jar" org.apache.lucene.index.CheckIndex -fast $1|grep "   version="|sed -e 's/.*=//g'|head -1)
      if [ -z $version ] ; then
        echo "Core '$1' - Empty index?"
        return
      fi
      majorVersion=$(echo $version|cut -c 1)
      if [ $majorVersion -ge 6 ] && [ $FORCE_UPDATE == "false" ] ; then
        echo "Core '$1' - Already on version $version, not upgrading. Use -f or --force option to run upgrade anyway."
      else
        echo "Core '$1' - Index version is $version, upgrading ..."
        PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/libs/lucene-core-6.6.0.jar:$DIR/libs/lucene-backward-codecs-6.6.0.jar" org.apache.lucene.index.IndexUpgrader -delete-prior-commits $1
        echo "Upgrading core '$1' has finished"
      fi
    fi
  done
}

function upgrade_index() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -c|--core-filters)
          local SOLR_CORE_FILTERS="$2"
          shift 2
        ;;
        -f|--force)
          local FORCE_UPDATE="true"
          shift
        ;;
        -d|--index-data-dir)
          local INDEX_DIR="$2"
          shift 2
        ;;
        *)
          echo "Unknown option: $1"
          exit 1
        ;;
      esac
  done
  if [[ -z "$INDEX_DIR" ]] ; then
    echo "Index data dirctory option is required (-d or --index-data-dir). Exiting..."
    exit 1
  fi

  if [[ -z "$SOLR_CORE_FILTERS" ]] ; then
    SOLR_CORE_FILTERS="hadoop_logs,audit_logs,history"
  fi

  if [[ -z "$FORCE_UPDATE" ]] ; then
    FORCE_UPDATE="false"
  else
    echo "NOTE: Forcing index upgrade is set."
  fi

  CORES=$(for replica_dir in `find $INDEX_DIR -name data`; do dirname $replica_dir; done);
  if [[ -z "$CORES" ]] ; then
    echo "No indices found on path $INDEX_DIR"
  else
      for c in $CORES ; do
        if find $c/data -maxdepth 1 -type d -name 'index*' 1> /dev/null 2>&1; then
          name=$(echo $c | sed -e 's/.*\///g')
          abspath=$(cd "$(dirname "$c")"; pwd)/$(basename "$c")
          find $c/data -maxdepth 1 -type d -name 'index*' | while read indexDir; do
          echo "Checking core $name - $abspath"
          upgrade_core "$indexDir" "$FORCE_UPDATE" "$SOLR_CORE_FILTERS"
          done
        else
          echo "No index folder found for $name"
        fi
      done
      echo "DONE"
  fi
}

function upgrade_index_tool() {
  # see: https://cwiki.apache.org/confluence/display/solr/IndexUpgrader+Tool
  PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/libs/lucene-core-6.6.0.jar:$DIR/libs/lucene-backward-codecs-6.6.0.jar" org.apache.lucene.index.IndexUpgrader ${@}
}

function check_index_tool() {
  PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/libs/lucene-core-6.6.0.jar:$DIR/libs/lucene-backward-codecs-6.6.0.jar" org.apache.lucene.index.CheckIndex ${@}
}

function main() {
  command="$1"
  case $command in
   "upgrade-index")
     upgrade_index "${@:2}"
     ;;
   "run-check-index-tool")
     check_index_tool "${@:2}"
     ;;
   "run-upgrade-index-tool")
     upgrade_index_tool "${@:2}"
     ;;
   "help")
     print_help
     ;;
   *)
   echo "Available commands: (upgrade-index | run-check-index-tool | run-upgrade-index-tool | help)"
   ;;
   esac
}

main ${1+"$@"}
