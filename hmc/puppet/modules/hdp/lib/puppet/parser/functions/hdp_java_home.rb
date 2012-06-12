module Puppet::Parser::Functions
  newfunction(:hdp_java_home, :type => :rvalue) do 
    size = lookupvar("size")
    if size.nil? or size == :undefined
      size = "64"
    end
    lookupvar("::hdp::params::java#{size.to_s}_home")
  end
end
