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

baseDir=`dirname ${0}`;

source ${baseDir}/easyInstallerLib.sh;
source ${baseDir}/dbLib.sh;
source ${baseDir}/servicesLib.sh;

###########################
### FILE-SCOPED GLOBALS ###
###########################

# TODO XXX Take this in from the UI in the future (and likely store it as a 
# marker on disk, to really be of any use).
clusterName="MyHDPCluster";

#############################
### FILE-SCOPED FUNCTIONS ###
#############################

function getServiceHost
{
  local serviceHost="";

  local serviceName="${1}";

  if [ "x" != "x${serviceName}" ]
  then
    serviceHostFile=`fetchDbFileForCluster "${clusterName}" "${serviceName}"`;
    serviceHost=`getFirstWordFromFile "${serviceHostFile}"`;
  fi

  echo "${serviceHost}";
}

function generateServiceConfigChoiceXml
{
  local serviceConfigChoiceXml="";

  local serviceHostFileName="${1}";
  local serviceConfigKey="${2}";

  if [ "x" != "x${serviceHostFileName}" ] && [ "x" != "x${serviceConfigKey}" ]
  then
    local serviceHostValue=`getServiceHost "${serviceHostFileName}"`;

    serviceConfigChoiceXml="<hudson.model.ChoiceParameterDefinition>\
<name>${serviceConfigKey}</name>\
<description></description>\
<choices class=\\\"java.util.Arrays\\\$ArrayList\\\">\
<a class=\\\"string-array\\\">\
<string>${serviceHostValue}</string>\
</a>\
</choices>\
</hudson.model.ChoiceParameterDefinition>";
  fi

  echo "${serviceConfigChoiceXml}";
}

function generateOptionalServicesConfigChoicesXml
{
  local optionalServicesConfigChoicesXml="";

  local isHBaseInstalled="${1}"; 
  local isHCatalogInstalled="${2}";
  local isTempletonInstalled="${3}";
  local isOozieInstalled="${4}";

  if [ "xyes" == "x${isHBaseInstalled}" ]
  then
    hBaseConfigChoicesXml=`generateServiceConfigChoiceXml "hbasemaster" "HDPHBaseMasterHost"`;
    optionalServicesConfigChoicesXml="${optionalServicesConfigChoicesXml}${hBaseConfigChoicesXml}";
  fi

  if [ "xyes" == "x${isHCatalogInstalled}" ]
  then
    hCatalogConfigChoicesXml=`generateServiceConfigChoiceXml "hcatserver" "HDPHCatalogServerHost"`;
    optionalServicesConfigChoicesXml="${optionalServicesConfigChoicesXml}${hCatalogConfigChoicesXml}";
  fi

  if [ "xyes" == "x${isTempletonInstalled}" ]
  then
    templetonConfigChoicesXml=`generateServiceConfigChoiceXml "templetonnode" "HDPTempletonNodeHost"`;
    optionalServicesConfigChoicesXml="${optionalServicesConfigChoicesXml}${templetonConfigChoicesXml}";
  fi

  if [ "xyes" == "x${isOozieInstalled}" ]
  then
    oozieConfigChoicesXml=`generateServiceConfigChoiceXml "oozieserver" "HDPOozieServerHost"`;
    optionalServicesConfigChoicesXml="${optionalServicesConfigChoicesXml}${oozieConfigChoicesXml}";
  fi

  echo "${optionalServicesConfigChoicesXml}";
}

#############
### SETUP ###
#############

# XXX XXX RESUME FROM HERE
###checkpointedStageNumber=`fetchDbCookieForCluster "${clusterName}" "${CHECKPOINTED_STAGE_NUMBER_COOKIE_NAME}"`;
###
###currentStageName="1-HDP-Initialize-Cluster";
###currentStageNumber=`getStageNumberFromName "${currentStageName}"`;
###
###currentStageCanRun=`isStageTransitionAdmissable "${checkpointedStageNumber}" "${currentStageNumber}"`;
###
###if [ "${currentStageCanRun}" != "1" ]
###then
###  echo "Inadmissable stage transition attempted - bailing out.";
###  exit 1;
###fi

# Create our per-build workspace - it's a shame Jenkins doesn't provide this for us.
perBuildWorkspace=${WORKSPACE}/${BUILD_NUMBER};

cmd="mkdir ${perBuildWorkspace}";

echo "${cmd}";
eval "${cmd}";

######################
### PRE-PROCESSING ###
######################

# Do nothing.

######################
### THE MAIN EVENT ###
######################

# Do nothing.

#######################
### POST-PROCESSING ###
#######################

