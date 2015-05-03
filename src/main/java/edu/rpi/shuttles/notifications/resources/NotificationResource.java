package edu.rpi.shuttles.notifications.resources;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import jersey.repackaged.com.google.common.base.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import redis.clients.jedis.Jedis;

import edu.rpi.shuttles.notifications.core.Notification;

@Slf4j
@NoArgsConstructor
@Path("/notifications/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {
  private static final Integer DEFAULT_NOTIFICATION_TTL = 86400;

  private final ObjectMapper objectMapper = new ObjectMapper();

  static final MetricRegistry metrics = new MetricRegistry();
  private final Timer queryTimer = metrics.timer(MetricRegistry.name(NotificationResource.class, "redis-queries"));
  private final Timer writeTimer = metrics.timer(MetricRegistry.name(NotificationResource.class, "redis-writes"));

  @GET
  @Path("/{id}")
  @Timed
  public Response getNotification(@PathParam("id") Optional<String> notificationId, @Context Jedis jedis) {
      final Timer.Context queryTimerContext;
      final Response response;
      final Notification notification;

      try {
        queryTimerContext = queryTimer.time();
        final String notificationData = jedis.get("Notification:" + notificationId.get());
        queryTimerContext.stop();

        if (Strings.isNullOrEmpty(notificationData)) {
          response = Response.status(Response.Status.NOT_FOUND).entity(String.format("No notification found for [id=%s]", notificationId.get())).build();
        } else {
          notification = objectMapper.readValue(notificationData, Notification.class);
          response = Response.status(Response.Status.OK).entity(notification).build();
        }
      } catch (final Exception exception) {
        throw new RuntimeException(exception);
      }

      return response;
  }

  @PUT
  @Path("/{id}")
  @Timed
  public Response updateNotification(@Valid Notification notification, @PathParam("id") Optional<String> notificationId, @Context Jedis jedis) {
    final Response response;
    final Notification currentNotification;
    final String currentNotificationData;
    final String newNotificationData;

    try {
      currentNotificationData = jedis.get("Notification:" + notificationId.get());

      if (Strings.isNullOrEmpty(currentNotificationData)) {
        response = Response.status(Response.Status.NOT_FOUND).entity(String.format("No notification found for [id=%s]", notificationId.get())).build();
      } else {
        currentNotification = objectMapper.readValue(currentNotificationData, Notification.class);

        notification.setId(currentNotification.getId());
        notification.setTtl(Objects.firstNonNull(notification.getTtl(), currentNotification.getTtl()));
        newNotificationData = objectMapper.writeValueAsString(notification);

        jedis.set("Notification:" + notificationId.get(), newNotificationData);

        response = Response.status(Response.Status.OK).entity(notification).build();
      }
    } catch (final Exception exception) {
      throw new RuntimeException(exception);
    }

    return response;
  }

  @PUT
  @Path("/")
  @Timed
  public Response createNotification(@Valid Notification notification, @QueryParam("topic") Optional<String> topic, @Context Jedis jedis) {
    final String identifier;
    final String notificationData;
    final LocalDateTime currentTime;
    final LocalDateTime timeToLive;
    final Integer timeToLiveSeconds;
    final Timer.Context writeTimerContext;

    currentTime = LocalDateTime.now();
    identifier = generatePseudorandomIdentifier();

    notification.setId(identifier);
    if (notification.getTtl() == null) {
      timeToLive = currentTime;
      timeToLive.plusSeconds(DEFAULT_NOTIFICATION_TTL);
      notification.setTtl(Date.from(timeToLive.atZone(ZoneId.systemDefault()).toInstant()));
      timeToLiveSeconds = DEFAULT_NOTIFICATION_TTL;
    } else {
      timeToLive = LocalDateTime.ofInstant(notification.getTtl().toInstant(), ZoneId.systemDefault());
      timeToLiveSeconds = Ints.checkedCast(currentTime.until(timeToLive, ChronoUnit.SECONDS));
    }

    try {
      notificationData = objectMapper.writeValueAsString(notification);

      writeTimerContext = writeTimer.time();
      jedis.setex("Notification:" + identifier, timeToLiveSeconds, notificationData);
      writeTimerContext.stop();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }

    try {
      if (topic.isPresent()) {
        jedis.publish(topic.get(), notificationData);
      }
    } catch (RuntimeException exception) {
      log.error(
        String.format("Encountered failure when publishing notification [id=%s] to topic [%s]", identifier, topic.get()),
        exception);
    }

    return Response.created(UriBuilder.fromResource(NotificationResource.class).path(NotificationResource.class, "getNotification").build(identifier)).build();
  }

  private String generatePseudorandomIdentifier() {
    return String.valueOf(System.currentTimeMillis()) + "-" + RandomStringUtils.randomAlphanumeric(8);
  }
}
