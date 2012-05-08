property=$1
OutputDir=$2

[ -f $property ] && source $property

`echo $HDPNameNodeHost > $OutputDir/namenode`
`echo $HDPSecondaryNameNodeHost > $OutputDir/snamenode`
`echo $HDPJobTrackerHost > $OutputDir/jobtracker`
`echo $HDPHBaseMasterHost > $OutputDir/hbasemaster`
`echo $HDPHCatalogServerHost > $OutputDir/hcatserver`
`echo $HDPGangliaCollectorHost > $OutputDir/gangliaserver`
`echo $HDPNagiosServerHost > $OutputDir/nagiosserver`
`echo $HDPOozieServerHost > $OutputDir/oozieserver`
`echo $HDPTempletonNodeHost > $OutputDir/templetonnode`

## dashboardhost same as nagios.
`echo $HDPNagiosServerHost > $OutputDir/dashboardhost`

## slaves are present in the file referred by the nodes env variable
## gateway = any slave.
`cat $HDPClusterHostsFile | grep -vwE "$HDPNameNodeHost|$HDPSecondaryNameNode|$HDPJobTrackerHost|$HDPHBaseMasterHost|$HDPHCatServerHost|$HDPGangliaCollectorHost|$HDPNagiosServerHost|$HDPOozieServerHost|$HDPTempletonHost" | sort | uniq > $OutputDir/nodes`

maxNodes=
getLineCountofNodes()
{
  lineCount=`wc -l $OutputDir/nodes`
  for words in $lineCount
  do
    maxNodes=$words
    break
  done
}

getLineCountofNodes
[ $maxNodes == 0 ] `cat $HDPClusterHostsFile > $OutputDir/nodes`
getLineCountofNodes

linenum=0
getNodeAtLineNum()
{
  linenum=$((linenum+1))  
  if [ "$linenum" -gt "$maxNodes" ]; then
    if [ "$maxNodes" -gt 0 ]; then
      linenum=1
    else
      linenum=$maxNodes
    fi
  fi
}

outFile=
writeNodeToFile()
{
  `awk "NR==$linenum{print;exit}" $OutputDir/nodes > $outFile`
}

appendNodeToFile()
{
  `awk "NR==$linenum{print;exit}" $OutputDir/nodes >> $outFile`
}

## Gateway host out
outFile=$OutputDir/gateway
if [ "$gwhost" != "" ]; then
  `echo $gwhost > $outFile`
else
  getNodeAtLineNum
  writeNodeToFile
fi

## Zookeeper
## zknodes = 3 or lesser if lesser slaves.
outFile=$OutputDir/zknodes
if [ $maxNodes -gt 3 ]; then
  getNodeAtLineNum
  writeNodeToFile
  getNodeAtLineNum
  appendNodeToFile
  getNodeAtLineNum
  appendNodeToFile
else
  `cat $OutputDir/nodes > $outFile`
fi


## HBase slaves are all slave nodes
`cat $OutputDir/nodes > $OutputDir/hbasenodes`

getNameNodeMountPoint()
{
  cmd="df -klh | sed \"1 d\" | grep -vw \"/boot\" | grep -vw \"/dev\/shm\" | grep -vw \"/home\" | grep -vw \/ | awk '{ print \$(NF)}'"
  [ "$HDPClusterDeployUserIdentityFile" == "" ] && `ssh -o StrictHostKeyChecking=no $HDPClusterDeployUser@$HDPNameNodeHost $cmd > $1`
  [ "$HDPClusterDeployUserIdentityFile" != "" ] && `ssh -o StrictHostKeyChecking=no -i $HDPClusterDeployUserIdentityFile $HDPClusterDeployUser@$HDPNameNodeHost $cmd > $1`
}

outfile=$OutputDir/NameNodeMountPointsSuggest.out
getNameNodeMountPoint $outfile

