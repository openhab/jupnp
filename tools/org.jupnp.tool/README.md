# jUPnP Tool

jUPnP Tool is a command line tool using jUPnP library.
It will provide options to search and query UPnP devices.

## How to build the tool

```shell
mvn clean compile assembly:single
```

It will generate all required jar files in `target` folder.

## How to use the tool

You can start this tool with:

```shell
$ alias jupnptool="java -jar jupnptool-3.0.0-SNAPSHOT.jar"
$ jupnptool

Usage: jupnptool [options] [command] [command options]
  Options:
    --help, -h, -?
       Help how to use this tool
       Default: false
    --loglevel, -l
       Set LogLevel to {OFF|ERROR|WARN|INFO|DEBUG|TRACE}
       Default: DISABLED
    --multicastResponsePort, -m
       Specify Multicast Response Port
       Default: 0
    --pool, -p
       Configure thread pools and enable pool statistic
       (mainPoolSize,asyncPoolSize[,stats])
       Default: 20,20
    --verbose, -v
       Enable verbose messages
       Default: false
  Commands:
    search      Search for UPnP devices
      Usage: search [options]
        Options:
          --filter, -f
             Filter for devices containing this text (in some description)
             Default: *
          --sort, -s
             Sort using {none|ip|model|serialNumber|manufacturer|udn}
             Default: none
          --timeout, -t
             The timeout when search will be finished
             Default: 10

    info      Show UPnP device information
      Usage: info [options] IP address or UDN

    nop      No operation
      Usage: nop [options]
```

### Samples:

```shell
$ jupnptool --help
...
$ jupnptool search
jUPnP Commandline Tool (3.0.0-SNAPSHOT): Search for UPnP devices for 10 seconds sorted by none and filtered by * (poolConfiguration='20,20', multicastResponsePort=0)
IP address       Model                      SerialNumber    
192.168.3.1      FRITZ!Box Fon WLAN 7390    -               
192.168.3.106    QIVICON                    3691234567      
192.168.3.110    NMR                        12345           

$ jupnptool search --timeout=30 --filter=QIVICON
...
$ jupnptool search --sort udn
jUPnP Commandline Tool (3.0.0-SNAPSHOT): Search for UPnP devices for 10 seconds sorted by udn and filtered by * (poolConfiguration='20,20', multicastResponsePort=0)
IP address       Model                      Manufacturer           SerialNumber    UDN                                     
192.168.3.1      FRITZ!Box Fon WLAN 7390    AVM Berlin             -               12345678-1234-40e7-8e6c-246511DDBE2E    
192.168.3.106    QIVICON                    Deutsche Telekom AG    3693BADCC3      861ee488-22b3-11e4-bbea-123456789123    
192.168.3.110    NMR                        Philips                12345           F00DBABE-SA5E-BABA-DADA188ED5A844213     
$ jupnptool --verbose --loglevel=DEBUG search --timeout=60 --filter=Philips
...
```

## Open Issues

* Seldom the ThreadPool is not able to process received packets
TODO here message
* Move build to distribution
