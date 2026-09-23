FROM busybox:1.38.0-uclibc as busybox

FROM gcr.io/distroless/java21

COPY --from=busybox /bin/sh /bin/sh
COPY --from=busybox /bin/printenv /bin/printenv

ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*.jar ./
EXPOSE 8080
USER nonroot

ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar $0"]
CMD ["dab.poao.nav.no.orkivar-all.jar"]