maxNodes=0
maxNodesFile=$(wc -l $easyInstallInput)

for words in $maxNodesFile
do
  maxNodes=$words
  break
done

nodes=$(cat $easyInstallInput)

cmd=
cmdOut=
maxMem=
maxCPU=
node=

execSshCmd() {
  echo "Executing cmd: "
  [ "$sshkey" == "" ] && echo "ssh $HDPClusterDeployUser@$node $cmd" && cmdOut=`ssh -o StrictHostKeyChecking=no $HDPClusterDeployUser@$node $cmd`
  [ "$sshkey" != "" ] && echo "ssh -o StrictHostKeyChecking=no -i $sshkey $HDPClusterDeployUser@$node $cmd" \
&& cmdOut=`ssh -o StrictHostKeyChecking=no -i $sshkey $HDPClusterDeployUser@$node $cmd` 
}

echo "SSHKEY is : " $sshkey
echo "OutputFile: " $easyInstallOut

for node in $nodes
do
  echo "Node: $node"
  cmd='free -mt | tail -1'
  execSshCmd
  echo "$node: run free"
  memFree=`echo $cmdOut | awk '{ print $NF }'`

  cmd='grep -c processor /proc/cpuinfo'
  execSshCmd
  echo "$node: find proc count"
  cpuCount=$cmdOut

  cmd='uname -m'
  execSshCmd
  echo "$node: find processor arch"
  osType=$cmdOut

  cmd='df -klh | grep -v Filesystem'
  execSshCmd
  echo "$node: find disks"
  MountPoints=$cmdOut
  echo "xxx"

  echo $node $memFree $cpuCount $osType >> $easyInstallOut
  echo "yyy"
done

