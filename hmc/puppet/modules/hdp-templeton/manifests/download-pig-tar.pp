class hdp-templeton::download-pig-tar()
{
  include hdp-templeton::params

  $url = $hdp-templeton::params::pig_tar_url
  $tar_name = $hdp-templeton::params::pig_tar_name
  $target = "${hdp::params::artifact_dir}/${tar_name}"
  
  exec{ "curl ${url}":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10  ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
}
