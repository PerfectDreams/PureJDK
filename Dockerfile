ARG BASE_IMAGE
FROM ${BASE_IMAGE}

RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends tzdata ca-certificates fontconfig && rm -rf /var/lib/apt/lists/*

COPY . /opt/java/openjdk

# Without a UTF-8 locale the JVM decodes filenames as ASCII (sun.jnu.encoding), breaking files with non-ASCII names
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${PATH}:/opt/java/openjdk/bin"

CMD ["jshell"]