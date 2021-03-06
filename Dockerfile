FROM java:openjdk-8-jre-alpine
WORKDIR /app
COPY . /app

RUN apk add --no-cache curl tar bash
RUN curl -L "https://github.com/sbt/sbt/releases/download/v$sbt_version$/sbt-$sbt_version$.tgz" | tar -xz -C /root && \
    ln -s /root/sbt/bin/sbt /usr/local/bin/sbt && \
    chmod 0755 /usr/local/bin/sbt && \
    sbt sbtVersion

RUN sbt "packageBin"
RUN unzip -o target/universal/$name$*.zip
RUN mkdir /build
RUN mv $name$-*.SNAPSHOT /build/app
RUN mv /build/app/bin/$name$ /build/app/bin/service

FROM java:openjdk-8-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app
COPY --from=0 /build/app /app
CMD "/app/bin/service"