# Store the data we've gotten from the user into our DB.
read -r -d '' gsClusterConf << EOConf
s!@HDPHadoopHeapSize@!${HDPHadoopHeapSize}!g
s!@HDPNameNodeHeapSize@!${HDPNameNodeHeapSize}!g
s!@HDPFSInMemorySize@!${HDPFSInMemorySize}!g
s!@HDPNameNodeOptNewSize@!${HDPNameNodeOptNewSize}!g
s!@HDPDataNodeDuReserved@!${HDPDataNodeDuReserved}!g
s!@HDPDataNodeHeapSize@!${HDPDataNodeHeapSize}!g
s!@HDPJobTrackerOptNewSize@!${HDPJobTrackerOptNewSize}!g
s!@HDPJobTrackerOptMaxNewSize@!${HDPJobTrackerOptMaxNewSize}!g
s!@HDPJobTrackerHeapSize@!${HDPJobTrackerHeapSize}!g
s!@HDPMapRedMapTasksMax@!${HDPMapRedMapTasksMax}!g
s!@HDPMapRedReduceTasksMax@!${HDPMapRedReduceTasksMax}!g
s!@HDPMapRedClusterMapMemoryMB@!${HDPMapRedClusterMapMemoryMB}!g
s!@HDPMapRedClusterReduceMemoryMB@!${HDPMapRedClusterReduceMemoryMB}!g
s!@HDPMapRedClusterMaxMapMemoryMB@!${HDPMapRedClusterMaxMapMemoryMB}!g
s!@HDPMapRedClusterMaxReduceMemoryMB@!${HDPMapRedClusterMaxReduceMemoryMB}!g
s!@HDPMapRedJobMapMemoryMB@!${HDPMapRedJobMapMemoryMB}!g
s!@HDPMapRedJobReduceMemoryMB@!${HDPMapRedJobReduceMemoryMB}!g
s!@HDPMapRedChildJavaOptsSize@!${HDPMapRedChildJavaOptsSize}!g
s!@HDPIoSortMB@!${HDPIoSortMB}!g
s!@HDPIoSortSpillPercent@!${HDPIoSortSpillPercent}!g
s!@HDPMapReduceUserLogRetainHours@!${HDPMapReduceUserLogRetainHours}!g
s!@HDPMaxTasksPerJob@!${HDPMaxTasksPerJob}!g
s!@HDPDFSDataNodeFailedVolumeTolerated@!${HDPDFSDataNodeFailedVolumeTolerated}!g
s!@HDPHBaseMasterHeapSize@!${HDPHBaseMasterHeapSize:-1024m}!g
s!@HDPHBaseRegionServerHeapSize@!${HDPHBaseRegionServerHeapSize:-1024m}!g
EOConf

# The escaped quotes around ${gsClusterConf} are important because 
# we're passing in a multi-line blob as a string, so the command-line 
# invocation needs to have the blob quoted so storeDbConfForCluster can treat
# it as a single string.
cmd="storeDbConfForCluster ${clusterName} ${GSCLUSTER_PROPERTIES_CONF_NAME} \"${gsClusterConf}\"";

echo "${cmd}";
eval "${cmd}";

# Fetch the next stage's configuration
nextJobName="5-HDP-Deploy";
nextJobConfigFile="${perBuildWorkspace}/${nextJobName}.config.xml";

cmd="fetchJobConfigTemplate ${nextJobName} ${nextJobConfigFile}";

echo "${cmd}";
eval "${cmd}";

# Modify this fetched config to take into account the output of all the jobs till now.

isHBaseInstalled=`isServiceInstalledForCluster "${clusterName}" "HBase"`;
isPigInstalled=`isServiceInstalledForCluster "${clusterName}" "Pig"`;
isHCatalogInstalled=`isServiceInstalledForCluster "${clusterName}" "HCatalog"`;
isTempletonInstalled=`isServiceInstalledForCluster "${clusterName}" "Templeton"`;
isOozieInstalled=`isServiceInstalledForCluster "${clusterName}" "Oozie"`;
isSqoopInstalled=`isServiceInstalledForCluster "${clusterName}" "Sqoop"`;

nameNodeHost=`getServiceHost "namenode"`;
secondaryNameNodeHost=`getServiceHost "snamenode"`;
jobTrackerHost=`getServiceHost "jobtracker"`;
gangliaCollectorHost=`getServiceHost "gangliaserver"`;
nagiosServerHost=`getServiceHost "nagiosserver"`;

optionalServicesConfigChoicesXml=`generateOptionalServicesConfigChoicesXml \
  "${isHBaseInstalled}" "${isHCatalogInstalled}" "${isTempletonInstalled}" "${isOozieInstalled}"`;

cmd="sed -i.prev \
	-e \"s!@HDPInstallHBaseChoice@!${isHBaseInstalled}!g\" \
	-e \"s!@HDPInstallPigChoice@!${isPigInstalled}!g\" \
	-e \"s!@HDPInstallHCatalogChoice@!${isHCatalogInstalled}!g\" \
	-e \"s!@HDPInstallTempletonChoice@!${isTempletonInstalled}!g\" \
	-e \"s!@HDPInstallOozieChoice@!${isOozieInstalled}!g\" \
	-e \"s!@HDPInstallSqoopChoice@!${isSqoopInstalled}!g\" \
	-e \"s!@HDPNameNodeHostChoice@!${nameNodeHost}!g\" \
	-e \"s!@HDPSecondaryNameNodeHostChoice@!${secondaryNameNodeHost}!g\" \
	-e \"s!@HDPJobTrackerHostChoice@!${jobTrackerHost}!g\" \
	-e \"s!@HDPGangliaCollectorHostChoice@!${gangliaCollectorHost}!g\" \
	-e \"s!@HDPNagiosServerHostChoice@!${nagiosServerHost}!g\" \
	-e \"s!@HDPOptionalServicesConfigChoices@!${optionalServicesConfigChoicesXml}!g\" \
      ${nextJobConfigFile}";

echo "${cmd}";
eval "${cmd}";

# "Pipe" the output of this job into the next one's configuration.
cmd="curl -H \"Content-Type: text/xml\" -d @${nextJobConfigFile} ${JENKINS_URL}job/${nextJobName}/config.xml";

echo "${cmd}";
eval "${cmd}";

# XXX TODO Advance the stage disk cookie.
