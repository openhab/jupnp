# Introduction

jUPnP is a Java UPnP library and has been forked from the Cling project (https://github.com/4thline/cling).

# Build Instructions

Building and running the project is fairly easy if you follow the steps
detailed below.

## Prerequisites

The build infrastructure is based on Maven in order to make it
as easy as possible to get up to speed. If you know Maven already then
there won't be any surprises for you. If you have not worked with Maven
yet, just follow the instructions and everything will miraculously work ;-)

What you need before you start:
- Maven3 from http://maven.apache.org/download.html

Make sure that the "mvn" command is available on your path


## Checkout

Checkout the source code from GitHub, e.g. by running

````
git clone https://github.com/openhab/jupnp.git
````

## Building with Maven

To build jUPnP from the sources, Maven takes care of everything:
- change into the jupnp directory (`cd jupnp`)
- run `mvn clean install` to compile and package all sources

The build result will be available in the folder `target`.
