<?php require_once "./head.inc" ?>
<html>
  <head>
    <?php require "./head.htmli" ?>
  </head>
  <body class="yui3-skin-sam">
    <?php require "./topnav.htmli"; ?>
    <div id="ContentDivId"> 
      <div class="container">
        <div class="alert alert-important" style="margin-top:40px;padding:20px">
          <h2 style="margin-bottom:10px"><?php echo $RES['installFailed.header'] ?></h2>
          <p><?php echo $RES['installFailed.body'] ?></p>
          <a id="submitLinkId" class="btn btn-large" style="margin-top:20px" href="uninstallWizard.php?clusterName=<?php echo $clusterName ?>"><?php echo $RES['installFailed.submit.label'] ?></a>          
        </div>
      </div>
    </div>
  </body>
</html>