module Puppet::Parser::Functions
  newfunction(:hdp_host, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    var = args[0]
    val = lookupvar(var)
    function_hdp_is_empty(val) ? "" : val 
  end
end
