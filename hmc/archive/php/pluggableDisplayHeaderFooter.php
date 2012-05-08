<?php

function get_header() {
  $header = <<<HEADER
    <img src="./logo.jpg"/>
    <section id="headerText">Hortonworks Data Platform</section>
HEADER;

  return $header;
}

function get_footer() {
  return "";
}

function render_header() {
  print "<header>\n".get_header()."\n</header>\n";
}

function render_footer() {
  print "<footer>\n".get_footer()."\n</footer>\n";
}

?>
