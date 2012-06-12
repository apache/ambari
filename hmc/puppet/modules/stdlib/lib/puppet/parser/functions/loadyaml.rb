module Puppet::Parser::Functions

  newfunction(:loadyaml, :type => :rvalue, :doc => <<-'ENDHEREDOC') do |args|
    Load a YAML file and return the data if it contains an Array, String, or Hash
    as a Puppet variable.

    For example:

      $myhash = loadyaml('/etc/puppet/data/myhash.yaml')
    ENDHEREDOC

    unless args.length == 1
      raise Puppet::ParseError, ("loadyaml(): wrong number of arguments (#{args.length}; must be 1)")
    end

    YAML.load_file(args[0])

  end

end
