module Puppet::Parser::Functions
  newfunction(:hdp_no_hosts, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    var = args[0]
    function_hdp_is_empty(function_hdp_host(var))
  end
end
