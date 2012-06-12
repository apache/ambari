module Puppet::Parser::Functions
  newfunction(:hdp_array_from_comma_list, :type => :rvalue) do |args|
    args = [args].flatten
    function_hdp_is_empty(args[0]) ? "" : args[0].split(",") 
  end
end
