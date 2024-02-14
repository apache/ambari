# 12.0.6

## Bug Fix

- ([708ade3](https://github.com/angular/webdriver-manager/commit/708ade31564ab5a48fbfcff80c37370fdc4f659a))
  fix(responses): response xml and json files (#247)

# 12.0.5

## Bug Fix

- ([242a72f](https://github.com/angular/webdriver-manager/commit/242a72ffc93037d651c9805e09b4fb30318d9f05))
  feat(start): start selenium without making web requests (#232)

# 12.0.4

## Bug Fixes
- ([52d8a23](https://github.com/angular/webdriver-manager/commit/52d8a23f2d5d5021d1d9d302c492bf78a233a79d))
  fix(ignoressl): pass option to both binary and config source

  closes #207.

- ([5af1c1c](https://github.com/angular/webdriver-manager/commit/5af1c1cdfb2d718004b02e9c0325ea6e758e78f1))
  fix(cache): change timestamp to 1 hour instead of 10 hours (#223)

  closes #221

# 12.0.3

## Bug Fixes

- ([bb13882](https://github.com/angular/webdriver-manager/commit/bb13882f1d111fc0c16032be33a7b8dc7b1a797c))
  feat(gecko): Improve error message when Github api limit reached. (#217)

 the user.  Also, API limit is reached, the error message now directly informs
  when any other failure occurs the status code is reported.
   This should hopefully give more info for issue #216.
- ([2cffd30](https://github.com/angular/webdriver-manager/commit/2cffd30d9ef87c5b53433f2aa73eda92b4251a76))
  fix(ignoressl): pass proxy and ignore ssl down to the binary and config source (#208)

  closes #207 and closes #221

# 12.0.2

- ([0bdf6a4](https://github.com/angular/webdriver-manager/commit/0bdf6a465ae2a4b106bb5ff948718ef4ae3f31ad))
  deps(typescript): use typescript@~2.0.0. fix any types (#203)

# 12.0.1

- ([6209666](https://github.com/angular/webdriver-manager/commit/620966611f48504619a594b582060ba04a61b3a7))
  fix(gecko): add additional check for OS when getting latest (#200)

# 12.0.0

### Changes to update

This release gets the latest release for selenium standalone, chromedriver,
iedriver, and gecko driver by downloading and parsing either a json or xml file.
These json or xml files are cached in the selenium directory. This means for
users that provide an alternative cdn will also be required to provide the proper
xml or json server response to find these binaries.

Since we are always downloading the latest, the `config.json` versions will no
longer be a place to override these.

### Changes to start

If a new release is out and you have old binaries, running `webdriver-manager
start` without specifying any versions should throw an error. The error will
tell the user that the binary is not present.

### Changes to status

Since we are downloading the latest and not maintaining a default version in
`config.json`, we are dropping the default tag.

## Features

- ([fe309ef](https://github.com/angular/webdriver-manager/commit/fe309ef0d85081592662164d4a24d79b0f2ed5cf))
  feat(latest): get the latest version from the cdn (#198)

  This reads the xml from the CDN to get the latest chromedriver, iedriver,
  or standalone version if the version is 'latest'. If the release is from
  Github, use the Github API to get the releases. Also store the downloaded
  information to a cache in the output directory (default: selenium/). If
  the file is older than one hour it will be rewritten.

  When getting the status, we are no longer showing the default version.
  Default versions will be deprecated and will be removed from the config.json
  file.

  When starting the standalone server, use the 'latest' version by default
  unless specified by --versions.{binary} flag.

  Change the gulp update task to use 3.0.0-beta4 so Firefox tests will pass.

# 11.1.1

## Bug Fixes

- ([70614a2](https://github.com/angular/webdriver-manager/commit/70614a23e289088c852f5c0162a947488ffc77e0))
  fix(ie): Use 32-bit version by default for IEDriver (#181)

  closes #180
- ([6f9a2ab](https://github.com/angular/webdriver-manager/commit/6f9a2abbf7d16f35e342f963543706ff3e1c45a1))
  fix(gecko): Respect versions.gecko in start command. (#184)

  Also bump the geckodriver version to latest.

## Dependencies

- ([5881c5b](https://github.com/angular/webdriver-manager/commit/5881c5bb49f330abd7804e2605df46901e87bf2a))
  deps(update): update devDependencies (#187)


# 11.1.0

- Update to set the default chrome driver version to 2.26

## Features

- ([72e3d9f](https://github.com/angular/webdriver-manager/commit/72e3d9f341f1d0ba190036a72938e727d83840c7))
  feat(status): show the last downloaded version when using status (#177)

  - added a test to run update, then checks status for labels

  closes #172

## Bug Fixes

- ([a3b46c7](https://github.com/angular/webdriver-manager/commit/a3b46c7a2ae59357b00fe5ce81d36964d6b0d45c))
  fix(iedriver): if downloading x64, use x64 version on start command (#173)

  - clang formatting

  closes #147

# 10.3

- Minor version update so users still on ES5/selenium 2.x can access appium/mobile fixes
  See https://github.com/angular/webdriver-manager/commits/10.3.0 for details


# 11.0.0

## Breaking Change:

- Requires node 6 since node 6 is in long term support. See (node LTS Schedule)[https://github.com/nodejs/LTS#lts-schedule].

## Features
- ([b5638ef](https://github.com/angular/webdriver-manager/commit/b5638ef0861843e1d42220af515adc3e03a2b65a))
  feat(update): on update, write full binary paths to file (#140)

  - Adding back in curl calls, these were removed on the new
   `Downloader.getFile`. Add curl call to reflect proxies.

  - Fix output dir to read from update's options instead of Config

  - Feature will help directConnect users for Protractor. The file
   will keep track of the last binary version as well as all other
   binaries downloaded.

   The file will be created in the output directory. By default this is
   `selenium/update-config.json`. On `clean` this file will be removed.

   ```
   webdriver-manager update --versions.chrome=2.20 --standalone=false
   --gecko=false
   ```

   file created:
   ```
   {
     "chrome": {
       "last": "/opt/src/webdriver-manager/selenium/chromedriver_2.20",
       "all": ["/opt/src/webdriver-manager/selenium/chromedriver_2.20"]
     }
   }
   ```

   then the user wants to use 2.25:

   ```
   webdriver-manager update --versions.chrome=2.25 --standalone=false
   --gecko=false

   ```

   file created:
   ```
   {
     "chrome": {
       "last": "/opt/src/webdriver-manager/selenium/chromedriver_2.25",
       "all": ["/opt/src/webdriver-manager/selenium/chromedriver_2.20",
               "/opt/src/webdriver-manager/selenium/chromedriver_2.25"]
     }
   }
   ```

- ([473ab3e](https://github.com/angular/webdriver-manager/commit/473ab3e40c44468bb79e2a23d7b12753cf6e2b4d))
  feat(android): match android arch to os.arch (#164)

  The default was x86-64, but x86 cannot be emulated on ARM.  This makes more sense
- ([c864c9a](https://github.com/angular/webdriver-manager/commit/c864c9af35514a4b5bf8a1d82b4339b39e5ac574))
  feat(shutdown): do not error if you try to shutdown a server which is already off (#162)

  When scripting, you might want to defensively run a `shutdown` command.  If the shutdown fails
  because the server is already off, you don't care.  If it fails for another reason, you do care.
  So I made trying to shutdown a server which is already off just a warning.  I added a flag in case
  you want the old behavior though.

- ([338fffd](https://github.com/angular/webdriver-manager/commit/338fffddf68ac2767aa5c226ba5374451b9e5308))
  feat(quiet/verbose): add `--quiet` and `--verbose` flags to control the level of output (#156)

  I added the `--quiet` flag for cases like:
 where currently the start --detach; ./tests.sh; webdriver-manager shutdown`
  selenium server output will get mixed in with other output.
   I also added the `--verbose` flag for `webdriver-manager update` in case you *really* wanted to
  see all the output which gets eaten by using `--android-accept-licenses`.

- ([91e36a3](https://github.com/angular/webdriver-manager/commit/91e36a3e56e712af2c104eafc45eeeba5997ad6a))
  feat(android on windows): Support android VMs on windows (#154)

  Closes https://github.com/angular/webdriver-manager/issues/51

- ([d533b03](https://github.com/angular/webdriver-manager/commit/d533b0389ac8a43b815890a644fdb9aa403ec769))
  feat(start android): extend the --detach flag to wait for appium/android (#141)

## Bug fixes

- ([26586f1](https://github.com/angular/webdriver-manager/commit/26586f1b341e02229d73d40827a9c1af2197ebb3))
  fix(start): wait for emulated android to really be ready before signaling (#161)

  Before, we were just waiting for the emulator to be running, rather than waiting for the OS to be
  booted up and ready to instance chrome.
   While I was doing that I moved some stuff into `lib/utils.ts` since I felt like too much of
  `lib/cmds/start.ts` was being devoted to this one feature.
   Also closes https://github.com/angular/webdriver-manager/issues/166
- ([a7c6eb5](https://github.com/angular/webdriver-manager/commit/a7c6eb5d3d1caed2afea1ef896753d53f4ea14ed))
  fix(update/android): 2a1505f broke android
- ([3ee3e1a](https://github.com/angular/webdriver-manager/commit/3ee3e1a328087cb8c5bf869e00a325cfdeb80f6d))
  fix(fs): path.join does not handle absolute paths as desired (#152)
- ([deead0f](https://github.com/angular/webdriver-manager/commit/deead0fc55ecd00b282aedc234592181746a307c))
  fix(downloader): destroy the request after receiving the header (#144)

  Otherwise we’ll won’t terminate until the whole file was downloaded, even though we don’t need it.
- ([c16bf90](https://github.com/angular/webdriver-manager/commit/c16bf9053fc90e4b5e89ab867c514d0622ab0716))
  chore(es6): allow to use es6 promises (#160)

  - with node 6 on LTS, we can update the tsconfig to es6
  - update travis tests to use node 6 and 7

# 10.2.10

- Since 10.2.9 produced breaking changes, released as version 11.0.0
- Version 10.2.8 is the same as 10.2.10 due to [issue #170](https://github.com/angular/webdriver-manager/issues/170).

# 10.2.8

## Features

- ([1f9713a](https://github.com/angular/webdriver-manager/commit/1f9713aff1e7d44de900ed3c74abac532d3e25ff))
  feat(start and shutdown): Added `--detach` option for `start` command and new `shutdown` command
  (#130)

- ([88cf46b](https://github.com/angular/webdriver-manager/commit/88cf46b715250559ba8a726370a83c5c2f4daed1))
  feat(version): have a way to get the package version (#136)

  closes #119

## Bug fixes
- ([5966b6a](https://github.com/angular/webdriver-manager/commit/5966b6ac7329878e9e16f5b1b88261c5b7f7e438))
  fix(cli): fix setting flag to false (#135)

  - This fixes `webdriver-manager update --gecko=false`
  - This does not fix `webdriver-manager update --gecko=0`. Minimist interprets 0 as true.
  - Add options and programs unit tests
   closes #110

- ([35676ee](https://github.com/angular/webdriver-manager/commit/35676ee70c816d43f045fa33d02e41bf502a3a14))
  fix(gecko): follow redirects for content-length (#133)

# 10.2.7

## Features

- ([66776a0](https://github.com/angular/webdriver-manager/commit/66776a0edc97e0b2718f2fdf4eeb2c2c8b40df73))
  feat(start): add way to programmatically detect when the selenium server is running (#120)

## Bug fixes

- ([dc2f9f9](https://github.com/angular/webdriver-manager/commit/dc2f9f99ebd9675b02addf06732a4d8d348046bc))
  fix(cli): fix default option values, boolean and string handling (#110) (#122)

  - default option values initialize properly for `minimist`
  - user-supplied boolean-type option values respected
  - string-type option values are always strings
  - simplify boolean-type option value access

- ([88d6105](https://github.com/angular/webdriver-manager/commit/88d6105f538f075968c152935131bf19bf289532))
  fix(gecko): Update geckodriver to 0.11.0 and fix suffixes. (#128)

  Fixes #111

- ([707e015](https://github.com/angular/webdriver-manager/commit/707e015737ee3ca4b26b6d89979251f8d8c2d11d))
  fix(android): fixed four things for android: (#116)

  - Make appium default to 1.6.0 (Android N didn't work on 1.5.x)
  - Make virtual devices default to `google_apis`
  - Don't delete old virtual devices on update
  - Update documentation

- ([9fe4b22](https://github.com/angular/webdriver-manager/commit/9fe4b226d58fbbce2e9cf49df58f45dee7f13cf2))
  fix typo in webdriver-manager/spec/files (#125)

# 10.2.6

## Features

- ([f892ec4](https://github.com/angular/webdriver-manager/commit/f892ec41c09c210527998c966a69edc081cf418e))
  chore(chromedriver): update chromedriver version to 2.25

# 10.2.5

## Bug Fixes

- ([b103850](https://github.com/angular/webdriver-manager/commit/b1038500466fe790cc8e3c2ff82dc3c7eb3796ba))
  fix(update): fix undefined gecko getBoolean error (#113)

  closes #107
- ([7fbacf5](https://github.com/angular/webdriver-manager/commit/7fbacf5bc902dd3ccd1c9fbf285c8ca9a1e48ee3))
  fix(start): set the port when standalone server starts

  closes #106

# 10.2.4

## Bug Fixes

- ([3984ea4](https://github.com/angular/webdriver-manager/commit/3984ea4e5cfd2edf0401a5e5310aecaaecb63555))
  fix(filemanager): respect proxy/ignoreSSL options in contentLength HEAD request (#101)


- ([946ee00](https://github.com/angular/webdriver-manager/commit/946ee005f7d316fd2d404c4bdbeae9a3802051af))
  fix(chrome_driver): use the x64 binary if chrome driver version is greater than 2.23 (#95)

  * fix(chrome_driver): use the x64 binary if chrome driver version is greater than 2.23
  * fix(chrome_driver): add semver to better determine version number
  * refactor(chrome_driver): check first to see if we have valid semver or not

  closes #93
- ([b183fad](https://github.com/angular/webdriver-manager/commit/b183fadd4ae0b47b0773d6979d090c74419ee327))
  fix(filemanager): Binaries can be downloaded from a custom CDN with alternate_cdn(#97)

  closes #96

## Features

- ([5241fc1](https://github.com/angular/webdriver-manager/commit/5241fc14eaf2b5cdf4b35362f260f6973cea0b1e))
  chore(chromedriver): update chromedriver version to 2.24 (#92)

  Chromedriver < 2.24 has issues with Chrome 54+
  (https://bugs.chromium.org/p/chromedriver/issues/detail?id=1451).

- ([61af7be](https://github.com/angular/webdriver-manager/commit/61af7be4edbaf070bdcc35dc85f11fb46ab9577e))
  feat(gecko): Add geckodriver, related config, and flags

  Users will still need 'marionette': true in their capabilities in order to use gecko driver.

# 10.2.3

## Bug Fixes

- ([fa48354](https://github.com/angular/webdriver-manager/commit/fa4835453385d4c79fcbba7bb6d408557c870bae))
  fix(downloader): fix against working proxy (#87)

- ([d6597e8](https://github.com/angular/webdriver-manager/commit/d6597e8a06004888371cca12b8e803c7d44eaf8d))
  fix(start): add the correct flags for windows (#83)

  closes #68
- ([c96090c](https://github.com/angular/webdriver-manager/commit/c96090c0f7cc24209b34f9634699e68669650070))
  fix(update): download standalone with proxy and ignore ssl (#81)

  closes #79

## Features

- ([7ec082a](https://github.com/angular/webdriver-manager/commit/7ec082a1bcc7f262237a616ec96592c36c28b89a))
  feat(start): add a gecko driver path to the start command (#86)

## Dependencies

- ([fe85c94](https://github.com/angular/webdriver-manager/commit/fe85c94e8db0680be25461cd3ea1ef59fc4d8fa4))
  dep(types): update typescript, remove typings in favor of @types (#84)

# 10.2.2

## Bug Fixes

- ([236a8ec](https://github.com/angular/webdriver-manager/commit/236a8ec901133cb21247fc452d7ef7c9d5fed172))
  fix(downloader): increase timeouts and unlink sync on download errors (#75)

  closes #62 and #63
- ([fa20ca8](https://github.com/angular/webdriver-manager/commit/fa20ca82e191b122ed49b144b8ebc53ee3b92a9d))
  fix(start): check if edge driver exists before adding to args (#73)

  closes #60
- ([8b61b71](https://github.com/angular/webdriver-manager/commit/8b61b71410dbca6e205fbc599b954fe61a8ee937))
  fix(start): use ie32 if specified via command line (#72)

  closes #68

## Features

- ([8346858](https://github.com/angular/webdriver-manager/commit/83468588fc21f7584b76a8c55afe659db045a4c9))
  feat(logging): add logging property to selenium standalone (#76)

  closes #61
- ([18f9f1d](https://github.com/angular/webdriver-manager/commit/18f9f1dfea02cd8f5c5a2cd5f09130f0ca24f68a))
  chore(selenium): add dev/urandom to selenium start args to prevent startup delays in linux


# 10.2.1

upgrade to latest chrome driver and selenium standalone server versions

# 10.2.0

- ([aa1b8b7](https://github.com/angular/webdriver-manager/commit/aa1b8b7cd9295f02b9bf69274e21eef1a7f3b7f0))
  feat(ios): iOS support (#57)

# 10.1.0

## Bug Fixes

- ([81c2aa3](https://github.com/angular/webdriver-manager/commit/81c2aa3ea6435934797b4d10c6734945484a641d))
  fix(iedriver): download url fix for iedriver (#54)

  closes #53

## Features

- ([57372eb](https://github.com/angular/webdriver-manager/commit/57372ebd076f6b1ccaf41d920601e867b7b3084c))
  feat(edge): add Microsoft Edge support in CLI (#56)

  closes #55
- ([d937245](https://github.com/angular/webdriver-manager/commit/d9372459c51a1aec553a79edaa32e497608a65de))
  feat(android): support android

# 10.0.4

## Dependency Upgrades

- ([970167a](https://github.com/angular/webdriver-manager/commit/970167a1b2db24fc8ca34db2994507ef0187ee7e))
  dep(typings): update typings (#42)

## Bug Fixes

- ([5073e23](https://github.com/angular/webdriver-manager/commit/5073e230574237047dd593a702f08f84907871bd))
  fix(folder): fix selenium folder location (#43)

# 10.0.3

## Bug Fixes

- ([d3724fb](https://github.com/angular/webdriver-manager/commit/d3724fbd9f6b0ceb481538f7f8f0088c8b004959))
  fix(config): simplify locating configuration file, selenium folder (#41)

  * Let the bin file decide which webdriver-manager to use
  * Use the configuration file / package from the default position
  * Selenium folder will always be located to webdriver-manager/selenium/

# 10.0.2

## Bug Fixes

- ([5bca026](https://github.com/angular/webdriver-manager/commit/5bca0266118dcabf2e2782820e5c9095f6d16ed4))
  fix(config): configuration file local look up when used as a dependency (#33)

  closes #32
- ([0cfcc88](https://github.com/angular/webdriver-manager/commit/0cfcc88f1383c400f72ea5e49f9600ff652f8214))
  fix(binary): Fix typo in fallback case when chalk isn't available.


# 10.0.1

## Bug Fixes

- ([a6f1edd](https://github.com/angular/webdriver-manager/commit/a6f1edd782251c96d35e79a3bb78b70c2b137aa9))
  fix(global): fix finding config.json for global installs and release 10.0.1 (#23)

# 10.0.0


## Bug Fixes

- ([70d32df](https://github.com/angular/webdriver-manager/commit/70d32df659f19510c25e97ea9a42c7f93813d448))
  fix(dir): check selenium dir and warn user that the folder does not exist (#17)

- ([0ec1443](https://github.com/angular/webdriver-manager/commit/0ec14435379161259435edc7c766388941f1a846))
  fix(binary): file type, unzipping, and permissions

  closes #7, #16

- ([a073fd0](https://github.com/angular/webdriver-manager/commit/a073fd0e9d0290e52ac3a808643b069c71b196c3))
  fix(npm): use global, local, and project without env

  closes #20


- ([6ccb9d8](https://github.com/angular/webdriver-manager/commit/6ccb9d8b9ac6daf79388c44e6d53f1d3d71fd3f8))
  fix(versions): versions option should stay consistent with existing webdriver-manager

  closes #6

- ([c34b05c](https://github.com/angular/webdriver-manager/commit/c34b05cc66849708a2fc515bc455a6a661c867d6))
  fix(bin): local, project, and global usage

- ([4a0caf5](https://github.com/angular/webdriver-manager/commit/4a0caf5a69cacda01df87d4b4cc35092e519d267))
  fix(dep): fix dependency for chalk

- ([15ae0e8](https://github.com/angular/webdriver-manager/commit/15ae0e815270c8af2441002492e3165edd3140df))
  fix(chmod): set permissions to 755

([1820fbc](https://github.com/angular/webdriver-manager/commit/1820fbc46ddc45b70911fb1678f1d99247ec7028))
  Initial commit

## Features

- ([374c3e7](https://github.com/angular/webdriver-manager/commit/374c3e719fce18a2f0a1b751b19bffb7d266cc69))
  feat(length): on update, check to see the file is the correct length

  closes #8
- ([8c47291](https://github.com/angular/webdriver-manager/commit/8c472918ac73390890bbc39fcc4c7a2e86d3b262))
  feat(local): use the local version of webdriver-tool if it is installed

  closes #5

- ([f0622d2](https://github.com/angular/webdriver-manager/commit/f0622d2e173b68e4afcd409f9c0356c8a1c2652a))
  feat(logs): add chrome logs command line option


  closes #11
- ([3b30312](https://github.com/angular/webdriver-manager/commit/3b303129040b17292028452f13c73d62736f1216))
  feat(logger): update logging methods (#5)
