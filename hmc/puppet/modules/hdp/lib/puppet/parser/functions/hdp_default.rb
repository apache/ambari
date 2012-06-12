module Puppet::Parser::Functions
  newfunction(:hdp_default, :type => :rvalue) do |args|
    args = function_hdp_args_as_array(args)
    scoped_var_name = args[0]
    var_name = scoped_var_name.split("/").last
    default = args[1]
    val = lookupvar("::#{var_name}")
    function_hdp_is_empty(val) ? (default||"") : val
  end
end


