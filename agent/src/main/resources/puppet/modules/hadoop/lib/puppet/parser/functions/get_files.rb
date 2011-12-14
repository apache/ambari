module Puppet::Parser::Functions
  newfunction(:get_files, :type => :rvalue) do |args|
    hadoop_conf_dir = args[0]
    hadoop_stack_conf = args[1]
    role_name = args[2]
    files = Array.new
    hadoop_stack_conf[role_name].keys.each {|fname| files << ""+hadoop_conf_dir+"/"+fname}
    return files
  end
end
