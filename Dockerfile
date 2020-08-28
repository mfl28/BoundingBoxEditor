FROM gradle:jdk11 AS builder
COPY --chown=gradle:gradle . /home/gradle/boundingboxeditor
WORKDIR /home/gradle/boundingboxeditor
RUN bash ./gradlew dist --no-daemon -x test && \
    unzip /home/gradle/boundingboxeditor/build/distributions/boundingboxeditor-linux.zip -d /tmp/boundingboxeditor

FROM scratch AS image
COPY --from=builder /tmp/boundingboxeditor/image/ /image
CMD [ "/image" ]
