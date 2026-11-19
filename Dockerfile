ARG BASE_IMAGE
FROM ${BASE_IMAGE}

RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends tzdata ca-certificates fontconfig && rm -rf /var/lib/apt/lists/*

COPY . /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${PATH}:/opt/java/openjdk/bin"

CMD ["jshell"]