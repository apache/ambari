Puppet::Type.newtype(:anchor) do
  desc <<-'ENDOFDESC'
  A simple resource type intended to be used as an anchor in a composite class.

      class ntp {
        class { 'ntp::package': }
        -> class { 'ntp::config': }
        -> class { 'ntp::service': }

        # These two resources "anchor" the composed classes
        # such that the end user may use "require" and "before"
        # relationships with Class['ntp']
        anchor { 'ntp::begin': }   -> class  { 'ntp::package': }
        class  { 'ntp::service': } -> anchor { 'ntp::end': }
      }

  This resource allows all of the classes in the ntp module to be contained
  within the ntp class from a dependency management point of view.

  This allows the end user of the ntp module to establish require and before
  relationships easily:

      class { 'ntp': } -> class { 'mcollective': }
      class { 'mcollective': } -> class { 'ntp': }

  ENDOFDESC

  newparam :name do
    desc "The name of the anchor resource."
  end

end
