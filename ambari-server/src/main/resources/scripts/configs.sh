#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

usage () {
  echo "Usage: configs.sh [-u userId] [-p password] [-port port] <ACTION> <AMBARI_HOST> <CLUSTER_NAME> <CONFIG_TYPE> [CONFIG_FILENAME | CONFIG_KEY [CONFIG_VALUE]]";
  echo "";
  echo "       [-u userId]: Optional user ID to use for authentication. Default is 'admin'.";
  echo "       [-p password]: Optional password to use for authentication. Default is 'admin'.";
  echo "       [-port port]: Optional port number for Ambari server. Default is '8080'. Provide empty string to not use port.";
  echo "       <ACTION>: One of 'get', 'set', 'delete'. 'Set' adds/updates as necessary.";
  echo "       <AMBARI_HOST>: Server external host name";
  echo "       <CLUSTER_NAME>: Name given to cluster. Ex: 'c1'"
  echo "       <CONFIG_TYPE>: One of the various configuration types in Ambari. Ex:global, core-site, hdfs-site, mapred-queue-acls, etc.";
  echo "       [CONFIG_FILENAME]: File where entire configurations are saved to, or read from. Only applicable to 'get' and 'set' actions";
  echo "       [CONFIG_KEY]: Key that has to be set or deleted. Not necessary for 'get' action.";
  echo "       [CONFIG_VALUE]: Optional value to be set. Not necessary for 'get' or 'delete' actions.";
  exit 1;
}

USERID="admin"
PASSWD="admin"
PORT=":8080"

if [ "$1" == "-u" ] ; then
  USERID=$2;
  shift 2;
  echo "USERID=$USERID";
fi

if [ "$1" == "-p" ] ; then
  PASSWD=$2;
  shift 2;
  echo "PASSWORD=$PASSWD";
fi

if [ "$1" == "-port" ] ; then
  if [ -z $2 ]; then
    PORT="";
  else
    PORT=":$2";
  fi
  shift 2;
  echo "PORT=$PORT";
fi

AMBARIURL="http://$2$PORT"
CLUSTER=$3
SITE=$4
SITETAG=''
CONFIGKEY=$5
CONFIGVALUE=$6

###################
## currentSiteTag()
###################
currentSiteTag () {
  currentSiteTag=''
  found=''
    
  #currentSite=`cat ds.json | grep -E "$SITE|tag"`; 
  currentSite=`curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$CLUSTER?fields=Clusters/desired_configs" | grep -E "$SITE|tag"`;
  for line in $currentSite; do
    if [ $line != "{" -a $line != ":" -a $line != '"tag"' ] ; then
      if [ -n "$found" -a -z "$currentSiteTag" ]; then
        currentSiteTag=$line;
      fi
      if [ $line == "\"$SITE\"" ]; then
        found=$SITE; 
      fi
    fi
  done;
  if [ -z $currentSiteTag ]; then
    errOutput=`curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$CLUSTER?fields=Clusters/desired_configs"`;
    echo "[ERROR] \"$SITE\" not found in server response.";
    echo "[ERROR] Output of \`curl -s -u $USERID:$PASSWD \"$AMBARIURL/api/v1/clusters/$CLUSTER?fields=Clusters/desired_configs\"\` is:";
    echo $errOutput | while read -r line; do
      echo "[ERROR] $line";
    done;
    exit 1;
  fi
  currentSiteTag=`echo $currentSiteTag|cut -d \" -f 2`
  SITETAG=$currentSiteTag;
}

