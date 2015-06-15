#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Stop on all errors
$ErrorActionPreference = 'Stop';

Function Gzip-File{
   Param(
        $inFile,
        $outFile = ($inFile + ".gz"),
        $force = $false
        )
  if(-not (Test-Path $inFile)) {
    Write-Host "$inFile does not exist"
    return $false
  }
  if((Test-Path $outFile)) {
    if(-not $force) {
      Write-Host "$outFile already exists"
      return $true
    } else {
      Remove-Item $outFile
    }
  }
  $inputStream = New-Object System.IO.FileStream $inFile, ([IO.FileMode]::Open), ([IO.FileAccess]::Read), ([IO.FileShare]::Read)
  $outputStream = New-Object System.IO.FileStream $outFile, ([IO.FileMode]::Create), ([IO.FileAccess]::Write), ([IO.FileShare]::None)
  $gzipStream = New-Object System.IO.Compression.GzipStream $outputStream, ([IO.Compression.CompressionMode]::Compress)

  $buffer = New-Object byte[](1024)
  while($true){
    $read = $inputStream.Read($buffer, 0, 1024)
    if ($read -le 0){break}
    $gzipStream.Write($buffer, 0, $read)
  }
  $gzipStream.Close()
  $outputStream.Close()
  $inputStream.Close()
  Remove-Item $inFile
  return $true
}

$errorFound = $false
$files = @()
$force = $false
ForEach ($arg in $args) {
  if($arg -eq "-f" -or $arg -eq "--force") {
    $force = $true
    continue
  }
  $files += $arg
}

ForEach ($file in $files) {
  $input = $file
  $output = $file + ".gz";
  Write-Host "Running: Gzip-File $input $output $force"
  $success = Gzip-File $input $output $force
  if(-not $success) {
    $errorFound = $true
  }
}

if ($errorFound) {
  throw "Failed to gzip all files!"
}