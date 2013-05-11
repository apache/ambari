Puppet::Type.newtype(:append_line) do

  desc <<-EOT
  Type that can append a line to a file if it does not already contain it.

  Example:

  append_line { 'sudo_rule':
    path => '/etc/sudoers',
    line => '%admin ALL=(ALL) ALL',
  }

  EOT

  ensurable do
    defaultto :present
    newvalue(:present) do
      provider.create
    end
  end

  newparam(:name, :namevar => true) do
    desc 'arbitrary name used as identity'
  end

  newparam(:line) do
    desc 'The line to be appended to the path.'
  end

  newparam(:path) do
    desc 'File to possibly append a line to.'
    validate do |value|
      unless (Puppet.features.posix? and value =~ /^\//) or (Puppet.features.microsoft_windows? and (value =~ /^.:\// or value =~ /^\/\/[^\/]+\/[^\/]+/))
        raise(Puppet::Error, "File paths must be fully qualified, not '#{value}'")
      end
    end
  end

  validate do
    unless self[:line] and self[:path]
      raise(Puppet::Error, "Both line and path are required attributes")
    end
  end
end
