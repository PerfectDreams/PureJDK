# PureJDK

Automatically updated images with [Oracle's OpenJDK](https://jdk.java.net/) builds.

See the [containers page](https://github.com/PerfectDreams/PureJDK/pkgs/container/purejdk).

See [TAGS.md](./TAGS.md) for the supported tags.

## But why?

These were created for developers who want to use the "latest and greatest" OpenJDK builds, where they don't mind following OpenJDK updates to keep up with improvement and security fixes.

I had the idea of running OpenJDK after the [Java4Shell vulnerability was disclosed](https://calif.io/java4shell), where while Oracle had already released new OpenJDK builds with the fix, Temurin took ~19 hours to release new Java 27 builds (which is fine) but, by the time I originally wrote this README (19/11/2026), there wasn't a new Temurin Docker image with the new JDK version!

So that made me wonder... why not use Oracle's OpenJDK builds?

## Aren't there already Docker images with OpenJDK?

* There are [OpenJDK Docker images maintained by docker-library](https://hub.docker.com/_/openjdk), but they only provide early access builds.
* There are [OpenJDK Docker images maintained by Oracle](https://container-registry.oracle.com/ords/ocr/ba/java/openjdk), but they only provide images with Oracle Linux.

## But what about LTS versions?

Java does not have LTS versions, what does exist are Oracle Java LTS versions, and vendors that follow the same Oracle Java LTS cadence that Oracle offers.

If you use a LTS version from a vendor like Adoptium you only have the fixes that Adoptium decided to backport, most of them from the JDK Updates repository.

This does also mean that if you are using PureJDK images you *must* track the most recent Java version, because if you are using JDK 27 while JDK 28 is released, then that means that you are **NOT** receiving any updates!

https://www.reddit.com/r/java/comments/ho4wxe/what_is_the_importance_of_java_lts/fxfxkd0/

___

*OpenJDK is a trademark of Oracle and/or its affiliates. PureJDK is not affiliated with or endorsed by Oracle.*