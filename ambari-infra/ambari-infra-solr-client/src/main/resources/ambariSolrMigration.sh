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

function print_help() {
  cat << EOF
   Usage: --mode <MODE> --ini-file <ini_file>

   -m, --mode  <MODE>                     available migration modes: delete-only | backup-only | migrate-restore | all
   -i, --ini-file <INI_FILE>              ini-file location (used by migrationHelper.py)
   -s, --migration-script-location <file> migrateHelper.py location (default: /usr/lib/ambari-infra-solr-client/migrationHelper.py)
   -w, --wait-between-steps <seconds>     wait between different migration steps in seconds (default: 30)
   --skip-solr-client-upgrade             skip ambari-infra-solr-client package upgrades
   --skip-solr-server-upgrade             skip ambari-infra-solr package upgrades
   --skip-logsearch-upgrade               skip ambari-logsearch-portal and ambari-logsearch-logfeeder package upgrades
   -h, --help                             print help
EOF
}

function handle_result() {
  local result_code=${1:?"usage: <last_command_result>"}
  local step=${2:?"usage: <step_context"}
  if [[ "$result_code" != "0" ]] ; then
    echo "$step command failed. Stop migration commands ..."
    exit 1
  fi
}

function run_migrate_commands() {
  local mode=${1:?"usage: <mode>"}
  local script_location=${2:?"usage: <script_location>"}
  local ini_file=${3:?"usage <ini_file>"}
  local time_sleep=${4:?"usage <time_sleep_seconds>"}
  local skip_solr_client_upgrade=${5:?"usage <true|false>"}
  local skip_solr_server_upgrade=${6:?"usage <true|false>"}
  local skip_logsearch_upgrade=${7:?"usage <true|false>"}
  local verbose=${8:?"usage <true|false>"}

  local verbose_val=""
  if [[ "$verbose" == "true" ]]; then
    verbose_val="--verbose"
  fi

  # execute on: backup - all
  if [[ "$mode" == "backup" || "$mode" == "all" ]] ; then
    $script_location --ini-file $ini_file --action check-shards $verbose_val
    handle_result "$?" "Check Shards"
  fi

  # execute on: backup - delete - all
  if [[ "$mode" != "migrate-restore" ]] ; then
    if [[ "$skip_solr_client_upgrade" != "true" ]]; then
      $script_location --ini-file $ini_file --action upgrade-solr-clients $verbose_val
      handle_result "$?" "Upgrade Solr Clients"
    fi
    $script_location --ini-file $ini_file --action check-docs $verbose_val
    handle_result "$?" "Check Documents"
  fi

  # ececute on: backup - all
  if [[ "$mode" == "backup" || "$mode" == "all" ]] ; then
    $script_location --ini-file $ini_file --action backup $verbose_val
    handle_result "$?" "Backup"
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    $script_location --ini-file $ini_file --action delete-collections $verbose_val
    handle_result "$?" "Delete collections"
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    if [[ "$skip_solr_server_upgrade" != "true" ]]; then
      $script_location --ini-file $ini_file --action upgrade-solr-instances $verbose_val
      handle_result "$?" "Upgrade Solr Instances"
    fi
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    $script_location --ini-file $ini_file --action restart-solr $verbose_val
    handle_result "$?" "Restart Solr Instances"
    $script_location --ini-file $ini_file --action restart-ranger $verbose_val
    handle_result "$?" "Restart Ranger Admins"
    $script_location --ini-file $ini_file --action restart-atlas $verbose_val
    handle_result "$?" "Restart Atlas Servers"
    if [[ "$skip_logsearch_upgrade" != "true" ]]; then
      $script_location --ini-file $ini_file --action upgrade-logsearch-portal $verbose_val
      handle_result "$?" "Upgrade Log Search Portal"
      $script_location --ini-file $ini_file --action upgrade-logfeeders $verbose_val
      handle_result "$?" "Upgrade Log Feeders"
    fi
    $script_location --ini-file $ini_file --action restart-logsearch $verbose_val
    handle_result "$?" "Restart Log Search"
  fi

  # execute on migrate-restore - all
  if [[ "$mode" == "migrate-restore" || "$mode" == "all" ]] ; then
    $script_location --ini-file $ini_file --action check-docs $verbose_val
    handle_result "$?" "Check Documents"
    $script_location --ini-file $ini_file --action migrate $verbose_val
    handle_result "$?" "Migrate Index"
    $script_location --ini-file $ini_file --action restore $verbose_val
    handle_result "$?" "Restore"
    $script_location --ini-file $ini_file --action rolling-restart-solr $verbose_val
    handle_result "$?" "Rolling Restart Solr"
  fi
}

function main() {
  while [[ $# -gt 0 ]]
    do
      key="$1"
      case $key in
        -m|--mode)
          local MODE="$2"
          shift 2
        ;;
        -i|--ini-file)
          local INI_FILE="$2"
          shift 2
        ;;
        -w|--wait-between-steps)
          local WAIT="$2"
          shift 2
        ;;
        -s|--migration-script-location)
          local SCRIPT_LOCATION="$2"
          shift 2
        ;;
        --skip-solr-client-upgrade)
          local SKIP_SOLR_CLIENT_UPGRADE="true"
          shift 1
        ;;
        --skip-solr-server-upgrade)
          local SKIP_SOLR_SERVER_UPGRADE="true"
          shift 1
        ;;
        --skip-logsearch-upgrade)
          local SKIP_LOGSEARCH_UPGRADE="true"
          shift 1
        ;;
        -v|--verbose)
          local VERBOSE="true"
          shift 1
        ;;
        -h|--help)
          shift 1
          print_help
          exit 0
        ;;
        *)
          echo "Unknown option: $1"
          exit 1
        ;;
      esac
  done

  if [[ -z "$SCRIPT_LOCATION" ]] ; then
    SCRIPT_LOCATION="/usr/lib/ambari-infra-solr-client/migrationHelper.py"
  fi

  if [[ -z "$WAIT" ]] ; then
    WAIT="30"
  fi

  if [[ -z "$VERBOSE" ]] ; then
    VERBOSE="false"
  fi

  if [[ -z "$SKIP_SOLR_CLIENT_UPGRADE" ]] ; then
    SKIP_SOLR_CLIENT_UPGRADE="false"
  fi

  if [[ -z "$SKIP_SOLR_SERVER_UPGRADE" ]] ; then
    SKIP_SOLR_SERVER_UPGRADE="false"
  fi

  if [[ -z "$SKIP_LOGSEARCH_UPGRADE" ]] ; then
    SKIP_LOGSEARCH_UPGRADE="false"
  fi

  if [[ -z "$INI_FILE" ]] ; then
    echo "ini-file argument is required (-i or --ini-file)."
    print_help
    exit 1
  fi

  if [[ -z "$MODE" ]] ; then
    echo "mode argument is required (-m or --mode)."
    print_help
    exit 1
  else
    if [[ "$MODE" == "delete" || "$MODE" == "backup" || "$MODE" == "migrate-restore" || "$MODE" == "all" ]]; then
      run_migrate_commands "$MODE" "$SCRIPT_LOCATION" "$INI_FILE" "$WAIT" "$SKIP_SOLR_CLIENT_UPGRADE" "$SKIP_SOLR_SERVER_UPGRADE" "$SKIP_LOGSEARCH_UPGRADE" "$VERBOSE"
    else
      echo "mode '$MODE' is not supported"
      print_help
      exit 1
    fi
  fi
}

main ${1+"$@"}