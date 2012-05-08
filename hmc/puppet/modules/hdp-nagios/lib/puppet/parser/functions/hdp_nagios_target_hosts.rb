module Puppet::Parser::Functions
  newfunction(:hdp_nagios_target_hosts, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    host_types = args[0]
#TODO: see if needed       Puppet::Parser::Functions.autoloader.loadall
    host_types.map{|t|function_hdp_host(t)}.map{|h|function_hdp_is_empty(h) ? [] : [h].flatten}.flatten
  end
end
