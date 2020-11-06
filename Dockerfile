FROM oracle/graalvm-ce:20.1.0-java11 AS build

WORKDIR /root

#prepare
RUN gu install native-image

# copy source
COPY src/ .

ENV AWS_REGION=us-east-1
# apk add iproute2ss -t
# build
RUN echo "Main-Class: OpenedFileDescriptors" > manifest.txt && javac OpenedFileDescriptors.java &&  jar -cvfm OpenedFileDescriptors.jar manifest.txt OpenedFileDescriptors.class && native-image -H:EnableURLProtocols=http -H:ReflectionConfigurationFiles=reflection-config.json -jar OpenedFileDescriptors.jar

FROM frolvlad/alpine-glibc:alpine-3.11_glibc-2.31
WORKDIR /root

COPY --from=build /root/OpenedFileDescriptors .

RUN chmod +x OpenedFileDescriptors

EXPOSE 8080

CMD ["./OpenedFileDescriptors", "-Xmx500m", "-Xss64m"]
