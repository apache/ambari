class hdp-hadoop::smoketest(
  $opts={}
)
{
  #TODO: put in wait
  #TODO: look for better way to compute outname
  $date_format = '"%M%d%y"'
  $outname = inline_template("<%=  `date +${date_format}`.chomp %>")

  #TODO: hardwired to run on namenode and to use user hdfs

  $put = "dfs -put /etc/passwd passwd-${outname}"
  $exec = "jar /usr/share/hadoop/hadoop-examples-*.jar wordcount passwd-${outname} ${outname}.out"
  $result = "fs -test -e ${outname}.out /dev/null 2>&1"
  anchor{ "hdp-hadoop::smoketest::begin" :} ->
  hdp-hadoop::exec-hadoop{ $put:
    command => $put
  } ->
  hdp-hadoop::exec-hadoop{ $exec:
    command =>  $exec
  } ->
  hdp-hadoop::exec-hadoop{ $result:
    command =>  $result
  } ->
  anchor{ "hdp-hadoop::smoketest::end" :}
}
