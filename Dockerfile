FROM eclipse-temurin:21-jdk AS builder-base
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      binutils \
      dos2unix \
      fakeroot \
      rpm && \
    rm -rf /var/lib/apt/lists/*

FROM builder-base AS builder
COPY . /home/boundingboxeditor
WORKDIR /home/boundingboxeditor
RUN dos2unix gradlew && \
    bash ./gradlew jpackage

FROM scratch AS artifacts
COPY --from=builder /home/boundingboxeditor/build/jpackage/ /artifacts
CMD [ "/artifacts" ]
