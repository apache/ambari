module Puppet::Parser::Functions
  newfunction(:hdp_option_value, :type => :rvalue) do |args|
    args = [args].flatten
    opts = args[0]
    key = args[1]
    if opts.kind_of?(Hash) and not function_hdp_is_empty(key)
      opts[key]||:undef
    else
      :undef
    end
  end
end