#############################################
## doConfigUpdate() 
##  @param MODE of update. Either 'set' or 'delete'
#############################################
doConfigUpdate () {
  MODE=$1
  currentSiteTag
  echo "########## Performing '$MODE' $CONFIGKEY:$CONFIGVALUE on (Site:$SITE, Tag:$SITETAG)";
  propertiesStarted=0;
  curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$CLUSTER/configurations?type=$SITE&tag=$SITETAG" | while read -r line; do
    ## echo ">>> $line";
    if [ "$propertiesStarted" -eq 0 -a "`echo $line | grep "\"properties\""`" ]; then
      propertiesStarted=1
    fi;
    if [ "$propertiesStarted" -eq 1 ]; then
      if [ "$line" == "}" ]; then
        ## Properties ended
        ## Add property
        if [ "$MODE" == "set" ]; then
          newProperties="$newProperties, \"$CONFIGKEY\" : \"$CONFIGVALUE\" ";
        elif [ "$MODE" == "delete" ]; then
          # Remove the last ,
          propLen=${#newProperties}
          lastChar=${newProperties:$propLen-1:1}
          if [ "$lastChar" == "," ]; then
            newProperties=${newProperties:0:$propLen-1}
          fi
        fi
        newProperties=$newProperties$line
        propertiesStarted=0;
        
        newTag=`date "+%s"`
        newTag="version${newTag}000"
        finalJson="{ \"Clusters\": { \"desired_config\": {\"type\": \"$SITE\", \"tag\":\"$newTag\", $newProperties}}}"
        newFile="doSet_$newTag.json"
        echo "########## PUTting json into: $newFile"
        echo $finalJson > $newFile
        curl -u $USERID:$PASSWD -X PUT -H "X-Requested-By: ambari" "$AMBARIURL/api/v1/clusters/$CLUSTER" --data @$newFile
        currentSiteTag
        echo "########## NEW Site:$SITE, Tag:$SITETAG";
      elif [ "`echo $line | grep "\"$CONFIGKEY\""`" ]; then
        echo "########## Config found. Skipping origin value"
      else
        newProperties=$newProperties$line
      fi
    fi
  done;
}

#############################################
## doConfigFileUpdate() 
##  @param File name to PUT on server
#############################################
doConfigFileUpdate () {
  FILENAME=$1
  if [ -f $FILENAME ]; then
    if [ "1" == "`grep -n \"\"properties\"\" $FILENAME | cut -d : -f 1`" ]; then
      newTag=`date "+%s"`
      newTag="version${newTag}000"
      newProperties=`cat $FILENAME`;
      finalJson="{ \"Clusters\": { \"desired_config\": {\"type\": \"$SITE\", \"tag\":\"$newTag\", $newProperties}}}"
      newFile="PUT_$FILENAME"
      echo $finalJson>$newFile
      echo "########## PUTting file:\"$FILENAME\" into config(type:\"$SITE\", tag:$newTag) via $newFile"
      curl -u $USERID:$PASSWD -X PUT -H "X-Requested-By: ambari" "$AMBARIURL/api/v1/clusters/$CLUSTER" --data @$newFile
      currentSiteTag
      echo "########## NEW Site:$SITE, Tag:$SITETAG";
    else
      echo "[ERROR] File \"$FILENAME\" should be in the following JSON format:";
      echo "[ERROR]   \"properties\": {";
      echo "[ERROR]     \"key1\": \"value1\",";
      echo "[ERROR]     \"key2\": \"value2\",";
      echo "[ERROR]   }";
      exit 1;
    fi
  else
    echo "[ERROR] Cannot find file \"$1\"to PUT";
    exit 1;
  fi
}


#############################################
## doGet()
##  @param Optional filename to save to
#############################################
doGet () {
  FILENAME=$1
  if [ -n $FILENAME -a -f $FILENAME ]; then
    rm -f $FILENAME;
  fi
  currentSiteTag
  echo "########## Performing 'GET' on (Site:$SITE, Tag:$SITETAG)";
  propertiesStarted=0;
  curl -s -u $USERID:$PASSWD "$AMBARIURL/api/v1/clusters/$CLUSTER/configurations?type=$SITE&tag=$SITETAG" | while read -r line; do
    ## echo ">>> $line";
    if [ "$propertiesStarted" -eq 0 -a "`echo $line | grep "\"properties\""`" ]; then
      propertiesStarted=1
    fi;
    if [ "$propertiesStarted" -eq 1 ]; then
      if [ "$line" == "}" ]; then
        ## Properties ended
        propertiesStarted=0;
      fi
      if [ -z $FILENAME ]; then
        echo $line
      else
        echo $line >> $FILENAME
      fi
    fi
  done;
}

case "$1" in
  set)
    if (($# == 6)); then
      doConfigUpdate "set" # Individual key
    elif (($# == 5)); then
      doConfigFileUpdate $5 # File based
    else
      usage
    fi
    ;;
  get)
    if (($# == 4)); then
      doGet
    elif (($# == 5)); then
      doGet $5
    else
      usage
    fi
    ;;
  delete)
    if (($# != 5)); then
      usage
    fi
    doConfigUpdate "delete"
    ;;
  *) 
    usage
    ;;
esac
