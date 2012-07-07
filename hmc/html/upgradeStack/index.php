<?php
/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/
?>
<?php require_once '../_router.php' ?>
<html>
<head>
  <?php require "../_head.php" ?>
  <script src="/hmc/js/ext/jquery.min.js"></script>
</head>
<body class="yui3-skin-sam">
  <?php require "../_topnav.php"; ?>
  <?php require "../_utils.php"; ?>
  <div id="content">
      <div class="alert alert-info" style="margin-top:40px;padding:20px">
        <h2><?php echo $RES['upgradeStack.index.pageSummary.header'] ?></h2>
        <p><?php echo $RES['upgradeStack.index.pageSummary.body'] ?></p>
        <p id="versionInfo" style="font-weight:bold"></p>
        <div id="buttonGroup" style="margin-top:20px">
          <a href="uninstall.php" class="btn btn-large btn-success" id="upgradeButton"><?php echo $RES['upgradeStack.index.submit.label'] ?></a>
          <a href="../" class="btn btn-large" id="cancelButton" style="margin-left:10px"><?php echo $RES['upgradeStack.index.cancel.label'] ?></a>
        </div>
      </div>
    </div>
  </div>
  <?php require "../_footer.php"; ?>
  <script type="text/javascript">

    var clusterName = '<?php echo $clusterName; ?>';

    var jsFilesToLoad = [
      'js/utils.js',
      'js/upgradeStack/index.js'
    ];
  </script>

  <?php require "../_bootstrapJs.php"; ?>
</body>
</html> 
