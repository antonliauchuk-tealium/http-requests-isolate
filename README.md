# http-requests-isolate

echo "Main-Class: OpenedFileDescriptors" > manifest.txt && javac OpenedFileDescriptors.java &&  jar -cvfm OpenedFileDescriptors.jar manifest.txt OpenedFileDescriptors.class && native-image -H:EnableURLProtocols=http -H:EnableURLProtocols=https  -H:ReflectionConfigurationFiles=reflection-config.json -jar OpenedFileDescriptors.jar

./OpenedFileDescriptors
