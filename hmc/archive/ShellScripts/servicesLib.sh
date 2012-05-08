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

##################################
###### INCLUDE REQUIREMENTS ######
##################################
#
# * easyInstallerLib.sh
# * dbLib.sh
#
##################################

#################
### FUNCTIONS ###
#################

function getServiceStatusForCluster
{
  local serviceStatus="";

  local clusterName="${1}";
  local serviceName="${2}";

  if [ "x" != "x${clusterName}" ] && [ "x" != "x${serviceName}" ]
  then
    serviceStatus=`fetchDbCookieForCluster "${clusterName}" "${serviceName}"`;
  fi

  echo "${serviceStatus}";
}

function isServiceInstalledForCluster
{
  local serviceIsInstalled="no";

  local serviceStatus=`getServiceStatusForCluster "$@"`;

  if [ "x" != "x${serviceStatus}" ]
  then
    serviceIsInstalled="yes";
  fi

  echo "${serviceIsInstalled}";
}

function isServiceStartedForCluster
{
  local serviceIsStarted="no";

  local serviceStatus=`getServiceStatusForCluster "$@"`;

  if [ "x1" == "x${serviceStatus}" ]
  then
    serviceIsStarted="yes";
  fi

  echo "${serviceIsStarted}";
}

function isServiceStoppedForCluster
{
  local serviceIsStopped="no";

  local serviceStatus=`getServiceStatusForCluster "$@"`;

  if [ "x0" == "x${serviceStatus}" ]
  then
    serviceIsStopped="yes";
  fi

  echo "${serviceIsStopped}";
}

function setServiceStatusForCluster
{
  local clusterName="${1}";
  local serviceName="${2}";
  local serviceStatus="${3}";

  if [ "x" != "x${clusterName}" ] && [ "x" != "x${serviceName}" ] && [ "x" != "x${serviceStatus}" ]
  then
    storeDbCookieForCluster "${clusterName}" "${serviceName}" "${serviceStatus}";
  fi
}

function markServiceAsStartedForCluster
{
  setServiceStatusForCluster "$@" "1";
}

function markServiceAsStoppedForCluster
{
  setServiceStatusForCluster "$@" "0";
}

function generateServiceStatusChoicesForCluster
{
  local serviceStatusChoices="";

  local clusterName="${1}";
  local serviceName="${2}";

  if [ "x" != "x${clusterName}" ] && [ "x" != "x${serviceName}" ]
  then
    local currentStatusChoice="";

    local serviceIsStarted=`isServiceStartedForCluster "${clusterName}" "${serviceName}"`;

    if [ "xyes" == "x${serviceIsStarted}" ]
    then
      currentStatusChoice=`wrapWithXMLTag "string" "Started"`;
    else
      currentStatusChoice=`wrapWithXMLTag "string" "Stopped"`;
    fi 

    serviceStatusChoices="${currentStatusChoice}";
  fi

  echo "${serviceStatusChoices}";
}

function generateServiceStatusChoicesXmlForCluster
{
  local serviceStatusChoicesXml="";

  local clusterName="${1}";
  local serviceName="${2}";

  if [ "x" != "x${clusterName}" ] && [ "x" != "x${serviceName}" ]
  then
    serviceStatusChoices=`generateServiceStatusChoicesForCluster "${clusterName}" "${serviceName}"`;

    serviceStatusChoicesXml="<hudson.model.ChoiceParameterDefinition>\
<name>HDP${serviceName}Status</name>\
<description></description>\
<choices class=\\\"java.util.Arrays\\\$ArrayList\\\">\
<a class=\\\"string-array\\\">\
${serviceStatusChoices}\
</a>\
</choices>\
</hudson.model.ChoiceParameterDefinition>";
  fi

  echo "${serviceStatusChoicesXml}";
}

function generateOptionalServicesStatusChoicesXmlForCluster
{
  local optionalServicesStatusChoicesXml="";

  local clusterName="${1}";

  local isHBaseInstalled="${2}"; 
  local isHCatalogInstalled="${3}";
  local isTempletonInstalled="${4}";
  local isOozieInstalled="${5}";

  if [ "x" != "x${clusterName}" ]
  then
    if [ "xyes" == "x${isHBaseInstalled}" ]
    then
      hBaseStatusChoicesXml=`generateServiceStatusChoicesXmlForCluster \
        "${clusterName}" "HBase"`;
      optionalServicesStatusChoicesXml="${optionalServicesStatusChoicesXml}${hBaseStatusChoicesXml}";
    fi

    if [ "xyes" == "x${isHCatalogInstalled}" ]
    then
      hCatalogStatusChoicesXml=`generateServiceStatusChoicesXmlForCluster \
        "${clusterName}" "HCatalog"`;
      optionalServicesStatusChoicesXml="${optionalServicesStatusChoicesXml}${hCatalogStatusChoicesXml}";
    fi

    if [ "xyes" == "x${isTempletonInstalled}" ]
    then
      templetonStatusChoicesXml=`generateServiceStatusChoicesXmlForCluster \
        "${clusterName}" "Templeton"`;
      optionalServicesStatusChoicesXml="${optionalServicesStatusChoicesXml}${templetonStatusChoicesXml}";
    fi

    if [ "xyes" == "x${isOozieInstalled}" ]
    then
      oozieStatusChoicesXml=`generateServiceStatusChoicesXmlForCluster \
        "${clusterName}" "Oozie"`;
      optionalServicesStatusChoicesXml="${optionalServicesStatusChoicesXml}${oozieStatusChoicesXml}";
    fi
  fi

  echo "${optionalServicesStatusChoicesXml}";
}
