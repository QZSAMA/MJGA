# Build stage
FROM golang:1.21-alpine AS builder

WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /openclaw-proxy .

# Final stage
FROM alpine:latest

RUN apk --no-cache add ca-certificates

COPY --from=builder /openclaw-proxy /openclaw-proxy

EXPOSE 8080

ENTRYPOINT ["/openclaw-proxy"]
