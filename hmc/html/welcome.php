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
<?php require_once "./head.inc"; ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>

  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>

    <div id="ContentDivId">
      <div class="alert alert-info" style="margin-top:40px;padding:20px">
        <h2 style="margin-bottom:10px"><?php echo $RES['welcome.header'] ?></h2>
        <p><?php echo $RES['welcome.body'] ?></p>
        <p><span class='label label-info'>Note</span><span style='margin-left:10px;'><?php echo $RES['welcome.note'] ?></span></p> 
        <a id="submitLinkId" class='btn btn-large' style='margin-top:20px' href='initializeCluster.php'><?php echo $RES['welcome.submit.label'] ?></a>
      </div>
    </div>
    <?php require "./footer.htmli"; ?>
  </body>
</html>
