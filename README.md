![build status](https://travis-ci.org/jupnp/jupnp.svg)

# Introduction

jUPnP is a Java UPnP library and has been forked from the Cling project (https://github.com/4thline/cling).

# Build Instructions

Building and running the project is fairly easy if you follow the steps detailed below.

1\. Prerequisites
================

The build infrastructure is based on Maven in order to make it
as easy as possible to get up to speed. If you know Maven already then
there won't be any surprises for you. If you have not worked with Maven
yet, just follow the instructions and everything will miraculously work ;-)

What you need before you start:
- Java SDK 8 (please note that the build does NOT work with any higher version!)
- Maven3 from http://maven.apache.org/download.html

Make sure that the "mvn" command is available on your path


2\. Checkout
===========

Checkout the source code from GitHub, e.g. by running

````
git clone https://github.com/jupnp/jupnp.git
````

3\. Building with Maven
======================

To build jUPnP from the sources, Maven takes care of everything:
- change into the jupnp directory (`cd jupnp`)
- run `mvn clean install` to compile and package all sources

The build result will be available in the folder `target`.


4\. Working with Eclipse
=======================

When using Eclipse ensure that the JDK is set via the `-vm` option in `eclipse.ini`.
Otherwise m2e might fail to resolve the system scoped dependency to `tools.jar`.
