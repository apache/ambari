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
# * dbLib.sh
#
##################################

PREVIOUS_VERSION_SUFFIX=".prev";

function getClusterDbDir
{
  clusterName=${1};

  if [ "x" != "x${clusterName}" ]
  then
    echo "${EASYINSTALLER_DB_DIR}/${clusterName}";
  fi
}

function initDbForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ]
  then
    clusterDbDirVersioned="${clusterDbDir}.`date +%s`";

    # Create the actual new DB directory (versioned with a timestamp).
    mkdir -p "${clusterDbDirVersioned}";

    if [ -L "${clusterDbDir}" ] 
    then
      clusterDbDirPrevious="${clusterDbDir}${PREVIOUS_VERSION_SUFFIX}";

      # There's about to be a new ${clusterDbDirPrevious}, so get the existing
      # one out of the way.
      if [ -L "${clusterDbDirPrevious}" ]
      then
        rm -f "${clusterDbDirPrevious}";
      fi

      # Roll the current "current" over...
      ln -s `readlink -n "${clusterDbDir}"` "${clusterDbDirPrevious}";
      # ...and make way for the new "current".
      rm -f "${clusterDbDir}";
    fi

    # Mark the newly-created version as "current".
    ln -s ${clusterDbDirVersioned} ${clusterDbDir};

    # And from here on, work with ${clusterDbDir} as if it were the only version in existence.
    mkdir -p ${clusterDbDir}/conf;
    mkdir -p ${clusterDbDir}/cookies;
    mkdir -p ${clusterDbDir}/files;
  else
    echo "Can't init DB for empty clusterName"
    return 1;
  fi
}

function storeDbFileForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ] && [ "x" != "x${3}" ]
  then
    storedFile="${clusterDbDir}/files/${2}";
    sourceFile="${3}";

    # Make sure we preserve modes, perms etc.
    [ -e "${sourceFile}" ] && cp -pf "${sourceFile}" "${storedFile}" && echo "${storedFile}";
  fi 
}

function fetchDbFileForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ]
  then
    storedFile="${clusterDbDir}/files/${2}";

    [ -e "${storedFile}" ] && echo "${storedFile}";
  fi 
}

function storeDbConfForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ] && [ "x" != "x${3}" ]
  then
    storedConfFile="${clusterDbDir}/conf/${2}";
    newConfReplacements="${3}";

    # The first store needs us to copy in the template for ${storedConfFile}.
    # Subsequent stores will build on this previously-partially-instantiated 
    # template.
    if [ ! -e "${storedConfFile}" ]
    then
      confTemplateFile="${EASYINSTALLER_CONF_TEMPLATES_DIR}/${2}.in";
      cp "${confTemplateFile}" "${storedConfFile}";
    fi

    sedCmd="sed -i.prev";

    # It's crucial that ${newConfReplacements} not be quoted here so it behaves
    # like a stream with one expression per line.
    for sedExpression in ${newConfReplacements}
    do
      sedCmd="${sedCmd} -e \"${sedExpression}\""; 
    done

    sedCmd="${sedCmd} ${storedConfFile}";

    echo "${sedCmd}";
    eval "${sedCmd}";
  fi
}

function fetchDbConfForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ]
  then
    storedConfFile="${clusterDbDir}/conf/${2}";

    [ -e "${storedConfFile}" ] && echo "${storedConfFile}";
  fi 
}

function storeDbCookieForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ] && [ "x" != "x${3}" ]
  then
    storedCookieFile="${clusterDbDir}/cookies/${2}";

    echo "${3}" > "${storedCookieFile}";
  fi 
}

function fetchDbCookieForCluster
{
  clusterDbDir=`getClusterDbDir ${1}`;

  if [ "x" != "x${clusterDbDir}" ] && [ "x" != "x${2}" ]
  then
    storedCookieFile="${clusterDbDir}/cookies/${2}";

    [ -e "${storedCookieFile}" ] && getFirstWordFromFile "${storedCookieFile}";
  fi 
}
