Network Latency detection
===============

This is a network latency project built on OpenDaylight.

## Installation

### Build Latency

``` bash
git clone https://github.com/mingming9/latency.git
cd latency
mvn clean install -DskipTests
```

### Prerequirements

Before building ODLMapleAdapter, please make sure your maven can access the
repository of OpenDaylight. Edit your `settings.xml` of maven like the following
link:

<https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup#Edit_your_.7E.2F.m2.2Fsettings.xml>

## How to use

### Prepare environment

Start Latency from karaf:

``` bash
username@ubuntu:$(latency_directory)./karaf/target/assembly/bin/karaf
```
After booting Latency project, we connect mininet to it.

In mininet VM:

``` bash
username@ubuntu:sudo mn --controller=remote,ip=${controller_ip_address} --mac --switch=ovsk,protocols=OpenFlow13 --topo tree,depth=2,fanout=3
```
Now the environment is ready! Let begin to test!

### Latency detecting

Login dulx:
http://192.168.126.152:8181/index.html
In Topology tag, we can see network topology.
In Yang UI tag, we can find "latency" restconf interface.

Path "latency/operations/network-latency", choose type "SWITCHES", send the input. Then we can see latency detecting of network in console.

Path "latency/operations/switch-switch-latency", fill in A switch dpId and B switch dpId, send the input. Then we can see latency dection between these two switches in console.



