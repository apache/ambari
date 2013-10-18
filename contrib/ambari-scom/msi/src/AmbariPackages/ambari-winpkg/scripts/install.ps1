### Licensed to the Apache Software Foundation (ASF) under one or more
### contributor license agreements.  See the NOTICE file distributed with
### this work for additional information regarding copyright ownership.
### The ASF licenses this file to You under the Apache License, Version 2.0
### (the "License"); you may not use this file except in compliance with
### the License.  You may obtain a copy of the License at
###
###     http://www.apache.org/licenses/LICENSE-2.0
###
### Unless required by applicable law or agreed to in writing, software
### distributed under the License is distributed on an "AS IS" BASIS,
### WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
### See the License for the specific language governing permissions and
### limitations under the License.

param($skipCredsFile = $true)
function Split_Hosts ($var,$hosts)
{
	Write-Log "Started split of $var"
	if ($var -like "*,*")
	{
		$split_hosts = $var -split ","
		foreach ($server in $split_hosts)
		{
			$hosts.Value += $server
		}
	}
	else
	{ 
		$hosts.Value += $var
	}

}
function CreateURL ($url, $name)
{
	$objShell = New-Object -ComObject ("WScript.Shell")
	$objShortCut = $objShell.CreateShortcut($env:USERPROFILE + "\Desktop\" + $name)
	$objShortCut.TargetPath = $url.Replace("'","")
	$objShortCut.Save()
}
function Invoke-Winpkg ($arguments)
{
    $command = "$ENV:WINPKG_BIN\winpkg.ps1 $arguments"
    Write-Log "Invoke-Winpkg: $command"
    $out = powershell.exe -InputFormat none -Command "$command" 2>&1
    if( $lastexitcode -ne 0 )
    {
        Write-Log "Invoke-Winpkg Failed to execute: $command"
        throw $out
    }
    Write-Log "Invoke-Winpkg Complete: $command"
}

function modify_xml($name)
{
	$xml_file= Join-Path $hdp_home "bin\$name.xml"
	if (Test-Path $xml_file)
	{
		[xml]$xml = Get-Content $xml_file
		$arguments = ($xml.service.arguments).split(" ")
		for ($i = 0; $i -lt $arguments.length; $i++){
			if ($arguments[$i] -clike "-classpath")
			{
				$flag = $true
			}
			if (($arguments[$i] -cnotlike "-classpath") -and $flag)
			{
				$txt = $arguments[$i]
				$arguments[$i] = "$txt;$destination\$ambari_metrics;$destination\sqljdbc4.jar"
				$flag = $false
			}
		}
		$arguments = $arguments -join " "
		$xml.service.arguments = $arguments
		$xml.Save($xml_file)
	}
}
function GetName($name,$param)
{
	if ($param -ieq "full")
	{
		$name = Get-Item $name
		return $name.FullName
	}
	elseif ($param -ieq "short")
	{
		$name = Get-Item $name
		return $name.Name
	}
}
function PushInstall-Files( $node,$source_path,$target_path)
{
	# tag output with node name:
    write-log ("" + $node + ":")
	$current_host = gc env:computername
	$all_files = "*"
	if (-not (($node -ieq $current_host) -and ($source_path -ieq $target_path)))  {
		# convert $target_path into the corresponding admin share path, so we can
		# push the files to the node
		$tgtDir = '\\' + ($node) + '\' +  $target_path.ToString().Replace(':', '$')

		# attempt to create the target install directory on the remote node,
		# if it doesn't already exist
		if(! (Test-Path "$tgtDir")) { $r = mkdir "$tgtDir" }

		# validate that the $tgtDir admin share exists and is accessible on the remote node
		if (! (Test-Path "$tgtDir") ) {
			write-error "$target_path on $node is not accessible by admin share.  Skipping."
			return
		}
		# push the files to each node.  Skip node if any errors.
		pushd "$source_path"
		cp $all_files "$tgtDir" -ErrorAction Stop
		popd
		if (! $?) {
			write-error "Some files could not be pushed to $node.  Skipping."
			return
		}
	}
}
function Main( $scriptDir )
{
	Write-Log "INSTALLATION started"
    Write-Log "Reading Ambari layout from $ENV:AMB_LAYOUT"
    if ( -not (Test-Path $ENV:AMB_LAYOUT))
    {
        throw "No Ambari Properties file found, make sure the file $ENV:AMB_LAYOUT exists and contains the current cluster layout"
    }
    Export-ClusterLayoutIntoEnv $ENV:AMB_LAYOUT "amb"
	Export-ClusterLayoutIntoEnv $ENV:HDP_LAYOUT "hdp"
	$destination= $env:AMB_DATA_DIR
	[Environment]::SetEnvironmentVariable("AMB_DATA_DIR", $destination, "Machine")
	if (!(test-path $destination)) {New-Item -path $destination -ItemType directory}
	$path = [Environment]::CurrentDirectory
	$Package_trim =  Split-Path -Path $path -Parent
	Write-Log "Copying Ambari-Scom.Jar"
	$jar = Join-Path $Package_trim "resources\ambari-scom-*.jar"
	Copy-Item -Force $jar $destination
	$ambari_scom = Getname $jar "short"
	Write-Log "Copying Ambari Metrics Jar"
	$jar = Join-Path $Package_trim "resources\metrics-sink-*.jar"
	Copy-Item -Force $jar $destination
	$ambari_metrics = Getname $jar "short"
	Write-Log "Copying SQL JDBC driver"
	$jar =  $env:SQL_JDBC_PATH 
	Copy-Item -Force $jar $destination
	Write-log "Copuing SQL query"
	$jar = Join-Path $Package_trim "resources\Hadoop-Metrics-SQLServer-CREATE.ddl"
	Copy-Item -Force $jar $destination
	Write-log "Pushing Ambari-SCOM and SQL Server JDBC to each node"
	$current = @()
	$SQL_SERVER_NAME = $env:SQL_SERVER_NAME
	if (($SQL_SERVER_NAME  -ieq "localhost") -or ($SQL_SERVER_NAME  -ieq "127.0.0.1"))
	{
		$SQL_SERVER_NAME= $env:COMPUTERNAME
	}
	$SQL_SERVER_PORT = $env:SQL_SERVER_PORT
	$SQL_SERVER_LOGIN = $env:SQL_SERVER_LOGIN
	$SQL_SERVER_PASSWORD= $env:SQL_SERVER_PASSWORD
	$START_SERVICES = $ENV:START_SERVICES
	Write-log "Start services flag is $START_SERVICES"
	$hosts= @($SQL_SERVER_NAME,$ENV:NAMENODE_HOST,$ENV:SECONDARY_NAMENODE_HOST,$ENV:JOBTRACKER_HOST,$ENV:HIVE_SERVER_HOST,$ENV:OOZIE_SERVER_HOST,
	$ENV:WEBHCAT_HOST,$ENV:HBASE_MASTER)
	Split_Hosts $ENV:SLAVE_HOSTS ([REF]$hosts)
	Split_Hosts $ENV:ZOOKEEPER_HOSTS ([REF]$hosts)
	Split_Hosts $ENV:FLUME_HOSTS ([REF]$hosts)
	Write-Log "Hosts list:"
	Write-log $hosts
	Write-Log "Intalling data sink on each host"
	foreach ($server in $hosts)
	{
		if (($current -notcontains $server) -and ($server -ne $null))
		{
			Write-Log "Pushing files to each node"
			PushInstall-Files $server $destination $destination
			Write-Log "Executing data sink installation"
			$out = Invoke-Command -ComputerName $server -ScriptBlock {
				param( $server,$SQL_SERVER_NAME,$SQL_SERVER_PORT,$SQL_SERVER_LOGIN,$SQL_SERVER_PASSWORD,$destination,$ambari_metrics,$START_SERVICES )
				$log = Join-Path $destination "ambari_install.log"
				function Invoke-Cmd ($command)
				{
					Out-File -FilePath $log -InputObject "$command" -Append -Encoding "UTF8"
				    Write-Host $command
				    $out = cmd.exe /C "$command" 2>&1
 					$out | ForEach-Object { Write-Host "CMD" $_ }
				    return $out
				}
				function modify_xml($name)
				{
					$xml_file= Join-Path $hdp_home "bin\$name.xml"
					if (Test-Path $xml_file)
					{
						Out-File -FilePath $log -InputObject "Modifying $name.xml" -Append -Encoding "UTF8"
						Write-Host "Modifying $name.xml"
						[xml]$xml = Get-Content $xml_file
						$arguments = ($xml.service.arguments).split(" ")
						for ($i = 0; $i -lt $arguments.length; $i++){
							if ($arguments[$i] -like "-classpath")
							{
								$flag = $true
							}
							if (($arguments[$i] -notlike "-classpath") -and $flag)
							{
								$txt = $arguments[$i]
								$arguments[$i] = "$txt;$destination\$ambari_metrics;$destination\sqljdbc4.jar"
								$flag = $false
							}
						}
						$arguments = $arguments -join " "
						$xml.service.arguments = $arguments
						$xml.Save($xml_file)
					}
				}
				function modify_value ($name)
				{
					$value = "$name.sink.sql.databaseUrl=jdbc:sqlserver://$SQL_SERVER_NAME':$SQL_SERVER_PORT;databaseName=HadoopMetrics;user=$SQL_SERVER_LOGIN;password=$SQL_SERVER_PASSWORD"
					Add-Content $metrics $value.Replace("'","")
				}
				if ($server -ieq $SQL_SERVER_NAME)
				{
					Out-File -FilePath $log -InputObject "Creating MonitoringDatabase environment" -Append -Encoding "UTF8"
					Write-HOST "Creating MonitoringDatabase environment"
					$sql_path = Join-Path $destination "\Hadoop-Metrics-SQLServer-CREATE.ddl"
					$cmd ="sqlcmd -s $SQL_SERVER_NAME -i $sql_path"
					invoke-cmd $cmd 
				}
				$hdp_home = [Environment]::GetEnvironmentVariable("HADOOP_HOME","Machine")
				if ($hdp_home -ne $null)
				{
					Out-File -FilePath $log -InputObject "Installing data sink on $server" -Append -Encoding "UTF8"
					Write-Host "Installing data sink on $server"
					$metrics = Join-Path $hdp_home "bin\hadoop-metrics2.properties"
					if (-not (test-path $metrics))
					{
						$metrics = Join-Path $hdp_home "conf\hadoop-metrics2.properties"
					}
					Add-Content $metrics "*.sink.sql.class=org.apache.hadoop.metrics2.sink.SqlServerSink"
					$names = @("namenode","datanode","jobtracker","tasktracker","maptask","reducetask")
					foreach ($name in $names)
					{
						modify_value $name
					}
					Out-File -FilePath $log -InputObject "Modifying CLASSPATH" -Append -Encoding "UTF8"
					Write-Host "Modifying CLASSPATH"
					$names = @("namenode","secondarynamenode","datanode","historyserver","jobtracker","tasktracker")
					foreach ($name in $names)
					{
						modify_xml $name
					}
					Write-Host "Start services flag is $START_SERVICES"
					if ($START_SERVICES -like "*yes*")
					{
						Write-Host "Starting services"
						Out-File -FilePath $log -InputObject "Starting HDP services" -Append -Encoding "UTF8"
						$hdp_root= [Environment]::GetEnvironmentVariable("HADOOP_NODE_INSTALL_ROOT","Machine")
						$cmd = Join-Path $hdp_root "start_local_hdp_services.cmd"
						Invoke-Cmd $cmd
					}
				}
				elseif (($hdp_home -eq $null) -and ($server -ieq $SQL_SERVER_NAME))
				{	
					Out-File -FilePath $log -InputObject "Cleaning things on SQL server" -Append -Encoding "UTF8"
					Write-Host "Cleaning things on SQL server"
					$log_new = Join-Path $ENV:TMP "ambari_install.log"
					Copy-Item $log $log_new
					Remove-Item $Destination -force -Recurse
				}
			} -ArgumentList ($server,$SQL_SERVER_NAME,$SQL_SERVER_PORT,$SQL_SERVER_LOGIN,$SQL_SERVER_PASSWORD,$destination,$ambari_metrics,$START_SERVICES)
			if ($out -eq $null)
			{
				Write-Log "Installation on $server failed. Please check host availability"
			}
			else
			{
				Write-Log "Installation on $server finished."
			}
			$current +=$server
		}
	}
	Write-Log "Unpacking ambari-server-conf"
	$package = Join-Path $Package_trim "resources\ambari-scom-*-conf.zip"
	$ambari_conf = GetName $package "short"
	$ambari_conf = $ambari_conf -replace ".zip"
	$package = GetName $package "full"
	$winpkg = Join-Path $Package_trim "resources\winpkg.ps1"
	$command = "$winpkg $Package unzip $destination"
	Invoke-Pschk $command
	Write-Log "Unpacking ambari-server-lib"
	$package = Join-Path $Package_trim "resources\ambari-scom-*-lib.zip"
	$ambari_lib = GetName $package "short"
	$ambari_lib = $ambari_lib -replace ".zip"
	$package = GetName $package "full"
	$command = "$winpkg $Package unzip $destination"
	Invoke-Pschk $command
	Write-Log "Modifiying ambari.properties"
	$props = Join-Path $destination "$ambari_conf\conf\ambari.properties"
	Add-Content $props "scom.sink.db.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver"
	$value = "scom.sink.db.url=jdbc:sqlserver://$env:SQL_SERVER_NAME':$env:SQL_SERVER_PORT;databaseName=HadoopMetrics;user=$env:SQL_SERVER_LOGIN;password=$env:SQL_SERVER_PASSWORD"
	Add-Content $props $value.Replace("'","")
	Write-Log "Copying cluster.properties to ambari config"
	$clp = $ENV:HDP_LAYOUT
	$destination_conf = Join-Path $destination "$ambari_conf\conf\clusterproperties.txt"
	Copy-Item -Force $clp $destination_conf
	$destination = Join-Path $destination "clusterproperties.txt"
	Copy-Item -Force $clp $destination
	Write-Log "Creating shortcut to start Ambari"
	$objShell = New-Object -ComObject ("WScript.Shell")
	$objShortCut = $objShell.CreateShortcut($env:USERPROFILE + "\Desktop" + "\Start Ambari SCOM Server.lnk")
	$classpath = "$env:AMB_DATA_DIR\$ambari_conf\conf\;$env:AMB_DATA_DIR\sqljdbc4.jar;$env:AMB_DATA_DIR\$ambari_scom;$env:AMB_DATA_DIR\$ambari_lib\lib\*"
	$targetpath = "$ENV:JAVA_HOME\bin\java"
	$arguments = "-server -XX:NewRatio=3 -XX:+UseConcMarkSweepGC -XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60  -cp $classpath org.apache.ambari.scom.AmbariServer"
	$objShortCut.TargetPath = $targetpath
	$objShortCut.Arguments = $arguments
	$objShortCut.Description = "Start Ambari"
	$objShortCut.Save()
	CreateUrl "http://$ENV:COMPUTERNAME':8080/api/v1/clusters" "Browse Ambari API.url"
	CreateUrl "http://$ENV:COMPUTERNAME':8080/api/v1/clusters/ambari/services/HDFS/components/NAMENODE" "Browse Ambari Metrics.url"
	Write-Log "Copying ambari properties file"
	Copy-Item $ENV:AMB_LAYOUT "$env:AMB_DATA_DIR\ambariproperties.txt"
	[Environment]::SetEnvironmentVariable("HDP_LAYOUT","","Machine")
	[Environment]::SetEnvironmentVariable("START_SERVICES","","Machine")
	Write-Log "INSTALLATION COMPLETE" 

	
}

try
{
    $scriptDir = Resolve-Path (Split-Path $MyInvocation.MyCommand.Path)
    $utilsModule = Import-Module -Name "$scriptDir\..\resources\Winpkg.Utils.psm1" -ArgumentList ("AMB") -PassThru
    $apiModule = Import-Module -Name "$scriptDir\InstallApi.psm1" -PassThru
	Main $scriptDir
}
catch
{
	Write-Log $_.Exception.Message "Failure" $_
	exit 1
}
finally
{
    if( $apiModule -ne $null )
    {
        Remove-Module $apiModule
    }
    if( $utilsModule -ne $null )
    {
        Remove-Module $utilsModule
    }
}
