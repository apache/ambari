module Puppet::Parser::Functions
  newfunction(:hdp_unique_id_and_date, :type => :rvalue) do 
    id = lookupvar('::uniqueid')
    date = `date +"%M%d%y"`.chomp
    "id#{id}_date#{date}"
  end
end
