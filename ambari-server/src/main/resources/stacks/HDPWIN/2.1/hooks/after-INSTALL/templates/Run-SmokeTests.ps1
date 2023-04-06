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

###
### Script to run smoke tests
### To run smoke tests for all components,
###   runSmokeTests.ps1
### To run smoke tests for specific component,
###   runSmokeTests.ps1 hadoop hive
###

function ExportPropertiesIntoEnv($filepath)
{
    $propfile = Get-Content $filepath
    foreach ($line in $propfile)
    {
        $line=$line.Trim()
        if (($line) -and (-not $line.StartsWith("#")))
        {
            $prop = @($line.split("=", 2))
            [Environment]::SetEnvironmentVariable( $prop[0].Trim(), $prop[1].Trim(), [EnvironmentVariableTarget]::Process )
        }
    }
}

function Invoke-Cmd($command)
{
    cmd.exe /C "SETLOCAL EnableDelayedExpansion & $command & exit !errorlevel!"
}

function Invoke-HadoopCmd($command)
{
    Invoke-Cmd "$ENV:HADOOP_HOME\bin\hadoop $command"
}

function Get-TimeStamp
{
    [math]::floor((Get-Date -UFormat('%s')))
}

function Run-HadoopSmokeTest
{
    $ts=Get-TimeStamp
    Write-Host "Hadoop smoke test - wordcount using hadoop.cmd file"
    Invoke-HadoopCmd "dfs -copyFromLocal $ENV:HADOOP_HOME\bin\hadoop.cmd hadoop-$ts"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Error copying the input file for the Hadoop smoke test"
    }
    $hadoopExamplesJAR = gci "$ENV:HADOOP_COMMON_HOME\share\hadoop\mapreduce\hadoop-mapreduce-examples*.jar"
    Invoke-HadoopCmd "jar $hadoopExamplesJAR wordcount hadoop-$ts out-$ts"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Hadoop Smoke Test: FAILED"
    }
    else
    {
        Write-Host "Hadoop Smoke Test: PASSED"
    }
    [environment]::exit($LastExitCode)
}

function Run-TezSmokeTest
{
    try
    {
        $xml = New-Object System.Xml.XmlDocument
        $xml.PreserveWhitespace = $true
        $xml.Load("$ENV:HIVE_HOME\conf\hive-site.xml")
        $name = $xml.SelectNodes('/configuration/property') | ? { ($_.name -eq "hive.execution.engine") -and ($_.Value -eq "tez") }
        if (-not $name)
        {
            return
        }
    }
    catch
    {
        return
    }
    $ts=Get-TimeStamp
    Write-Host "Tez smoke test - orderedwordcount using hadoop.cmd file"
    Invoke-HadoopCmd "dfs -copyFromLocal $ENV:HADOOP_HOME\bin\hadoop.cmd tez-in-$ts"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Error copying the input file for the Tez smoke test"
    }
    $tezExamplesJAR = gci "$ENV:TEZ_HOME\tez-examples-*.jar"
    Invoke-HadoopCmd "jar $tezExamplesJAR orderedwordcount tez-in-$ts tez-out-$ts"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Tez Smoke Test: FAILED"
    }
    else
    {
        Write-Host "Tez Smoke Test: PASSED"
    }
    [environment]::exit($LastExitCode)
}

function Run-PigSmokeTest
{
    $ts=Get-TimeStamp
    Write-Host "Pig smoke test - wordcount using hadoop.cmd file"
    $pigscript = Join-Path $ENV:TMP "script-$ts.pig"
    Invoke-HadoopCmd "dfs -mkdir -p /user/hadoop/"
    Invoke-HadoopCmd "dfs -copyFromLocal $ENV:HADOOP_HOME\bin\hadoop.cmd hadoop-$ts"
    Add-Content $pigscript "A = load 'hadoop-$ts' using PigStorage(' ');"
    Add-Content $pigscript "B = foreach A generate `$0 as id;"
    Add-Content $pigscript "store B into 'out-${ts}.log';"
    Invoke-Cmd "$ENV:PIG_HOME\bin\pig -l $ENV:TEMP\pig-$ts.log $pigscript"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Pig Smoke Test: FAILED"
    }
    else
    {
        Write-Host "Pig Smoke Test: PASSED"
    }
    [environment]::exit($LastExitCode)
}

