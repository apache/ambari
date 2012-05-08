inputDir=$1
outputFile=$3
inputFile=$2
maxMapsPerNode=4
maxRedsPerNode=2
check=$4
echo XXXX $inputFile XXXX
echo XXXX $inputDir XXXX
echo XXXX $outputFile XXXX

source $inputFile

cd $inputDir
[[ -f "gateway" ]] && gwhost=$(cat gateway)
[[ -f "namenode" ]] && nnhost=$(cat namenode)
[[ -f "snamenode" ]] && snhost=$(cat snamenode)
[[ -f "jobtracker" ]] && jthost=$(cat jobtracker)
[[ -f "hbasemaster" ]] && hbmhost=$(cat hbasemaster)
[[ -f "hcatserver" ]] && hcshost=$(cat hcatserver)
[[ -f "templetonnode" ]] && ttonhosts=$(cat templetonnode)
[[ -f "oozieserver" ]] && oozieshost=$(cat oozieserver)
[[ -f "nodes" ]] && slaves=$(cat nodes)
[[ -f "hbasenodes" ]] && rshosts=$(cat hbasenodes)
[[ -f "zknodes" ]] && zkhosts=$(cat zknodes)
[[ -f "gangliaserver" ]] && gangliahost=$(cat gangliaserver)
[[ -f "nagiosserver" ]] && nagioshost=$(cat nagiosserver)

maxMem=
heapSizeSuggest=
ConnectTimeOut=3
sshCmdOnHost ()
{
  local cmd="ssh -o ConnectTimeOut=$ConnectTimeOut -o StrictHostKeyChecking=no -i $HDPClusterDeployUserIdentityFile $HDPClusterDeployUser@$1 \"$2\" > $3"
  echo $cmd
  eval $cmd
}

findMaxMemOnHost ()
{
  #  echo "SSHCMD: ssh -i /var/db/hdp/easyInstaller/MyHDPCluster/files/clusterDeployUserIdentity root@$1 \"free -m | sed \\\"1 d\\\" | awk '{print \\\$4}' | sed -n 1p\""
  sshCmdOnHost $1 "free -m" "${PWD}/tmpMem.out"
#  ssh -i $HDPClusterDeployUserIdentityFile $HDPClusterDeployUser@$1 "free -m" > tmpMem.out
  maxMem=`cat $PWD/tmpMem.out | sed "1 d" | awk '{print $4}' | sed -n 1p`
}

scaleByThreads=2
scaleRelativeMapsReds=3
minMapsPerNode=1000
minRedsPerNode=1000
findMaxCoresOnHost ()
{
  sshCmdOnHost $1 "grep -c processor /proc/cpuinfo" ${PWD}/tmpCpu.out
  cpuCount=`cat ${PWD}/tmpCpu.out`
  maxMapsPerNode=`expr $cpuCount \* $scaleByThreads \* 2 / $scaleRelativeMapsReds`
  maxRedsPerNode=`expr $cpuCount \* $scaleByThreads - $maxMapsPerNode`
  totalProcessesPerSlave=`expr $maxMapsPerNode + $maxRedsPerNode`

  if [ $maxMapsPerNode -lt 1 ]; then 
    maxMapsPerNode=1
  fi

  if [ $maxRedsPerNode -lt 1 ]; then
    maxRedsPerNode=1
  fi

  if [ $maxMapsPerNode -lt $minMapsPerNode ]; then
    minMapsPerNode=$maxMapsPerNode
  fi

  if [ $maxRedsPerNode -lt $minRedsPerNode ]; then
    minRedsPerNode=$maxRedsPerNode
  fi
  
  echo CPUCOUNT: $cpuCount
}

list=
for slave in $(echo $slaves)
do
  findMaxCoresOnHost $slave
done

maxMapsPerNode=$minMapsPerNode
maxRedsPerNode=$minRedsPerNode

if [ $check -eq 1 ]; then
  userInputProperties=$5
  source $userInputProperties
fi

#### -2 because -1 for count to go to 0 and -1 for allHosts
totalProcessesPerSlave=`expr $maxMapsPerNode + $maxRedsPerNode - 2`
list=$slaves
while [ $totalProcessesPerSlave -ne 0 ];
do
totalProcessesPerSlave=`expr $totalProcessesPerSlave - 1`
list="$list $slaves"
done


allHosts=`echo $slaves $jthost $nnhost $snhost $gwhost $hbmhost $hcshost $rshosts $zkhosts $nagioshost $gangliahost $list`
# echo ALLHOSTS: $allHosts

