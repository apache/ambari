module Puppet::Parser::Functions
  newfunction(:hdp_user, :type => :rvalue) do |args|
    args = [args].flatten
    user = args[0]
    val = lookupvar("::hdp::params::#{user}")
    (val.nil? or val == :undefined) ? "" : val 
  end
end
