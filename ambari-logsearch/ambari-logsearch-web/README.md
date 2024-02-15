# AmbariLogsearchWeb

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 1.0.0.

## Development server

Run `npm start` or `yarn start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Webpack Development Config
In order to use the UI without changing the main webpack config file (and commit accidentally) we can use a `webpack.config.dev.js` file for that. So you can set a service URL proxy.

The content of the `webpack.config.dev.js` can be:
```
const merge = require('webpack-merge');
const baseConfig = require('./webpack.config.js');

module.exports = merge(baseConfig, {
  devServer: {
    historyApiFallback: true,
    proxy: {
      '/api': 'http://c7401.ambari.apache.org:61888/', // proxying the api requests
      '/login': 'http://c7401.ambari.apache.org:61888/', // proxying the login action
      '/logout': 'http://c7401.ambari.apache.org:61888/' // proxying the the logout action
    }
  }
});
```
And you can start it that way: `yarn start --config webpack.config.dev.js`

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive/pipe/service/class/module`.

## Build

Run `npm run build` or `yarn build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `npm run build-prod` or `yarn build-prod` command for a production build.

## Running unit tests

Run `npm test` or `yarn test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `npm run e2e` or `yarn e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).
Before running the tests make sure you are serving the app via `npm start` or `yarn start`.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

# Application Info

## Routing and URL params
We use [Matrix URIs](https://www.w3.org/DesignIssues/MatrixURIs.html) to route our components. That means within the components we use semicolon separated parameters.
You can create/generate Log Search Client application URL with the following paths and parameters

URL pattern: `/logs/{logs-type};{filter-param1};...;{filter-paramN}`
Where the `{logs-type}` can be `serviceLogs` or `auditLogs` right now.

For this screen the available URL params are the followings:
- `components`:
  - type: filter parameter
  - a comma separated list of components/services
  - eg.: `components=activity_explorer,ambari_agent`
- `levels`:
  - type: filter parameter
  - a comma separated list of log levels
  - eg.: `levels=INFO,WARN,ERROR,FATAL,DEBUG,TRACE,UNKNOW`
- `hosts`:
  - type: filter parameter
  - a comma separated list of hosts
  - eg.: `hosts=c7401.ambari.apache.org,c7402.ambari.apache.org`
- `query`:
  - type: filter parameter
  - a JSON object with the following available keys as filters
  - eg.: `[{"name": "level", "label": "Level", "value": "ERROR", "isExclude": false}]` 

### Time range matrix params
To filter for a range of time you can use the following matrix parameters:
- `timeRangeUnit`: the unit of the time
  - `m`: minute
  - `h`: hour
  - `d`: day
  - `w`: week
  - `M`: month
  - `y`: year
- `timeRangeType`
  - `LAST`: it will count the time from the current moment back
  - `PAST`: it will count the time from the end of the previous time unit (set up in `timeRangeUnit`)
  - `CURRENT`: it will count the time from the end of the current time unit (set up in `timeRangeUnit`)
  - `CUSTOM`: will check the `timeRangeStart` and the `timeRangeEnd` matrix parameters
- `timeRangeStart`: UTC + Time zone format, eg.: `timeRangeStart=2018-06-30T22:00:00.000Z`
- `timeRangeEnd`: UTC + Time zone format, eg.: `timeRangeStart=2018-06-30T23:00:00.000Z`

#### Examples to defining time ranges:
Last 3 hours
`timeRangeType=LAST;timeRangeInterval=3;timeRangeUnit=h`
Last 7 days:
`timeRangeType=LAST;timeRangeInterval=7;timeRangeUnit=d`
Last 1 year
`timeRangeType=LAST;timeRangeInterval=1;timeRangeUnit=y`
Previous week
`timeRangeType=PAST;timeRangeUnit=w`
Previous month
`timeRangeType=PAST;timeRangeUnit=M`
Custom time range
`timeRangeType=CUSTOM;timeRangeStart=2018-07-01T10:06:00.000Z;timeRangeEnd=2018-07-02T13:06:00.000Z`

##### The `query` param
###### Structure
The `query` matrix param is an array of filter params in JSON format. The item schema is the following:
```
{
    name: string,
    label: string,
    value: string/number/boolean,
    isExclude: boolean
}
```
Where the `name` is for the field name that you want to filter, the `label` is what the filter input bar will display, the `value` is the filter value and the `isExclude` is a boolean value indicating if the given field value should be included or excluded from the search result.

###### Available fields in the `query` matrix param for Service Logs
- `cluster` (label: Cluster)
- `method` (label: Method)
- `level` (label: Level)
- `ip` (label: IP)
- `key_log_messafe` (Key Log Message)
- `type` (label: Component)
- `path` (label: Path)
- `logtype` (label: Logtype)
- `file` (label: File)
- `line_number` (label: Line Number)
- `thread_name` (label: Thread)
- `host` (label: Host)
- `log_message`(label: Message)
- `logger_name` (label: Logger Name)
- `logfile_line_number` (label: Logfile Line Number)
- `logtime` (label: Log Time)
- `group` (label: Group)

###### Example of a `query` matrix param
 
```
[{
    "name": "level",
    "label": "Level",
    "value": "ERROR",
    "isExclude": false
},{
    "name": "type",
    "label": "Component",
    "value": "ambari_server",
    "isExclude": true
}]
```

###### Available fields in the `query` matrix param for Audit Logs
- `cluster` (label: Cluster)
- `reason` (label: Reason)
- `ws_status` (label: Status)
- `agent` (label: Agent)
- `sess` (label: Session)
- `ws_repo_id` (label: Repo Id)
- `type` (label: Type)
- `path` (label: Path)
- `ws_details` (label: Details)
- `ugi` (label: UGI)
- `host` (label: Host)
- `case_id` (label: Case Id)
- `action` (label: Action)
- `id` (label: Id)
- `logger_name` (label: Logger Name)
- `text` (label: Text)
- `logfile_line_number` (label: Logfile Line Number)
- `ws_base_url` (label: Base Url)
- `level` (label: Level)
- `resource` (label: Resource)
- `resType` (label: Res Type)
- `ip` (label: IP)
- `req_self_id` (label: Req Self Id)
- `repoType` (label: Repo Type)
- `ws_roles` (label: Roles)
- `bundle_id` (label: Bundle Id)
- `cliType` (label: Client Type)
- `reqContext` (label: Req Context)
- `ws_result_status` (label: Result Status)
- `proxyUsers` (label: Proxy Users)
- `logType` (label: Log Type)
- `access` (label: Access Type)
- `dst` (label: DST)
- `perm` (label: Perm)
- `event_count` (label: Event Count)
- `repo` (label: Repo)
- `ws_request_id` (label: Request Id)
- `reqUser` (label: User)
- `task_id` (label: Task Id)
- `ws_consecutive_failures` (label: Consecutive Failures)
- `ws_stack_version` (label: Stack Version)
- `result` (label: Result)
- `ws_version_number` (label: Version Number)
- `reqData` (label: Req Data)
- `file` (label: File)
- `ws_repositories` (label: Repositories)
- `log_message` (label: Log Message)
- `ws_stack` (label: Stack)
- `agentHost` (label: Agent Host)
- `authType` (label: Auth Type)
- `ws_version_note` (label: Version Note)
- `policy` (label: Policy)
- `cliIP` (label: Client Ip)
- `ws_os` (label: Os)
- `ws_display_name` (label: Display Name)
- `ws_repo_version` (label: Repo Version)
- `evtTime` (label: Event Time)
- `req_caller_id` (label: Req Caller Id)
- `enforcer` (label: Access Enforcer)
- `ws_component` (label: Component)
- `ws_command` (label: Command)
