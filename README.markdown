Muuntaja [![Build Status](https://secure.travis-ci.org/jelovirt/dita-ot.png?branch=develop)](http://travis-ci.org/jelovirt/dita-ot)
=================

The Muuntaja is an open source tool that provides processing for OASIS DITA content. It is a fork of [DITA Open Toolkit](https://github.com/dita-ot/dita-ot) where Ant has been replaced with Scala code.

Prerequisites
-------------

In order to build and use Muuntaja, you’ll need:

* Java Development Kit 7 or newer
* SBT 0.12.1 or newer

Building
--------

1. Clone Muuntaja Git repository.
2. On root directory, compile Java and Scala code:

        sbt compile

3. Make sure the following files and directories are added to your `CLASSPATH` system variable:
   * `src/main/lib/`
     
3. Run plug-in installation:

        ant -f src/main/integrator.xml
 
Usage
-----

1. Add the following files and directories to `CLASSPATH` system variable:
   * `src/main/`
   * `src/main/lib/`
   * `src/main/lib/dost.jar`
   * `src/main/lib/xercesImpl.jar`
   * `src/main/lib/xml-apis.jar`
   * `src/main/lib/commons-codec-1.4.jar`
   * `src/main/lib/saxon/saxon9-dom.jar`
   * `src/main/lib/saxon/saxon9.jar`
   * `src/main/lib/resolver.jar`
   * `src/main/lib/icu4j.jar`
2. Change directory to `src/main`.
3. Run Muuntaja with:

        ant [options]
        
   See [documention](http://dita-ot.sourceforge.net/latest/) for arguments and options.

Distribution
------------

1. On root directory, compile Java code:

        ant jar
     
2. Add the following files and directories to `CLASSPATH` system variable:
   * `src/main/`
   * `src/main/lib/`
   * `src/main/lib/dost.jar`
   * `src/main/lib/xercesImpl.jar`
   * `src/main/lib/xml-apis.jar`
   * `src/main/lib/commons-codec-1.4.jar`
   * `src/main/lib/saxon/saxon9-dom.jar`
   * `src/main/lib/saxon/saxon9.jar`
   * `src/main/lib/resolver.jar`
   * `src/main/lib/icu4j.jar`

3. Run plug-in installation:

        ant -f src/main/integrator.xml

4. Build distribution packages:

        ant dist
   
   Distribution packages are build into `target` directory.

   On some systems you may encounter an `java.lang.OutOfMemoryError: Java heap space`. In that case you need to provide more memory to the `ant` process. One way of doing that is by setting the `ANT_OPTS` system variable to specify more memory, for example setting that to `-Xmx1000m` should be enough.
   
License
-------

The Muuntaja is licensed for use under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).