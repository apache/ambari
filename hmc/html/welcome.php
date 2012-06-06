<?php require_once "./head.inc"; ?>
<html>
  <head>
    <title id="pageTitleId"><?php echo $RES['page.title'] ?></title>

    <!-- CSS -->
    <link type="text/css" rel="stylesheet" href="../yui-3.5.1/build/cssreset/cssreset-min.css">
    <link type="text/css" rel="stylesheet" href="../css/bootstrap.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common2.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/common3.css" media="screen"/>
    <link type="text/css" rel="stylesheet" href="../css/selectHosts.css" media="screen"/>
    <link rel="shortcut icon" href="../images/logo-micro.gif">
    <!-- End CSS -->
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
  </body>
</html>