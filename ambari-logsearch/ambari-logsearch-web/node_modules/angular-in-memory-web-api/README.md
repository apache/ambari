# Angular in-memory-web-api
[![Build Status][travis-badge]][travis-badge-url]

An in-memory web api for Angular demos and tests.

It intercepts Angular `Http` requests that would otherwise go to the remote server
via the Angular `XHRBackend` service

>**LIMITATIONS**
>
>The _in-memory-web-api_ exists primarily to support the 
[Angular documentation](https://angular.io/docs/ts/latest/ "Angular documentation web site").
It is not supposed to emulate every possible real world web API and is not intended for production use.
>
>Most importantly, it is ***always experimental***. 
We will make breaking changes and we won't feel bad about it 
because this is a development tool, not a production product. 
We do try to tell you about such changes in the `CHANGELOG.md`
and we fix bugs as fast as we can.


>**UPDATE NOTICE**
>
>As of v.0.1.0, the npm package was renamed from `angular2-in-memory-web-api` to its current name,
`angular-in-memory-web-api`. All versions ***after 0.0.21*** are shipped under this name.
**Be sure to update your `package.json` and import statements**.

## HTTP request handling
This in-memory web api service processes an HTTP request and 
returns an `Observable` of HTTP `Response` object
in the manner of a RESTy web api.
It natively handles URI patterns in the form `:base/:collectionName/:id?`

Examples:
```
  // for requests to an `api` base URL that gets heroes from a 'heroes' collection 
  GET api/heroes          // all heroes
  GET api/heroes/42       // the character with id=42
  GET api/heroes?name=^j  // 'j' is a regex; returns heroes whose name starting with 'j' or 'J'
  GET api/heroes.json/42  // ignores the ".json"
```
<a id="commands"></a>
## Commands

The service also accepts "commands" that can, for example, reconfigure the service and reset the database.

When the last segment of the _api base path_ is "commands", the `collectionName` is treated as the _command_.
Example URLs:
```
  commands/resetdb   // Reset the "database" to its original state
  commands/config    // Get or update this service's config object
```

Commands are "hot", meaning they are always executed immediately
whether or not someone subscribes to the returned observable.

Usage:
```
  http.post('commands/resetdb', undefined);
  http.get('commands/config');
  http.post('commands/config', '{"delay":1000}');
```

## Basic usage
Create an `InMemoryDataService` class that implements `InMemoryDbService`.

At minimum it must implement `createDb` which 
creates a "database" hash whose keys are collection names
and whose values are arrays of collection objects to return or update.
For example:
```ts
import { InMemoryDbService } from 'angular-in-memory-web-api';

export class InMemHeroService implements InMemoryDbService {
  createDb() {
    let heroes = [
      { id: '1', name: 'Windstorm' },
      { id: '2', name: 'Bombasto' },
      { id: '3', name: 'Magneta' },
      { id: '4', name: 'Tornado' }
    ];
    return {heroes};
  }
}
```

Register this module and your service implementation in `AppModule.imports`
calling the `forRoot` static method with this service class and optional configuration object:
```ts
// other imports
import { HttpModule }           from '@angular/http';
import { InMemoryWebApiModule } from 'angular-in-memory-web-api';

import { InMemHeroService }     from '../app/hero-data';
@NgModule({
 imports: [
   HttpModule,
   InMemoryWebApiModule.forRoot(InMemHeroService),
   ...
 ],
 ...
})
export class AppModule { ... }
```

See examples in the Angular.io such as the
[Server Communication](https://angular.io/docs/ts/latest/guide/server-communication.html) and
[Tour of Heroes](https://angular.io/docs/ts/latest/tutorial/toh-pt6.html) chapters.

>Always import the `InMemoryWebApiModule` _after_ the `HttpModule` to ensure that 
the `XHRBackend` provider of the `InMemoryWebApiModule` supersedes all others.

# Bonus Features
Some features are not readily apparent in the basic usage example.

The `InMemoryBackendConfigArgs` defines a set of options. Add them as the second `forRoot` argument:
```ts
  InMemoryWebApiModule.forRoot(InMemHeroService, { delay: 500 }),
```

**Read the `InMemoryBackendConfigArgs` interface to learn about these options**.


## Request evaluation order
This service can evaluate requests in multiple ways depending upon the configuration.
Here's how it reasons:
1. If it looks like a [command](#commands), process as a command
2. If the [HTTP method is overridden](#method-override) 
3. If the resource name (after the api base path) matches one of the configured collections, process that
4. If not but the `Config.passThruUnknownUrl` flag is `true`, try to [pass the request along to a real _XHRBackend_](#passthru).
5. Return a 404.

See the `handleRequest` method implementation for details.

## Default delayed response

By default this service adds a 500ms delay (see `InMemoryBackendConfig.delay`) 
to all requests to simulate round-trip latency.
You can eliminate that or extend it by setting a different value:
```ts
  InMemoryWebApiModule.forRoot(InMemHeroService, { delay: 0 }),    // no delay
  InMemoryWebApiModule.forRoot(InMemHeroService, { delay: 1500 }), // 1.5 second delay
```

## Simple query strings
Pass custom filters as a regex pattern via query string. 
The query string defines which property and value to match.

Format: `/app/heroes/?propertyName=regexPattern`

The following example matches all names start with the letter 'j'  or 'J' in the heroes collection.

`/app/heroes/?name=^j`

>Search pattern matches are case insensitive by default. 
Set `config.caseSensitiveSearch = true` if needed.

<a id="passthru"></a>
## Pass thru to a live _XHRBackend_

If an existing, running remote server should handle requests for collections 
that are not in the in-memory database, set `Config.passThruUnknownUrl: true`.
This service will forward unrecognized requests via a base version of the Angular `XHRBackend`.

## _parseUrl_ and your override

The `parseUrl` parses the request URL into a `ParsedUrl` object.
`ParsedUrl` is a public interface whose properties guide the in-memory web api
as it processes the request.

### Default _parseUrl_

Default parsing depends upon certain values of `config`: `apiBase`, `host`, and `urlRoot`.
Read the source code for the complete story.

Configuring the `apiBase` yields the most interesting changes to `parseUrl` behavior:

* For `apiBase=undefined` and `url='http://localhost/api/customers/42'`
    ```
    {base: 'api/', collectionName: 'customers', id: '42', ...}
    ```

*  For `apiBase='some/api/root/'` and `url='http://localhost/some/api/root/customers'`
    ```
    {base: 'some/api/root/', collectionName: 'customers', id: undefined, ...}
    ```

*  For `apiBase='/'` and `url='http://localhost/customers'`
    ```
    {base: '/', collectionName: 'customers', id: undefined, ...}
    ```

**The actual api base segment values are ignored**. Only the number of segments matters.
The following api base strings are considered identical: 'a/b' ~ 'some/api/' ~ `two/segments'

This means that URLs that work with the in-memory web api may be rejected by the real server.

### Custom _parseUrl_

You can override the default by implementing a `parseUrl` method in your `InMemoryDbService`.
Such a method must take the incoming request URL string and return a `ParsedUrl` object. 

Assign your alternative to `InMemDbService['parseUrl']`

## _responseInterceptor_

You can morph the response returned by the default HTTP methods, called by `collectionHandler`, 
to suit your needs by adding a `responseInterceptor` method to your `InMemoryDbService` class. 
The `collectionHandler` calls your interceptor like this:
```ts
responseOptions = this.responseInterceptor(responseOptions, requestInfo);
```

<a id="method-override"></a>
## HTTP method interceptors

If you make requests this service can't handle but still want an in-memory database to hold values,
override the way this service handles any HTTP method by implementing a method in
your `InMemoryDbService` that does the job.

The `InMemoryDbService` method name must be the same as the HTTP method name but **all lowercase**.
This service calls it with an `HttpMethodInterceptorArgs` object.
For example, your HTTP GET interceptor would be called like this:
e.g., `yourInMemDbService["get"](interceptorArgs)`.
Your method must **return an `Observable<Response>`** which _should be "cold"_.

The `HttpMethodInterceptorArgs` (as of this writing) are:
```ts
requestInfo: RequestInfo;           // parsed request
db: Object;                         // the current in-mem database collections
config: InMemoryBackendConfigArgs;  // the current config
passThruBackend: ConnectionBackend; // pass through backend, if it exists
```
## Examples

The file `examples/hero-data.service.ts` is an example of a Hero-oriented `InMemoryDbService`,
derived from the [HTTP Client](https://angular.io/docs/ts/latest/guide/server-communication.html) 
sample in the Angular documentation.

To try it, add the following line to `AppModule.imports`
```ts
InMemoryWebApiModule.forRoot(HeroDataService)
```
  
That file also has a `HeroDataOverrideService` derived class that demonstrates overriding
the `parseUrl` method and it has a "cold" HTTP GET interceptor.

Add the following line to `AppModule.imports` to see this version of the data service in action:
```ts
InMemoryWebApiModule.forRoot(HeroDataOverrideService)
```

# To Do
* add tests (shameful omission!)

# Build Instructions

Mostly gulp driven.

The following describes steps for updating from one Angular version to the next

>This is essential even when there are no changes of real consequence.
Neglecting to synchronize Angular 2 versions
triggers typescript definition duplication error messages when
compiling your application project.

- `gulp bump` - up the package version number

- update `CHANGELOG.MD` to record the change

- update the dependent version(s) in `package.json`

- `npm install` the new package(s) (make sure they really do install!)<br>
   `npm list --depth=0`

- consider updating typings, install individually/several:
```
  npm install @types/jasmine @types/node --save-dev
```

- `gulp clean` - clear out all generated `text`

- `npm run tsc` to confirm the project compiles w/o error (sanity check)

 -- NO TESTS YET ... BAD --

- `gulp build`
- commit and push

- `npm publish`

- Fix and validate angular.io docs samples

- Add two tags to the release commit with for unpkg
  - the version number
  - 'latest'

[travis-badge]: https://travis-ci.org/angular/in-memory-web-api.svg?branch=master
[travis-badge-url]: https://travis-ci.org/angular/in-memory-web-api
