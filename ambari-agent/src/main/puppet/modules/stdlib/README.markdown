# Puppet Labs Standard Library #

This module provides a "standard library" of resources for developing Puppet
Modules.  This modules will include the following additions to Puppet

 * Stages
 * Facts
 * Functions
 * Defined resource types
 * Types
 * Providers

This module is officially curated and provided by Puppet Labs.  The modules
Puppet Labs writes and distributes will make heavy use of this standard
library.

# Compatibility #

This module is designed to work with Puppet version 2.6 and later.  It may be
forked if Puppet 2.7 specific features are added.  There are currently no plans
for a Puppet 0.25 standard library module.

# Overview #

TBA

# Contact Information #

  Jeff McCune <jeff@puppetlabs.com>

# Functions #
## validate\_hash ##

    $somehash = { 'one' => 'two' }
    validate\_hash($somehash)

## getvar() ##

This function aims to look up variables in user-defined namespaces within
puppet.  Note, if the namespace is a class, it should already be evaluated
before the function is used.

    $namespace = 'site::data'
    include "${namespace}"
    $myvar = getvar("${namespace}::myvar")
