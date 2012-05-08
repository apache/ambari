class hdp-oozie::download-ext-zip()
{
  include hdp-oozie::params

  $url = $hdp-oozie::params::ext_zip_url
  $zip_name = $hdp-oozie::params::ext_zip_name
  $target = "${hdp::params::artifact_dir}/${zip_name}"
  
  exec{ "curl ${url}":
    command => "mkdir -p ${artifact_dir} ; curl -f --retry 10 ${url} -o ${target} ",
    creates => $target,
    path    => ["/bin","/usr/bin/"]
  }
}
