FROM gcr.io/distroless/java21
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*.jar ./
EXPOSE 8080
USER nonroot
CMD ["dab.poao.nav.no.orkivar-all.jar"]