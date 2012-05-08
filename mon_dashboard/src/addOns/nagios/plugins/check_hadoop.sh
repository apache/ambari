#!/bin/bash

user=""
secure="false"
keytab=""
while getopts ":u:k:s" opt; do
  case $opt in
    u)
      user=$OPTARG;
      ;;
    k)
      keytab=$OPTARG;
      ;;
    s)
      secure="true";
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 3
      ;;
    :)
      echo "UNKNOWNOption -$OPTARG requires an argument." >&2
      exit 3
      ;;
  esac
done

outfile="/tmp/nagios-hadoop-check.out"
curtime=`date +"%F-%H-%M-%S"`
fname="nagios-hadoop-check-${curtime}"

if [[ "$user" == "" ]]; then
  echo "INVALID: user argument not specified";
  exit 3;
fi
if [[ "$keytab" == "" ]]; then 
  keytab="/homes/$user/$user.headless.keytab"
fi

if [[ "$secure" == "true" ]]; then
  sudo -u $user -i "/usr/kerberos/bin/kinit -kt $keytab $user" > ${outfile} 2>&1
fi

sudo -u $user -i "hadoop dfs -copyFromLocal /etc/passwd ${fname}.input " > ${outfile} 2>&1
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: Error copying file to HDFS. See error output in ${outfile} on nagios server";
  exit 2; 
fi
sudo -u $user -i "hadoop dfs -ls" > ${outfile} 2>&1
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: Error listing HDFS files. See error output in ${outfile} on nagios server";
  exit 2; 
fi
sudo -u $user -i "hadoop jar /usr/share/hadoop/hadoop-examples-*.jar wordcount ${fname}.input ${fname}.out" >> ${outfile} 2>&1
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: Error running M/R job. See error output in ${outfile} on nagios server";
  exit 2; 
fi
sudo -u $user -i "hadoop fs -rmr -skipTrash ${fname}.out" >> ${outfile} 2>&1
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: Error removing M/R job output. See error output in ${outfile} on nagios server";
  exit 2; 
fi
sudo -u $user -i "hadoop fs -rm -skipTrash ${fname}.input" >> ${outfile} 2>&1
if [[ "$?" -ne "0" ]]; then 
  echo "CRITICAL: Error removing M/R job input. See error output in ${outfile} on nagios server";
  exit 2; 
fi

echo "OK: M/R WordCount Job ran successfully"
exit 0;
