#!/bin/sh

AAPATH=~/ambari

function dodir {
  if [ -d $1 ]; then
    mv $1 "$1.`date +%s`"
  fi
  
  cp -R $AAPATH/$2 $1

}

dodir /usr/lib/python2.6/site-packages/ambari_agent ambari-agent/src/main/python/ambari_agent
dodir /usr/lib/python2.6/site-packages/resource_management ambari-agent/src/main/python/resource_management
dodir /usr/lib/python2.6/site-packages/common_functions ambari-common/src/main/python/common_functions

dodir /var/lib/ambari-agent/cache/stacks/HDP ambari-server/src/main/resources/stacks/HDP

if [ ! -d /var/lib/ambari-agent/resources ]; then
  mkdir -p /var/lib/ambari-agent/resources
fi
dodir /var/lib/ambari-agent/resources/custom_actions ambari-server/src/main/resources/custom_actions

cat $AAPATH/version > /var/lib/ambari-agent/data/version
