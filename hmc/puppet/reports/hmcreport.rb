require 'puppet'

Puppet::Reports.register_report(:hmcreport) do
  desc <<-DESC
    Send summary report information to the report directory." 
  DESC

  def process
    client = self.configuration_version
    summary = self.to_yaml
    dir = File.join(Puppet[:reportdir], client)
    if ! FileTest.exists?(dir)
      FileUtils.mkdir_p(dir)
      FileUtils.chmod_R(0750, dir)
    end
    file = self.host
    destination = File.join(dir, file)
    begin
      File.open(destination, "w", 0640) do |f|
        f.print summary
      end
    rescue => detail
      puts detail.backtrace if Puppet[:trace]
      Puppet.warning "Could not write report for #{self.host} at #{destination}: #{detail}"
    end

    # Only testing cares about the return value
    file

  end 
end
