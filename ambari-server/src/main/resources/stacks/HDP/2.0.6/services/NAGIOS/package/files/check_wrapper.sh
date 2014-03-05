#!/bin/sh

function real_service() {
  desc=$NAGIOS_SERVICEGROUPNAME
  eval "$1='$desc'"
}

function real_component() {
  arrDesc=(${NAGIOS_SERVICEDESC//::/ })

  compName="${arrDesc[0]}"

  case "$compName" in
    HBASEMASTER)
      realCompName="HBASE_MASTER"
    ;;
    REGIONSERVER)
      realCompName="HBASE_REGIONSERVER"
    ;;
    JOBHISTORY)
      realCompName="MAPREDUCE2"
    ;;
    HIVE-METASTORE)
      realCompName="HIVE_METASTORE"
    ;;
    HIVE-SERVER)
      realCompName="HIVE_SERVER"
    ;;
    FLUME)
      realCompName="FLUME_SERVER"
    ;;
    HUE)
      realCompName="HUE_SERVER"
    ;;
    WEBHCAT)
      realCompName="WEBHCAT_SERVER"
    ;;
    *)
      realCompName=$compName
    ;;
  esac

  eval "$1='$realCompName'"
}

real_service_var=""
real_service real_service_var

real_comp_var=""
real_component real_comp_var


wrapper_output=`exec $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11} ${12} ${13} ${14} ${15} ${16} ${17} ${18} ${19} ${20}`
wrapper_result=$?

if [ "$wrapper_result" == "0" ]; then
  echo "$wrapper_output"
  exit $wrapper_result
fi

if [ ! -f /var/nagios/ignore.dat ]; then
  echo "$wrapper_output"
  exit $wrapper_result
else
  count=`grep $NAGIOS_HOSTNAME /var/nagios/ignore.dat | grep $real_service_var | grep $real_comp_var | wc -l`
  if [ "$count" -ne "0" ]; then
    echo "$wrapper_output\nAMBARIPASSIVE=${wrapper_result}" | sed 's/^[ \t]*//g'
    exit 0
  else
    echo "$wrapper_output"
    exit $wrapper_result
  fi
fi

