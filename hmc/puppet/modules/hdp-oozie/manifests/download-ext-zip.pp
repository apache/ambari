class hdp-oozie::download-ext-zip()
{
  include hdp-oozie::params

  $zip_name = $hdp-oozie::params::ext_zip_name
  $target = "${hdp::params::artifact_dir}/${zip_name}"

  anchor { 'hdp-oozie::download-ext-zip::begin':}

   hdp::package { 'extjs-2.2-1' :
     require   => Anchor['hdp-oozie::download-ext-zip::begin']
   }

   hdp::exec { 'mkdir -p ${artifact_dir} ;  cp /tmp/HDP-oozie/${zip_name} ${target}':
       command => "mkdir -p ${artifact_dir} ;  cp /tmp/HDP-oozie/${zip_name} ${target}",
       unless  => "test -f ${target}",
       creates => $target,
       path    => ["/bin","/usr/bin/"]
       require => Hdp::Package['extjs-2.2-1'],
       notify  =>  Anchor['hdp-oozie::download-ext-zip::end'],
   }

   anchor { 'hdp-oozie::download-ext-zip::end':}

}