findForNodeType ()
{
  echo Hostname: $1
  hostname=$1
  echo "grep -o $1 <<< \"$allHosts\" | wc -l"
  procCount=`grep -o $1 <<< "$allHosts" | wc -l`
  echo PROCCOUNT: $procCount
  findMaxMemOnHost $1
#  findMaxCoresOnHost $1
  echo MAXMEM: $maxMem
  heapSizeSuggest=`expr $maxMem / $procCount`
}

echo NAMENODE $nnhost
findForNodeType $nnhost
nameNodeHeapSizeSuggest=$heapSizeSuggest
echo "HDPNameNodeHeapSize ${heapSizeSuggest}" > $outputFile

echo JOBTRACKER $jthost
findForNodeType $jthost
jobTrackerHeapSizeSuggest=$heapSizeSuggest
echo "HDPJobTrackerHeapSize ${heapSizeSuggest}" >> $outputFile

echo HBASEMASTER $hbmhost
if [ "x" != "x$hbmhost" ]; then
  findForNodeType $hbmhost
  hbmHeapSizeSuggest=$heapSizeSuggest
  echo "HDPHBaseMasterHeapSize=${heapSizeSuggest}" >> $outputFile
fi

# for now max value assumed to be 100G 
minSuggest=100000000000
for node in $(echo $slaves)
do
  findForNodeType $node
  if [ "$minSuggest" -gt "$heapSizeSuggest" ]; then
    minSuggest=$heapSizeSuggest
  fi
done

echo DATANODE
dataNodeHeapSizeSuggest=$minSuggest
echo "HDPDataNodeHeapSize ${minSuggest}" >> $outputFile
echo HADOOPHEAP
hadoopHeapSizeSuggest=$minSuggest
echo "HDPHadoopHeapSize ${minSuggest}" >> $outputFile
echo CHILDOPTS
childJavaOptsSize=$minSuggest
echo "HDPMapRedChildJavaOptsSize ${minSuggest}" >> $outputFile

echo "HDPMapRedMapTasksMax ${maxMapsPerNode}" >> $outputFile
echo "HDPMapRedReduceTasksMax ${maxRedsPerNode}" >> $outputFile

rm -f $PWD/tmpCpu.out $PWD/tmpMem.out

checkAlpha ()
{
  num=$1
  expr $num + 1 2> /dev/null
  if [ $? = 0 ]; then
    echo -n ""
  else
    echo "ERROR: Value was non-numeric"
    exit 1;
  fi

  if [ $num -lt 0 ]; then
    echo "ERROR: Invalid value less than 0"
    exit 1;
  fi
}

checkConfig ()
{
  echo "Checking Namenode heap size"
  checkAlpha $nameNodeHeapSizeSuggest

  echo "Checking Namenode heap size"
  checkAlpha $jobTrackerHeapSizeSuggest

  echo "Checking Namenode heap size"
  checkAlpha $hbmHeapSizeSuggest

  echo "Checking Namenode heap size"
  checkAlpha $dataNodeHeapSizeSuggest

  echo "Checking Namenode heap size"
  checkAlpha $hadoopHeapSizeSuggest

  if [ $HDPNameNodeHeapSize -gt $nameNodeHeapSizeSuggest ]; then
    exit 1;
    echo ERROR: Insufficient heap size for Name Node.
  fi

  if [ $HDPJobTrackerHeapSize -gt $jobTrackerHeapSizeSuggest ]; then
    exit 1;
    echo ERROR: Insufficient heap size for JobTracker.
  fi

  if [ $HDPHBaseMasterHeapSize -gt $hbmHeapSizeSuggest ]; then
    exit 1;
    echo ERROR: Insufficient heap size for HBase Master.
  fi

  if [ $HDPDataNodeHeapSize -gt $dataNodeHeapSizeSuggest ]; then
    exit 1;
    echo ERROR: Insufficient heap size for Data Node.
  fi

  ## May not be checked. If the user does not want to use the 
  ## above conservative method, this can be masked
  if [ $HDPHadoopHeapSize -gt $hadoopHeapSizeSuggest ]; then
    exit 1;
    echo ERROR: Insufficient heap size for Hadoop Heap.
  fi

#  if [ $HDPMapRedChildJavaOptsSize -gt $childJavaOptsSize ]; then
#    echo ERROR: Insufficient heap size for Child Java Opts.
#  fi
}

if [ $check -eq 1 ]; then
  checkConfig
fi
