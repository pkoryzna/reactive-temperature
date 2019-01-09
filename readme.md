# This code is very old and depends on older APIs from experimental versions of Akka Streams etc.

# Reactive Temperature

_or: probably the world's most overengineered attempt at..._

RasperryPi-based weather station supporting DS18B20 1-wire sensors
(probably also anything else that [w1_therm Linux module](https://www.kernel.org/doc/Documentation/w1/slaves/w1_therm) supports),
using Akka Streams for reading and processing data and Akka HTTP to expose an API. **WIP**
 
## Features

* Finding sensors in directory supplied as first argument, e.g. `/sys/bus/w1/devices` (default)
* Periodically reloading device files for each found sensor
* Logging measurements to console
* HTML and JSON view for last measurements
* Configurable sensor names for sensors in `application.conf` (overridable with `-Dsensor.name.<SERIAL>=<NAME>`, thanks to Typesafe's excellent Config library)

## how do I run?

Generate JAR with 
    
    sbt assembly 
    
Run it (protip: add `nohup` to the beginning of line so it doesn't die after logging out of ssh shell)

    java -jar reactive-temperature-assembly-1.1.jar /sys/bus/w1/devices/ &
    
HTTP server will be exposed at `<rpi-ip>:8080`.
