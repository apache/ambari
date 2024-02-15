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
   Usage: /usr/lib/ambari-infra-solr-client/ambariSolrMigration.sh --mode <MODE> --ini-file <ini_file> [additional options]

   -m, --mode  <MODE>                     available migration modes: delete-only | backup-only | migrate-restore | all | transport
   -i, --ini-file <INI_FILE>              ini-file location (used by migrationHelper.py)
   -s, --migration-script-location <file> migrateHelper.py location (default: /usr/lib/ambari-infra-solr-client/migrationHelper.py)
   -w, --wait-between-steps <seconds>     wait between different migration steps in seconds (default: 15)
   -p, --python-path                      python location, default: /usr/bin/python
   -b, --batch-interval                   seconds between batch tasks for rolling restart solr at last step (default: 60)
   -k, --keep-backup                      keep backup data (more secure, useful if you have enough space for that)
   --skip-solr-client-upgrade             skip ambari-infra-solr-client package upgrades
   --skip-solr-server-upgrade             skip ambari-infra-solr package upgrades
   --skip-logsearch-upgrade               skip ambari-logsearch-portal and ambari-logsearch-logfeeder package upgrades
   --skip-warnings                        skip warnings at check-shards step
   -h, --help                             print help
EOF
}

function handle_result() {
  local result_code=${1:?"usage: <last_command_result>"}
  local step=${2:?"usage: <step_context"}
  local python_location=${3:?"usage: <python_location>"}
  local start_date=${4:?"usage: <date>"}
  if [[ "$result_code" != "0" ]] ; then
    end_date=$(date +%s)
    runtime=$($python_location -c "print '%02u:%02u:%02u' % ((${end_date} - ${start_date})/3600, ((${end_date} - ${start_date})/60)%60, (${end_date} - ${start_date})%60)")
    echo "Total Runtime: $runtime"
    echo "$step command FAILED. Stop migration commands ..."
    exit 1
  fi
}

function wait() {
  local seconds=${1:?"usage: <seconds>"}
  echo "Waiting $seconds seconds before next step ..."
  sleep $seconds
}

function log_command() {
  local command_to_execute=${1:?"usage: <seconds>"}
  echo "Execute command: $command_to_execute"
}

