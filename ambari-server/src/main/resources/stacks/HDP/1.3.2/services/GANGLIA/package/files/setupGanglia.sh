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

cd `dirname ${0}`;

# Get access to Ganglia-wide constants, utilities etc.
source ./gangliaLib.sh

function usage()
{
  cat << END_USAGE
Usage: ${0} [-c <gmondClusterName> [-m]] [-t] [-o <owner>] [-g <group>]

Options:
  -c <gmondClusterName>   The name of the Ganglia Cluster whose gmond configuration we're here to generate.

  -m                      Whether this gmond (if -t is not specified) is the master for its Ganglia 
                          Cluster. Without this, we generate slave gmond configuration.

  -t                      Whether this is a call to generate gmetad configuration (as opposed to the
                          gmond configuration that is generated without this).
  -o <owner>              Owner
  -g <group>              Group
END_USAGE
}

function instantiateGmetadConf()
{
  # gmetad utility library.
  source ./gmetadLib.sh;

  generateGmetadConf > ${GMETAD_CONF_FILE};
}

function instantiateGmondConf()
{
  # gmond utility library.
  source ./gmondLib.sh;
 
  gmondClusterName=${1};

  if [ "x" != "x${gmondClusterName}" ]
  then

    createDirectory "${GANGLIA_RUNTIME_DIR}/${gmondClusterName}";
    createDirectory "${GANGLIA_CONF_DIR}/${gmondClusterName}/conf.d";
    
    # Always blindly generate the core gmond config - that goes on every box running gmond. 
    generateGmondCoreConf ${gmondClusterName} > `getGmondCoreConfFileName ${gmondClusterName}`;

    isMasterGmond=${2};

    # Decide whether we want to add on the master or slave gmond config.
    if [ "0" -eq "${isMasterGmond}" ]
    then
      generateGmondSlaveConf ${gmondClusterName} > `getGmondSlaveConfFileName ${gmondClusterName}`;
    else
      generateGmondMasterConf ${gmondClusterName} > `getGmondMasterConfFileName ${gmondClusterName}`;
    fi

    chown -R ${3}:${4} ${GANGLIA_CONF_DIR}/${gmondClusterName}

  else
    echo "No gmondClusterName passed in, nothing to instantiate";
  fi
}

# main()

gmondClusterName=;
isMasterGmond=0;
configureGmetad=0;
owner='root';
group='root';

while getopts ":c:mto:g:" OPTION
do
  case ${OPTION} in
    c) 
      gmondClusterName=${OPTARG};
      ;;
    m)
      isMasterGmond=1;
      ;;
    t)
      configureGmetad=1;
      ;;
    o)
      owner=${OPTARG};
      ;;
    g)
      group=${OPTARG};
      ;;
    ?)
      usage;
      exit 1;
  esac
done

# Initialization.
createDirectory ${GANGLIA_CONF_DIR};
createDirectory ${GANGLIA_RUNTIME_DIR};
# So rrdcached can drop its PID files in here.
chmod a+w ${GANGLIA_RUNTIME_DIR};
chown ${owner}:${group} ${GANGLIA_CONF_DIR};

if [ -n "${gmondClusterName}" ]
then

  # Be forgiving of users who pass in -c along with -t (which always takes precedence).
  if [ "1" -eq "${configureGmetad}" ]
  then
    instantiateGmetadConf;
  else
    instantiateGmondConf ${gmondClusterName} ${isMasterGmond} ${owner} ${group};
  fi

elif [ "1" -eq "${configureGmetad}" ]
then
  instantiateGmetadConf;
else
  usage;
  exit 2;
fi
