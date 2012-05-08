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

# Stage gsInstaller.

# 0) Create the staging directory.
stagingDirectory="${perBuildWorkspace}/gsInstaller";

cmd="mkdir ${stagingDirectory}";

echo "${cmd}";
eval "${cmd}";

# 1) Copy the sources over to ${stagingDirectory}.
cmd="cp -r ${GSINSTALLER_SCRIPTS_DIR}/* ${stagingDirectory}";

echo "${cmd}";
eval "${cmd}";

# 2) Pull in the customized (till this point in the flow) .properties files.
for propertiesFile in "${GSINSTALLER_PROPERTIES_CONF_NAME}" "${GSCLUSTER_PROPERTIES_CONF_NAME}"
do
  storedPropertiesFile=`fetchDbConfForCluster "${clusterName}" "${propertiesFile}"`;

  cmd="cp -pf ${storedPropertiesFile} ${stagingDirectory}";

  echo "${cmd}";
  eval "${cmd}";
done
 
# 3) Pull in all the flat node files.
for nodeFile in "namenode" "snamenode" "jobtracker" "dashboard" "gangliaserver" "gateway" "hbasemaster" "hbasenodes" "hcatserver" "nagiosserver" "nodes" "oozieserver" "templetonnode" "zknodes"
do
  storedNodeFile=`fetchDbFileForCluster "${clusterName}" "${nodeFile}"`;

  cmd="cp -pf ${storedNodeFile} ${stagingDirectory}";

  echo "${cmd}";
  eval "${cmd}";
done

# And we're done!

######################
### THE MAIN EVENT ###
######################

# XXX TODO Break this into separate commands and work upon the exit code of each.
jobCmd="cd ${stagingDirectory}; sh createUsers.sh; sh gsPreRequisites.sh; echo y | sh gsInstaller.sh; echo y | sh monInstaller.sh;";
#jobCmd="cd ${stagingDirectory}; echo *******xxxyyyzzz********;";

echo "${jobCmd}";
eval "${jobCmd}";

jobExitCode=$?;

#######################
### POST-PROCESSING ###
#######################

# Mark the start/stop status of all the installed services on disk.

for serviceName in "HDFS" "MapReduce" "HBase" "HCatalog" "Templeton" "Oozie"
do
  serviceIsInstalled=`isServiceInstalledForCluster "${clusterName}" "${serviceName}"`;

  # Generate customized variables, one for each ${serviceName}, for use below.
  eval "is${serviceName}Installed=${serviceIsInstalled}";

  if [ "xyes" == "x${serviceIsInstalled}" ]
  then
    if [ "x0" == "x${jobExitCode}" ]
    then
      markServiceAsStartedForCluster "${clusterName}" "${serviceName}";
    else
      markServiceAsStoppedForCluster "${clusterName}" "${serviceName}";
    fi
  fi
done

# Update the downstream pages' configurations accordingly.

# Fetch the next page's configuration.
nextJobName="HDP-Manage-Services";
nextJobConfigFile="${perBuildWorkspace}/${nextJobName}.config.xml";

cmd="fetchJobConfigTemplate ${nextJobName} ${nextJobConfigFile}";

echo "${cmd}";
eval "${cmd}";

# Modify this fetched config to take into account the output of all the jobs till now.

hdfsStatusChoices=`generateServiceStatusChoicesForCluster "${clusterName}" "HDFS"`;
mapReduceStatusChoices=`generateServiceStatusChoicesForCluster "${clusterName}" "MapReduce"`;

# Note that the is${serviceName}Installed variables were generated and set in the
# loop above that invokes markServiceAsStartedForCluster() for each ${serviceName}.
optionalServicesStatusChoicesXml=`generateOptionalServicesStatusChoicesXmlForCluster \
  "${clusterName}" \
  "${isHBaseInstalled}" "${isHCatalogInstalled}" "${isTempletonInstalled}" "${isOozieInstalled}"`;

cmd="sed -i.prev \
	-e \"s!@HDPHDFSStatusChoices@!${hdfsStatusChoices}!g\" \
	-e \"s!@HDPMapReduceStatusChoices@!${mapReduceStatusChoices}!g\" \
	-e \"s!@HDPOptionalServicesStatusChoices@!${optionalServicesStatusChoicesXml}!g\" \
      ${nextJobConfigFile}";

echo "${cmd}";
eval "${cmd}";

# "Pipe" the output of this job into the relevant job's configuration.
cmd="curl -H \"Content-Type: text/xml\" -d @${nextJobConfigFile} ${JENKINS_URL}job/${nextJobName}/config.xml";

echo "${cmd}";
eval "${cmd}";

# XXX TODO Advance the stage disk cookie.