function run_migrate_commands() {
  local mode=${1:?"usage: <mode>"}
  local script_location=${2:?"usage: <script_location>"}
  local python_location=${3:?"usage: <python_location>"}
  local ini_file=${4:?"usage <ini_file>"}
  local time_sleep=${5:?"usage <time_sleep_seconds>"}
  local skip_solr_client_upgrade=${6:?"usage <true|false>"}
  local skip_solr_server_upgrade=${7:?"usage <true|false>"}
  local skip_logsearch_upgrade=${8:?"usage <true|false>"}
  local skip_warnings=${9:?"usage <true|false>"}
  local batch_interval=${10:?"usage <seconds>"}
  local keep_backup=${11:?"usage <true|false>"}
  local verbose=${12:?"usage <true|false>"}

  local verbose_val=""
  if [[ "$verbose" == "true" ]]; then
    verbose_val="--verbose"
  fi

  local skip_warnings_val=""
  if [[ "$skip_warnings" == "true" ]]; then
    skip_warnings_val="--skip-warnings"
  fi

  local keep_backup_val=""
  if [[ "$keep_backup" == "true" ]]; then
    keep_backup_val="--keep-backup"
  fi

  start_date=$(date +%s)

  # execute on: transport
  if [[ "$mode" == "transport" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action transport-old-data $verbose_val"
    $python_location $script_location --ini-file $ini_file --action transport-old-data $verbose_val
    handle_result "$?" "Transport Old Solr Data" "$python_location" "$start_date"
  fi

  # execute on: backup - all
  if [[ "$mode" == "backup" || "$mode" == "all" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action check-shards $verbose_val $skip_warnings_val"
    $python_location $script_location --ini-file $ini_file --action check-shards $verbose_val $skip_warnings_val
    handle_result "$?" "Check Shards" "$python_location" "$start_date"
  fi

  # execute on: backup - delete - all
  if [[ "$mode" == "delete" || "$mode" == "backup" || "$mode" == "all" ]] ; then
    if [[ "$skip_solr_client_upgrade" != "true" ]]; then
      log_command "$python_location $script_location --ini-file $ini_file --action upgrade-solr-clients $verbose_val"
      $python_location $script_location --ini-file $ini_file --action upgrade-solr-clients $verbose_val
      handle_result "$?" "Upgrade Solr Clients" "$python_location" "$start_date"
    fi
    log_command "$python_location $script_location --ini-file $ini_file --action check-docs $verbose_val"
    $python_location $script_location --ini-file $ini_file --action check-docs $verbose_val
    handle_result "$?" "Check Documents" "$python_location" "$start_date"
  fi

  # ececute on: backup - all
  if [[ "$mode" == "backup" || "$mode" == "all" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action backup $verbose_val"
    $python_location $script_location --ini-file $ini_file --action backup $verbose_val
    handle_result "$?" "Backup" "$python_location" "$start_date"
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action delete-collections $verbose_val"
    $python_location $script_location --ini-file $ini_file --action delete-collections $verbose_val
    handle_result "$?" "Delete collections" "$python_location" "$start_date"
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    if [[ "$skip_solr_server_upgrade" != "true" ]]; then
      log_command "$python_location $script_location --ini-file $ini_file --action upgrade-solr-instances $verbose_val"
      $python_location $script_location --ini-file $ini_file --action upgrade-solr-instances $verbose_val
      handle_result "$?" "Upgrade Solr Instances" "$python_location" "$start_date"
    fi
  fi

  # execute on: delete - all
  if [[ "$mode" == "delete" || "$mode" == "all" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action restart-solr $verbose_val"
    $python_location $script_location --ini-file $ini_file --action restart-solr $verbose_val
    handle_result "$?" "Restart Solr Instances" "$python_location" "$start_date"
    wait $time_sleep

    log_command "$python_location $script_location --ini-file $ini_file --action restart-ranger $verbose_val"
    $python_location $script_location --ini-file $ini_file --action restart-ranger $verbose_val
    handle_result "$?" "Restart Ranger Admins" "$python_location" "$start_date"
    wait $time_sleep
    if [[ "$skip_logsearch_upgrade" != "true" ]]; then
      log_command "$python_location $script_location --ini-file $ini_file --action upgrade-logsearch-portal $verbose_val"
      $python_location $script_location --ini-file $ini_file --action upgrade-logsearch-portal $verbose_val
      handle_result "$?" "Upgrade Log Search Portal" "$python_location" "$start_date"

      log_command "$python_location $script_location --ini-file $ini_file --action upgrade-logfeeders $verbose_val"
      $python_location $script_location --ini-file $ini_file --action upgrade-logfeeders $verbose_val
      handle_result "$?" "Upgrade Log Feeders" "$python_location" "$start_date"
    fi
    log_command "$python_location $script_location --ini-file $ini_file --action restart-logsearch $verbose_val"
    $python_location $script_location --ini-file $ini_file --action restart-logsearch $verbose_val
    handle_result "$?" "Restart Log Search" "$python_location" "$start_date"
    wait $time_sleep

    log_command "$python_location $script_location --ini-file $ini_file --action restart-atlas $verbose_val"
    $python_location $script_location --ini-file $ini_file --action restart-atlas $verbose_val
    handle_result "$?" "Restart Atlas Servers" "$python_location" "$start_date"
    wait $time_sleep
  fi

  # execute on migrate-restore - all
  if [[ "$mode" == "migrate-restore" || "$mode" == "all" ]] ; then
    log_command "$python_location $script_location --ini-file $ini_file --action check-docs $verbose_val"
    $python_location $script_location --ini-file $ini_file --action check-docs $verbose_val
    handle_result "$?" "Check Documents" "$python_location" "$start_date"

    log_command "$python_location $script_location --ini-file $ini_file --action migrate $verbose_val"
    $python_location $script_location --ini-file $ini_file --action migrate $verbose_val
    handle_result "$?" "Migrate Index" "$python_location" "$start_date"

    log_command "$python_location $script_location --ini-file $ini_file --action restore $keep_backup_val $verbose_val"
    $python_location $script_location --ini-file $ini_file --action restore $keep_backup_val $verbose_val
    handle_result "$?" "Restore" "$python_location" "$start_date"

    log_command "$python_location $script_location --ini-file $ini_file --action rolling-restart-solr $verbose_val --batch-interval $batch_interval"
    $python_location $script_location --ini-file $ini_file --action rolling-restart-solr $verbose_val --batch-interval $batch_interval
    handle_result "$?" "Rolling Restart Solr" "$python_location" "$start_date"
  fi

  end_date=$(date +%s)
  runtime=$($python_location -c "print '%02u:%02u:%02u' % ((${end_date} - ${start_date})/3600, ((${end_date} - ${start_date})/60)%60, (${end_date} - ${start_date})%60)")
  echo "Total Runtime: $runtime"
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
        -p|--python-path)
          local PYTHON_PATH_FOR_MIGRATION="$2"
          shift 2
        ;;
        -b|--batch-interval)
          local BATCH_INTERVAL="$2"
          shift 2
        ;;
        -k|--keep-backup)
          local KEEP_BACKUP="true"
          shift 1
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
        --skip-warnings)
          local SKIP_WARNINGS="true"
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

  if [[ -z "$PYTHON_PATH_FOR_MIGRATION" ]] ; then
    PYTHON_PATH_FOR_MIGRATION="/usr/bin/python"
  fi

  if [[ -z "$WAIT" ]] ; then
    WAIT="15"
  fi

  if [[ -z "$BATCH_INTERVAL" ]] ; then
    BATCH_INTERVAL="60"
  fi

  if [[ -z "$VERBOSE" ]] ; then
    VERBOSE="false"
  fi

  if [[ -z "$SKIP_WARNINGS" ]] ; then
    SKIP_WARNINGS="false"
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

  if [[ -z "$KEEP_BACKUP" ]] ; then
    KEEP_BACKUP="false"
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
    if [[ "$MODE" == "delete" || "$MODE" == "backup" || "$MODE" == "migrate-restore" || "$MODE" == "all" || "$MODE" == "transport" ]]; then
      run_migrate_commands "$MODE" "$SCRIPT_LOCATION" "$PYTHON_PATH_FOR_MIGRATION" "$INI_FILE" "$WAIT" "$SKIP_SOLR_CLIENT_UPGRADE" "$SKIP_SOLR_SERVER_UPGRADE" "$SKIP_LOGSEARCH_UPGRADE" "$SKIP_WARNINGS" "$BATCH_INTERVAL" "$KEEP_BACKUP" "$VERBOSE"
    else
      echo "mode '$MODE' is not supported"
      print_help
      exit 1
    fi
  fi
}

main ${1+"$@"}