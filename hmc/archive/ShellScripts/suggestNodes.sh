property=$1

[ -f $property ] && source $property

sshkey=$HDPClusterDeployUserIdentityFile

PreConfigOutputDir=$2

easyInstallInput=$HDPClusterHostsFile
easyInstallOut=$PreConfigOutputDir/HostDiscovery.out

mkdir -p $PreConfigOutputDir
if [ -f $easyInstallOut ]; then
  rm -f $easyInstallOut
fi

cd `dirname ${0}`;
echo $PWD
source ./discoverNodes.sh 

##### Find fat nodes from the discovered nodes #####
##### Parse the output file from the DiscoverNodes.sh output #####

##### For master nodes, we need to choose 64 bit Fat nodes #####
##### Fat nodes are nodes with the highest memory #####
`cat $easyInstallOut | grep x86_64 | sort -r -k 2 > $PreConfigOutputDir/temp64.out`

maxNodes=0
maxNodesFile=$(wc -l $PreConfigOutputDir/temp64.out)
for words in $maxNodesFile
do
  maxNodes=$words
  break
done

[ $maxNodes == 0 ] 

linenum=0
outfile=

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

writeNodeToFile()
{
  `awk "NR==$linenum{print;exit}" $PreConfigOutputDir/temp64.out > $outfile`
}

namenode=
cmd=
getNameNodeMountPoint()
{
  cmd="df -klh | sed \"1 d\" | grep -vw \"/boot\" | grep -vw \"/dev\/shm\" | grep -vw \"/home\" | grep -vw \/ | awk '{ print \$(NF)}'"
  [ "$sshkey" == "" ] && `ssh -o StrictHostKeyChecking=no $HDPClusterDeployUser@$node $cmd > $outfile`
  [ "$sshkey" != "" ] && `ssh -o StrictHostKeyChecking=no -i $sshkey $HDPClusterDeployUser@$node $cmd > $outfile`
}

getNodeAtLineNum
outfile=$PreConfigOutputDir/NameNodeSuggest.out
writeNodeToFile

namenode=`cat $PreConfigOutputDir/NameNodeSuggest.out | awk '{print $1}'`
outfile=$PreConfigOutputDir/NameNodeMountPointsSuggest.out
getNameNodeMountPoint

getNodeAtLineNum
outfile=$PreConfigOutputDir/SecondaryNameNodeSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/HBaseMasterSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/JobTrackerSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/GatewaySuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/HCatalogServerSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/GangliaCollectorSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/NagiosServerSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/DashboardSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/OozieServerSuggest.out
writeNodeToFile

getNodeAtLineNum
outfile=$PreConfigOutputDir/TempletonNodeSuggest.out
writeNodeToFile

rm -f $PreConfigOutputDir/temp64.out
