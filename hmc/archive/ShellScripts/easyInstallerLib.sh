#!/bin/sh

#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#################
### CONSTANTS ###
#################

# Meant to be used in the future to deal with upgrades and downgrades.
# XXX Remember to update this with every release of EasyInstaller. 
HDP_VERSION_STRING="HDP-1.0.4-Preview-6";

# We assume these are created by the RPM.
EASYINSTALLER_DB_DIR="/var/db/hdp/easyInstaller";
EASYINSTALLER_CONF_TEMPLATES_DIR="/etc/hdp/easyInstaller/templates";
EASYINSTALLER_SCRIPTS_DIR="/usr/libexec/hdp/easyInstaller";
GSINSTALLER_SCRIPTS_DIR="/usr/libexec/hdp/gsInstaller";

GSINSTALLER_PROPERTIES_CONF_NAME="gsInstaller.properties";
GSCLUSTER_PROPERTIES_CONF_NAME="gsCluster.properties";

INSTALLED_HDP_VERSION_COOKIE_NAME="installedHDPVersion";
CHECKPOINTED_STAGE_NUMBER_COOKIE_NAME="checkpointedStageNumber";

CLUSTER_DEPLOY_USER_IDENTITY_FILE_NAME="clusterDeployUserIdentity";
CLUSTER_HOSTS_FILE_NAME="clusterHosts";
NAMENODE_MOUNT_POINTS_FILE_NAME="NameNodeMountPointsSuggest.out";

# XXX TODO Remove this temporary hack once we figure out why Jenkins is not 
#          pushing the advertised keys into the environment.
JENKINS_URL="http://localhost:9040/"

#################
### FUNCTIONS ###
#################

function true2yes
{
  # Anything other than exactly "true" results in "no".
  local answer="no";

  if [ "xtrue" == "x${1}" ]
  then
    answer="yes";
  fi

  echo "${answer}";
}

function fetchJobConfigTemplate
{
  local jobName="${1}";
  local jobConfigTemplateDestinationFile="${2}";

  if [ "x" != "x${jobName}" ] && [ "x" != "x${jobConfigTemplateDestinationFile}" ]
  then
    cp -pf "${EASYINSTALLER_CONF_TEMPLATES_DIR}/jobs/${jobName}/config.xml" "${jobConfigTemplateDestinationFile}";
  else
    return 1;
  fi
}

function getFirstWordFromFile
{
  local firstWord="";
  local absFilePath="${1}";

  if [ "x" != "x${absFilePath}" ] && [ -e "${absFilePath}" ]
  then
    firstWord=`head -1 ${absFilePath} | awk '{print $1;}'`;
  fi

  echo "${firstWord}";
}

function wrapWithXMLTag
{
  local wrappedOutput="";

  local xmlTag="${1}";
  local wrappedEntity="${2}";

  if [ "x" != "x${xmlTag}" ] && [ "x" != "x${wrappedEntity}" ]
  then
    wrappedOutput="<${xmlTag}>${wrappedEntity}</${xmlTag}>";
  fi

  echo "${wrappedOutput}";
}
