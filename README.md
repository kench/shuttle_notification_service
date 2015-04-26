# Real-time Notification Server for Shuttle Tracking

This service provides publishing, retrieval, and notification APIs for real-time notification.

This service is written in Java using the Dropwizard framework.

Notifications are persisted in Redis and sent over Redis PubSub.

Consumers for WebSockets are coming soon.

## Usage

### Sending notifications

Sending a notification to all subscribers of the "test" topic:

    curl -H 'Content-Type: application/json' -X PUT -d '{"title":"This is a test notification.", "payload": ["Foo", "Bar", "Baz"]}' http://localhost:8080/notifications?topic=test

## Building

    mvn package
    java -jar target/shuttle-notifications-0.0.1-SNAPSHOT.jar server config/configuration.sample.yml

## Setting up a Redis server for development

## Mac OS X

    brew install redis
    redis-server /usr/local/etc/redis.conf