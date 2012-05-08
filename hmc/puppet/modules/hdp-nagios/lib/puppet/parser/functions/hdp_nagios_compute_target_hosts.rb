module Puppet::Parser::Functions
  newfunction(:hdp_nagios_compute_target_hosts, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    monitored_hosts = args[0]
    component_name_mapping = args[1]
    ret = Hash.new
    monitored_hosts.each do |host_info|
      hostname = host_info.keys.first
      cmps = host_info.values.first
      cmps.each do |cmp|
        next unless host_var_info = component_name_mapping[cmp]
        host_var = host_var_info['host_var']
	if host_var_info['type'] == 'array'
          (ret[host_var] ||= Array.new) << hostname
	elsif host_var_info['type'] == 'scalar'
	  ret[host_var] = hostname
        end
      end
    end	
    ret
  end
end