function Invoke-HCatCmd($command)
{
    Write-Host "Running hcat command: $command"
    Invoke-Cmd "python $ENV:HCAT_HOME\bin\hcat.py -e `"$command`""
}

function Run-HCatalogSmokeTest
{
    Write-Host "Hcatalog smoke test - show tables, create table, and drop table"
    $hcatcmds = @("show tables", "drop table if exists hcatsmoke", "create table hcatsmoke ( id INT, name string ) stored as rcfile")
    foreach ($cmd in $hcatcmds)
    {
        Invoke-HCatCmd "$cmd"
        if ($LastExitCode -ne 0)
        {
            Write-Error "HCatalog Smoke Test: FAILED"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
    }
    Write-Host "HCatalog Smoke Test: PASSED"
    [environment]::exit($LastExitCode)
}

function Invoke-HiveCmd($command)
{
    Write-Host "Running hive command: $command"
    $filename = [System.IO.Path]::GetTempFileName()
    Add-Content $filename "${command};"
    Invoke-Cmd "$ENV:HIVE_HOME\bin\hive.cmd -f `"$filename`""
    Remove-Item $filename
}

function Run-HiveSmokeTest
{
    Write-Host "Hive smoke test - drop table, create table and describe table"
    $hivecmds = @("drop table if exists hivesmoke", "create external table if not exists hivesmoke ( foo INT, bar STRING)", "describe hivesmoke")
    foreach ($cmd in $hivecmds)
    {
        Invoke-HiveCmd "$cmd"
        if ($LastExitCode -ne 0)
        {
            Write-Error "Hive Smoke Test: FAILED"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
    }
    Write-Host "Hive Smoke Test: PASSED"
    [environment]::exit($LastExitCode)
}

function Run-HiveServer2SmokeTest
{
    Write-Host "HiveServer2 smoke test - drop table, create table and describe table"
    $hivecmds = @("drop table if exists hive2smoke", "create external table if not exists hive2smoke ( foo INT, bar STRING)", "describe hive2smoke")
    foreach ($cmd in $hivecmds)
    {
        Invoke-Cmd "$ENV:HIVE_HOME\bin\beeline.cmd -u `"jdbc:hive2://${ENV:HIVE_SERVER_HOST}:10001/`" -n hadoop -p fakepwd -d org.apache.hive.jdbc.HiveDriver -e `"$cmd;`""
        if ($LastExitCode -ne 0)
        {
            Write-Error "HiveServer2 Smoke Test: FAILED"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
    }
    Write-Host "HiveServer2 Smoke Test: PASSED"
    [environment]::exit($LastExitCode)
}

function Run-SqoopSmokeTest
{
    Write-Host "Sqoop smoke test - version"
    $out = Invoke-Cmd "$ENV:SQOOP_HOME\bin\sqoop.cmd version"
    if (($LastExitCode -eq 0) -and ($out -match 'Sqoop [\d.]'))
    {
        Write-Host "Sqoop Smoke Test: PASSED"
    }
    else
    {
        Write-Error "Sqoop Smoke Test: FAILED"
    }
    [environment]::exit($LastExitCode)
}

function Run-SliderSmokeTest
{
    Write-Host "Slider smoke test - version"
    $out = Invoke-Cmd "$ENV:SLIDER_HOME\bin\slider.py version"
    if ($LastExitCode -eq 0)
    {
        $out = Invoke-Cmd "$ENV:SLIDER_HOME\bin\slider.py list"
        if ($LastExitCode -eq 0)
        {
            Write-Host "Slider Smoke Test: PASSED"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
    }
    Write-Error "Slider Smoke Test: FAILED"
    [environment]::exit($LastExitCode)
}

function Run-OozieSmokeTest
{
    Write-Host "Oozie smoke test - status"
    $out = Invoke-Cmd "$ENV:OOZIE_HOME\bin\oozie.cmd admin -oozie http://${ENV:OOZIE_SERVER_HOST}:11000/oozie -status"
    if (($LastExitCode -eq 0) -and ($out -match 'System mode: NORMAL'))
    {
        Write-Host "Oozie Smoke Test: PASSED"
    }
    else
    {
        Write-Error "Oozie Smoke Test: FAILED"
    }
    [environment]::exit($LastExitCode)
}

function Run-MahoutSmokeTest
{
	$MAHOUT_HOME = "$ENV:MAHOUT_HOME"
	$PATH_TO_MAHOUT_CMD = Get-ChildItem $ENV:HADOOP_NODE_INSTALL_ROOT\mahout-*\bin\mahout.cmd
	if((!$MAHOUT_HOME) -and ($PATH_TO_MAHOUT_CMD))
	{
		$MAHOUT_HOME = "$ENV:HADOOP_NODE_INSTALL_ROOT\mahout-*"
	}
	if($MAHOUT_HOME)
	{
		Write-Host "MAHOUT smoke test"
		if ((Test-Path "$MAHOUT_HOME\bin\mahout.cmd") -and
		(Test-Path "$MAHOUT_HOME\integration\target\mahout-integration-*.jar") -and
		(Test-Path "$MAHOUT_HOME\math\target\mahout-math-*.jar") -and
		(Test-Path "$MAHOUT_HOME\examples\target\mahout-examples-*-job.jar") -and
		(Test-Path "$MAHOUT_HOME\examples\target\mahout-examples-*.jar"))
		{
			Write-Host "Mahout Smoke Test: PASSED"
			[environment]::exit(0)
		}
		else
		{
        Write-Error "Mahout Smoke Test: FAILED"
        [environment]::exit(-1)
		}
	}
}

function Invoke-HttpGetRequest($url)
{
    Write-Host "Calling URL: $url"
    $req = [System.Net.WebRequest]::Create($url)
    try
    {
        $resp = $req.GetResponse()
        $respcode = [int]$resp.StatusCode
        $stream = $resp.GetResponseStream()
        $sreader = New-Object System.IO.StreamReader $stream
        $result = $sreader.ReadToEnd()
        $sreader.Close()
    }
    catch
    {
        $respcode = -1
        $result = ""
    }
    return $respcode, $result
}

function Run-WebHcatSmokeTest
{
    Write-Host "WebHcat smoke test - status, show databases, show tables"
    $ttcmds = @("status", "ddl/database?user.name=hadoop", "ddl/database/default/table?user.name=hadoop")
    foreach ($cmd in $ttcmds)
    {
        $code, $out = Invoke-HttpGetRequest "http://${ENV:WEBHCAT_HOST}:50111/templeton/v1/$cmd"
        Write-Host "Response: $code"
        Write-Host "Data: $out"
        if ($code -ne 200)
        {
            Write-Error "WebHcat Smoke Test: FAILED"
            [environment]::exit(-1)
            return $code
        }
    }
    Write-Host "WebHcat Smoke Test: PASSED"
    [environment]::exit(0)
}

function Run-HBaseSmokeTest
{
    # HBase is an optional component
    if ( (-not (Test-Path ENV:HBASE_MASTER) ) -and (-not (Test-Path ENV:HBASE_CLASSPATH) ) )
    {
        return
    }
    Write-Host "Hbase smoke test - status, disable, drop, create, put and scan table"

    # grant privs to the smoke test user only in secure mode
    # skip for now

    #check master URL
    Write-Host "Checking master web URL"
    $hbaseweburl="http://${ENV:HBASE_MASTER}:60010/master-status"
    $code, $out = Invoke-HttpGetRequest $hbaseweburl
    Write-Host "Response: $code"
    if ($code -ne 200)
    {
        Write-Error "HBase Smoke Test: FAILED Hbase Master url $hbaseweburl not accessible"
        [environment]::exit(-1)
        return $code
    } else
    {
        Write-Host "Hbase Master url $hbaseweburl accessible"
    }

    # run the smoke test
    $hbasecmds = @("status", "disable 'usertable'", "drop 'usertable'", "create 'usertable', 'family'", "put 'usertable', 'row01', 'family:col01', 'value1'", "scan 'usertable'")
    foreach ($cmd in $hbasecmds)
    {
        Write-Host "Running on hbase shell: $cmd"
        $output=Invoke-Cmd "echo $cmd|$ENV:HBASE_HOME\bin\hbase.cmd shell"
        if ($LastExitCode -ne 0)
        {
            Write-Error "HBase Smoke Test: FAILED failed to run command $cmd"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
        if ($cmd -eq "status")
        {
            if ($output -match "status 0 servers*")
            {
                Write-Error "HBase Smoke Test: FAILED no regionservers running"
                [environment]::exit(-1)
                return -1
            }
        } elseif ($cmd.StartsWith("scan"))
        {
            if (-not ($output -match "1 row"))
            {
                Write-Error "HBase Smoke Test: FAILED scan did not return rows"
                [environment]::exit(-1)
                return -1
            }
        }
    }

    #cleanup
    Write-Host "Runnng cleanup for HBase smoke tests"
    $hbasecmds = @("disable 'usertable'", "drop 'usertable'")
    foreach ($cmd in $hbasecmds)
    {
        Write-Host "Running on hbase shell: $cmd"
        $output=Invoke-Cmd "echo $cmd|$ENV:HBASE_HOME\bin\hbase.cmd shell"
        if ($LastExitCode -ne 0)
        {
            Write-Error "HBase Smoke Test: FAILED failed to run command $cmd"
            [environment]::exit($LastExitCode)
            return $LastExitCode
        }
    }
    Write-Host "HBase Smoke Test: PASSED"
    [environment]::exit(0)
}

function Run-ZooKeeperSmokeTest
{
    # ZooKeeper is an optional component
    if (-not (Test-Path ENV:ZOOKEEPER_HOSTS))
    {
        return
    }
    Write-Host "ZooKeeper smoke test - create root znode and verify data consistency across the quorum"

    $zk_node1=""
    foreach ($tmpHost in ${ENV:ZOOKEEPER_HOSTS})
    {
        $zk_node1=$tmpHost
        break
    }

    Write-Host "Delete /zk_smoketest znode if exists"
    $cmd="delete /zk_smoketest"
    $output=Invoke-Cmd "$ENV:ZOOKEEPER_HOME\bin\zkCli.cmd -server ${zk_node1}:2181 $cmd"
    if ($LastExitCode -ne 0)
    {
        Write-Error "ZooKeeper Smoke Test: FAILED failed to run command $cmd"
        [environment]::exit($LastExitCode)
        return $LastExitCode
    }

    Write-Host "Creat /zk_smoketest znode"
    $cmd="create /zk_smoketest smoke_data"
    $output=Invoke-Cmd "$ENV:ZOOKEEPER_HOME\bin\zkCli.cmd -server ${zk_node1}:2181 $cmd"
    if ($LastExitCode -ne 0)
    {
        Write-Error "ZooKeeper Smoke Test: FAILED failed to run command $cmd"
        [environment]::exit($LastExitCode)
        return $LastExitCode
    }

    $zookeeper_exit_code=0
    Write-Host "Verify the data associated with znode across all the nodes in the zookeeper quorum"
    $cmd="get /zk_smoketest"
    foreach ($tmpHost in ${ENV:ZOOKEEPER_HOSTS})
    {
        $output=Invoke-Cmd "$ENV:ZOOKEEPER_HOME\bin\zkCli.cmd -server ${tmpHost}:2181 $cmd"
        if (-not ($output -match "smoke_data"))
        {
            Write-Error "Data associated with znode /zk_smoketests is not consistent on host $tmpHost"
            $zookeeper_exit_code=$zookeeper_exit_code+1
        }
    }

    Write-Host "Response: $zookeeper_exit_code"
    if ($zookeeper_exit_code -ne 0)
    {
        Write-Error "ZooKeeper Smoke Test: FAILED"
        [environment]::exit(-1)
        return $zookeeper_exit_code
    }

    #cleanup
    Write-Host "Runnng cleanup for ZooKeeper smoke tests"
    $cmd="delete /zk_smoketest "
    $output=Invoke-Cmd "$ENV:ZOOKEEPER_HOME\bin\zkCli.cmd -server ${zk_node1}:2181 $cmd"

    Write-Host "ZooKeeper Smoke Test: PASSED"
    [environment]::exit(0)
}

function Run-KnoxSmokeTest
{
    Write-Host "Knox Smoke Test"
    #Knox is an optional component and may not exist on every machine.
    if (-not (Test-Path ENV:KNOX_HOME))
    {
        return
    }

    $url = "https://${ENV:KNOX_HOST}:8443/gateway/default/webhdfs/v1/?op=GETHOMEDIRECTORY"
    $req = [System.Net.WebRequest]::Create( $url )
    # Set the HTTP basic auth credentials to use for the request
    $req.Credentials = New-Object Net.NetworkCredential( "guest", "guest-password" )
    # Disable SSL cerificate validation temporarily
    [Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

    Write-Host "GET $url"
    try
    {
        $resp = $req.GetResponse()
        $status = [int]$resp.StatusCode
        $stream = $resp.GetResponseStream()
        $reader = New-Object System.IO.StreamReader $stream
        $entity = $reader.ReadToEnd()
        $reader.Close()
    }
    catch [System.Net.WebException]
    {
        $entity = $_.Exception
        $resp = $_.Exception.Response
        $status = [int]$resp.StatusCode
    }

    # Restore previous server certificate validator callback.
    [Net.ServicePointManager]::ServerCertificateValidationCallback = $null

    Write-Host "Status: $status"
    if ( $status -eq 200 )
    {
        Write-Host "Result: $entity"
        Write-Host "Knox Smoke Test: PASSED"
    }
    elseif ( $status -eq 500 )
    {
        Write-Host "Result: WebHDFS unavailable"
        Write-Host "Knox Smoke Test: PASSED"
    }
    else
    {
        Write-Host "Result: $entity"
        Write-Error "Knox Smoke Test: FAILED"
        [environment]::exit(-1)
        return -1
    }
}

function Run-PhoenixSmokeTest
{
    # Phoenix is an optional component
    if (-not (Test-Path ENV:PHOENIX_HOME))
    {
        return
    }
    # Phoenix depends on hbase setup
    if (-not (Test-Path ENV:HBASE_CONF_DIR))
    {
        return
    }

    $zookeeper = 'localhost:2181'
    $output=Invoke-Cmd "$ENV:HBASE_HOME\bin\hbase.cmd org.apache.hadoop.hbase.util.HBaseConfTool hbase.zookeeper.quorum"
    if ([string]::Compare($output,"null", $True) -ne 0)
    {
        $zookeeper=[regex]::replace($output,":\d+", '')
        $output=Invoke-Cmd "$ENV:HBASE_HOME\bin\hbase.cmd org.apache.hadoop.hbase.util.HBaseConfTool hbase.zookeeper.property.clientPort"
        $zookeeper = $zookeeper +":" + $output
    }
    $output=Invoke-Cmd "$ENV:HBASE_HOME\bin\hbase.cmd org.apache.hadoop.hbase.util.HBaseConfTool zookeeper.znode.parent"
    if ([string]::Compare($output,"null", $True) -ne 0)
    {
        $zookeeper = $zookeeper +":" + $output
    }
    $ENV:PHOENIX_LIB_DIR = $ENV:PHOENIX_HOME

    Write-Host "ZooKeeper connection string: $zookeeper"
    Write-Host "Phoenix smoke test - create WEB_STAT table and load data into it"

    $phoenixClientJAR = gci "$ENV:PHOENIX_HOME\phoenix-*client.jar"
    $output=Invoke-Cmd "$ENV:JAVA_HOME\bin\java -cp $phoenixClientJAR org.apache.phoenix.util.PhoenixRuntime $zookeeper $ENV:PHOENIX_HOME\examples\WEB_STAT.sql $ENV:PHOENIX_HOME\examples\WEB_STAT.csv $ENV:PHOENIX_HOME\examples\WEB_STAT_QUERIES.sql"
    if (-not ($output -match "39 rows upserted"))
    {
        Write-Error "Phoenix Smoke Test Failed To Load Data Into WEB_STAT sample table"
        [environment]::exit(-1)
        return -1
    }

    Write-Host "Phoenix Smoke Test: PASSED"
    [environment]::exit(0)
}

function Run-FalconSmokeTest
{
    if (-not (Test-Path ENV:FALCON_HOME))
    {
        return
    }
    Write-Host "Falcon smoke test - get admin status"
    $command = "admin -status"
    Write-Host "Running falcon command: $command"
    Invoke-Cmd "python $ENV:FALCON_HOME\bin\falcon.py $command"
    if ($LastExitCode -ne 0)
    {
        Write-Error "Falcon Smoke Test: FAILED"
    }
    else
    {
        Write-Host "Falcon Smoke Test: PASSED"
    }
    [environment]::exit($LastExitCode)
}

function Run-StormSmokeTest
{
    if (-not (Test-Path ENV:STORM_HOME))
    {
        return
    }
    Write-Host "Storm smoke test - get configuration value from the server"
    $command = "remoteconfvalue nimbus.host"
    Write-Host "Running storm command: $command"
    Invoke-Cmd "$ENV:STORM_HOME\bin\storm.cmd $command"
    if ($LastExitCode -ne 0)
    {
        Write-Host "Storm Smoke Test: FAILED"
    }
    else
    {
        Write-Host "Storm Smoke Test: PASSED"
    }
    [environment]::exit($LastExitCode)
}


$scriptDir = Resolve-Path (Split-Path $MyInvocation.MyCommand.Path)
ExportPropertiesIntoEnv (Join-Path $scriptDir "cluster.properties")
$VALID_COMPONENTS = ls function:* | foreach { if ($_.Name -match '^Run-(.*)SmokeTest') { $matches[1] } }

if ($args.Length -eq 0)
{
    $args = $VALID_COMPONENTS
}
foreach ($comp in $args)
{
    if ($VALID_COMPONENTS -contains $comp)
    {
        Invoke-Expression "Run-${comp}SmokeTest"
    }
    else
    {
        Write-Error "No smoke test found for $comp"
    }
}

