![build-status](https://travis-ci.org/Jacoby6000/clone-hero-chart-db.svg?branch=master)
# clone-hero-chart-db 
System for indexing clone hero charts

## Contributing

Currently, the system uses symlinks to make sharing code between the build and the project a bit 
simpler.  This may change in the future.  However, if you are on a filesystem that does not support
symlinks, you're going to have a bad time trying to help.

The project is built via `sbt`. You can get `sbt` from Lightbend, or you can use an alternate 
launcher (which i recommend): [paulp/sbt-extras](https://github.com/paulp/sbt-extras).

Once you have `sbt`, you'll need the jdk. For now, the project is built using `openjdk-8`.  Oracle's
jdk works fine, too as long as you have version 8.

## Technology stack

* [PostgreSQL](https://www.postgresql.org/): Because it's the best relational database out there.
* [Scala](https://www.scala-lang.org/): Because I don't know haskell very well yet.
  * [http4s](http://http4s.org): To serve http requests and provide an http client.
  * [scalaz](https://github.com/scalaz/scalaz): To provide robust functional programming abstractions.
  * [doobie](http://tpolecat.github.io/doobie/): To provide database access in a sane manner.
  * [argonaut](http://argonaut.io): For functional json (de)serialization.
  * [tsec](https://github.com/jmcardon/tsec): For cryptography.
  * [pureconfig](https://pureconfig.github.io/docs/): To provide functional config file parsing.
  * [scalacheck](http://www.scalacheck.org/): For property based testing.
  * [flywaydb](https://flywaydb.org/): For convenient database migrations.
  * [enumeratum](https://github.com/lloydmeta/enumeratum): For sane scala enums.
  * [shims](https://github.com/djspiewak/shims): For compatibility between the fp libs.

If the above ever seems incorrect or out of date, check 
[BuildSettings.scala](./project/BuildSettings.scala). If the technology stack is out of date, please
create a pull request to fix it.

## Design Philosophy

This project is to be done in a functionally pure fashion.  
* No null values.
* No exceptions.
* No type casing. (`isInstanceOf`)
* No type casting. (`asInstanceOf`)
* **No side-effects.** _"but..."_ No. Not even then.
* No using methods defined on `Any`. (`equals`, `toString`, `hashCode`)
* No calls to `notify` or `wait`. In fact, avoid most of `Predef`.
* No calls to `classOf` or `getClass`.

This leads to a use of a subset of the scala language, which some call `scalazzi`.

Doing this, we will create an application made up of more composable components which are easier to 
test, easier to debug, and easier to expand upon.  This means more features faster in the long run, 
with fewer bugs.

This design philosophy is expanded upon in Tony Morris' (dibblego) talk about parametricity at YOW West 
2014 ([video](https://www.youtube.com/watch?v=pVCkDZFSmVU&index=5&list=PLIpl4GKFQR6eXub6zaSren896Dfq4lUhs)/[slides](http://yowconference.com.au/slides/yowwest2014/Morris-ParametricityTypesDocumentationCodeReadability.pdf)), 
and is talked about in [lihaoyi's blog](http://www.lihaoyi.com/post/StrategicScalaStylePracticalTypeSafety.html)

Having some knowledge of type classes and functional programming concepts will go a long way to make
working with this code easier.  I highly recommend the 
[data61 fp course](https://github.com/data61/fp-course) if you are new to fp.

