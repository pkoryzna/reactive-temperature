# Reactive Temperature

RasperryPi-based weather sensor supporting DS18B20 1-wire sensors
(probably also anything else that [w1_therm Linux module](https://www.kernel.org/doc/Documentation/w1/slaves/w1_therm) supports),
using Akka Streams for reading and processing data and Akka HTTP to expose an API. **WIP**
 
## Features

* Finding sensors in directory supplied as first argument, e.g. `/sys/bus/w1/devices`
* Periodically reloading device files for each found sensor


