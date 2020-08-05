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
     -b, --backup-enabled                    Use indexer tool with backup snapshots. (core filter won't be used)
     -g, --debug                             Enable debug mode, IndexUpgrader output will be verbose.
     -f, --force                             Force to start index upgrade, even is the version is at least 6.
     -v, --version                           Lucene version to upgrade (default: 6.6.2, available: 6.6.2, 7.7.3)
EOF
}

function upgrade_core() {
  local INDEX_DIR=${1:?"usage: <index_base_dir> e.g.: /opt/ambari_infra_solr/data"}
  local FORCE_UPDATE=${2:?"usage <force_update_flag> e.g.: true"}
  local SOLR_CORE_FILTERS=${3:?"usage: <comma separated core filters> e.g.: hadoop_logs,audit_logs,history"}
  local LUCENE_VERSION=${4:?"usage <lucene_index_version> e.g.: 7.7.3"}
  local BACKUP_MODE=${5:?"usage <backup_mode_enabled> e.g.: true"}
  local DEBUG_MODE=${6:?"usage <debug_mode> e.g.: true"}
  SOLR_CORE_FILTER_ARR=$(echo $SOLR_CORE_FILTERS | sed "s/,/ /g")

  local version_prefix="$(echo $LUCENE_VERSION | head -c 1)"
  local write_lock_exists="false"
  local core_str="Core"
  if [[ "$BACKUP_MODE" == "true" ]]; then
    core_str="Snapshot"
  fi

  local verbose=""
  if [[ "$DEBUG_MODE" == "true" ]]; then
    verbose="-verbose"
  fi

  if [[ -f "$INDEX_DIR/write.lock" ]]; then
    echo "Deleting $INDEX_DIR/write.lock file..."
    write_lock_exists="true"
    rm "$INDEX_DIR/write.lock"
  fi

  for coll in $SOLR_CORE_FILTER_ARR; do
    if [[ "$1" == *"$coll"* ]]; then
      echo "$core_str '$1' dir name contains $coll (core filter)'";
      version=$(PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/migrate/lucene-core-$LUCENE_VERSION.jar:$DIR/migrate/lucene-backward-codecs-$LUCENE_VERSION.jar" org.apache.lucene.index.CheckIndex -fast $1|grep "   version="|sed -e 's/.*=//g'|head -1)
      if [ -z $version ] ; then
        echo "$core_str '$1' - Empty index?"
        return
      fi
      majorVersion=$(echo $version|cut -c 1)
      if [ $majorVersion -ge $version_prefix ] && [ $FORCE_UPDATE == "false" ] ; then
        echo "$core_str '$1' - Already on version $version, not upgrading. Use -f or --force option to run upgrade anyway."
      else
        echo "$core_str '$1' - Index version is $version, upgrading ..."
        echo "Run: PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/migrate/lucene-core-$LUCENE_VERSION.jar:$DIR/migrate/lucene-backward-codecs-$LUCENE_VERSION.jar" org.apache.lucene.index.IndexUpgrader -delete-prior-commits $verbose $1"
        PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/migrate/lucene-core-$LUCENE_VERSION.jar:$DIR/migrate/lucene-backward-codecs-$LUCENE_VERSION.jar" org.apache.lucene.index.IndexUpgrader -delete-prior-commits $verbose $1
        echo "Upgrading core '$1' has finished"
      fi
    fi
  done

  if [[ "$write_lock_exists" == "true" ]]; then
    echo "Putting write.lock file back..."
    touch "$INDEX_DIR/write.lock"
  fi
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
        -b|--backup-enabled)
          local BACKUP_ENABLED="true"
          shift
        ;;
        -g|--debug)
          local DEBUG_ENABLED="true"
          shift
        ;;
        -d|--index-data-dir)
          local INDEX_DIR="$2"
          shift 2
        ;;
        -v|--version)
          local LUCENE_VERSION="$2"
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

  if [[ -z "$BACKUP_ENABLED" ]] ; then
    BACKUP_ENABLED="false"
  else
    if [[ -z "$SOLR_CORE_FILTERS" ]]; then
      SOLR_CORE_FILTERS="snapshot"
      echo "NOTE: Use 'snapshot' as filter."
    fi
  fi

  if [[ -z "$SOLR_CORE_FILTERS" ]] ; then
    SOLR_CORE_FILTERS="hadoop_logs,audit_logs,history"
    echo "NOTE: Use 'hadoop_logs,audit_logs,history' as filte."
  fi

  if [[ -z "$LUCENE_VERSION" ]] ; then
    LUCENE_VERSION="6.6.2"
  fi

  if [[ -z "$FORCE_UPDATE" ]] ; then
    FORCE_UPDATE="false"
  else
    echo "NOTE: Forcing index upgrade is set."
  fi

  if [[ -z "$DEBUG_ENABLED" ]] ; then
    DEBUG_ENABLED="false"
  else
    echo "NOTE: Debug mode is enabled."
  fi

  if [[ "$BACKUP_ENABLED" == "true" ]]; then
    for SNAPSHOT_DIR in $(find $INDEX_DIR -maxdepth 1 -mindepth 1); do
      if $(test -d ${SNAPSHOT_DIR}); then
        abspath=$(cd "$(dirname "$SNAPSHOT_DIR")"; pwd)/$(basename "$SNAPSHOT_DIR")
        echo "--------------------------------"
        echo "Checking snapshot: $abspath"
        upgrade_core "$abspath" "$FORCE_UPDATE" "$SOLR_CORE_FILTERS" "$LUCENE_VERSION" "$BACKUP_ENABLED" "$DEBUG_ENABLED"
      fi;
    done
  else
    CORES=$(for replica_dir in `find $INDEX_DIR -name data`; do dirname $replica_dir; done);
    if [[ -z "$CORES" ]] ; then
      echo "No indices found on path $INDEX_DIR"
    else
        for c in $CORES ; do
          if find $c/data -maxdepth 1 -type d -name 'index*' 1> /dev/null 2>&1; then
            name=$(echo $c | sed -e 's/.*\///g')
            abspath=$(cd "$(dirname "$c")"; pwd)/$(basename "$c")
            find $c/data -maxdepth 1 -type d -name 'index*' | while read indexDir; do
              echo "--------------------------------"
              echo "Checking core $name - $abspath"
              upgrade_core "$indexDir" "$FORCE_UPDATE" "$SOLR_CORE_FILTERS" "$LUCENE_VERSION" "$BACKUP_ENABLED" "$DEBUG_ENABLED"
            done
          else
            echo "No index folder found for $name"
          fi
        done
        echo "DONE"
    fi
  fi
}

function upgrade_index_tool() {
  # see: https://cwiki.apache.org/confluence/display/solr/IndexUpgrader+Tool
  : ${INDEX_VERSION:?"Please set the INDEX_VERSION variable! (6.6.2 or 7.7.3)"}
  PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/migrate/lucene-core-$INDEX_VERSION.jar:$DIR/migrate/lucene-backward-codecs-$INDEX_VERSION.jar" org.apache.lucene.index.IndexUpgrader ${@}
}

function check_index_tool() {
  : ${INDEX_VERSION:?"Please set the INDEX_VERSION variable! (6.6.2 or 7.7.3)"}
  PATH=$JAVA_HOME/bin:$PATH $JVM -classpath "$DIR/migrate/lucene-core-$INDEX_VERSION.jar:$DIR/migrate/lucene-backward-codecs-$INDEX_VERSION.jar" org.apache.lucene.index.CheckIndex ${@}
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
