#!/usr/bin/env bash

for i in `find ../../../ -regex '.*/configuration.*/.*.xml'` ;
do
  echo $i;

  xmlstarlet ed --inplace -d "//on-ambari-upgrade" $i
  xmlstarlet ed --inplace -d "//on-stack-upgrade" $i

  `xmlstarlet ed --inplace --subnode "/configuration/property" -t 'elem' -n "on-ambari-upgrade" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-ambari-upgrade" -t 'attr' -n "add" -v "false" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-ambari-upgrade" -t 'attr' -n "change" -v "true" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-ambari-upgrade" -t 'attr' -n "delete" -v "true" $i`;

  `xmlstarlet ed --inplace --subnode "/configuration/property" -t 'elem' -n "on-stack-upgrade" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-stack-upgrade" -t 'attr' -n "add" -v "true" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-stack-upgrade" -t 'attr' -n "change" -v "true" $i`;
  `xmlstarlet ed --inplace --subnode "/configuration/property/on-stack-upgrade" -t 'attr' -n "delete" -v "false" $i`;
done
