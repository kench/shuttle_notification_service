package edu.rpi.shuttles.notifications;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import edu.rpi.shuttles.notifications.core.Notification;

public class IntegrationTest {
  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("configuration.test.yml");
  private static final String DEFAULT_NOTIFICATION_TITLE = "Test Notification Title";
  private static final String DEFAULT_NOTIFICATION_MESSAGE = "I'm the bat!";
  private static final long DEFAULT_TIME_TO_LIVE = 5;
  private static final String REDIS_HOST = "localhost";
  private static final String REDIS_TOPIC = "DeliveryQueue:Notifications";
  private static final String UPDATE_NOTIFICATION_MESSAGE = "Fool, I'm the bat!";

  private Client client;
  private Jedis jedis;

  @ClassRule
  public static final DropwizardAppRule<NotificationServerConfiguration> RULE = new DropwizardAppRule<>(
    NotificationServerApplication.class, CONFIG_PATH);

  @Before
  public void setup() {
    client = ClientBuilder.newClient();
    jedis = new Jedis(REDIS_HOST);
  }

  @After
  public void tearDown() {
    client.close();
    jedis.del(REDIS_TOPIC);
    jedis.close();
  }

  @Test
  public void testNotificationCreation() {
    final Notification notification = new Notification(null, DEFAULT_NOTIFICATION_TITLE, DEFAULT_NOTIFICATION_MESSAGE, null, null);
    final Response putResponse = client.target("http://localhost:" + RULE.getLocalPort() + "/notifications")
      .request()
      .put(Entity.entity(notification, MediaType.APPLICATION_JSON_TYPE));
    final Notification newNotification = client.target(putResponse.getLocation())
      .request()
      .get(Notification.class);

    assertNotNull(newNotification);
    assertNotNull(newNotification.getId());
    assertEquals(newNotification.getTitle(), DEFAULT_NOTIFICATION_TITLE);
    assertEquals(newNotification.getContent(), DEFAULT_NOTIFICATION_MESSAGE);
    assertEquals(newNotification.getId(), jedis.rpop(REDIS_TOPIC));
  }

  @Test
  public void testNotificationCreationWithTimeToLive() throws InterruptedException {
    final LocalDateTime timeToLiveDateTime = LocalDateTime.now().plusSeconds(DEFAULT_TIME_TO_LIVE);
    final Date timeToLiveDate = Date.from(timeToLiveDateTime.atZone(ZoneId.systemDefault()).toInstant());
    final Notification notification = new Notification(null, DEFAULT_NOTIFICATION_TITLE, DEFAULT_NOTIFICATION_MESSAGE, null, timeToLiveDate);
    final Response putResponse = client.target("http://localhost:" + RULE.getLocalPort() + "/notifications")
      .request()
      .put(Entity.entity(notification, MediaType.APPLICATION_JSON_TYPE));
    final Notification newNotification = client.target(putResponse.getLocation())
      .request()
      .get(Notification.class);

    assertNotNull(newNotification);
    assertNotNull(newNotification.getId());
    assertEquals(newNotification.getTitle(), DEFAULT_NOTIFICATION_TITLE);
    assertEquals(newNotification.getContent(), DEFAULT_NOTIFICATION_MESSAGE);

    Thread.sleep(TimeUnit.SECONDS.toMillis(DEFAULT_TIME_TO_LIVE));

    final Response getResponse = client.target(putResponse.getLocation())
      .request()
      .get();
    assertEquals(getResponse.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void testNotificationUpdate() {
    final Notification notification = new Notification(null, DEFAULT_NOTIFICATION_TITLE, DEFAULT_NOTIFICATION_MESSAGE, null, null);
    final Response putResponse = client.target("http://localhost:" + RULE.getLocalPort() + "/notifications")
      .request()
      .put(Entity.entity(notification, MediaType.APPLICATION_JSON_TYPE));
    final Notification newNotification = client.target(putResponse.getLocation())
      .request()
      .get(Notification.class);

    assertNotNull(newNotification);
    assertEquals(newNotification.getContent(), DEFAULT_NOTIFICATION_MESSAGE);

    newNotification.setContent(UPDATE_NOTIFICATION_MESSAGE);
    final Response updateResponse = client.target(putResponse.getLocation())
      .request()
      .put(Entity.entity(newNotification, MediaType.APPLICATION_JSON_TYPE));
    assertEquals(updateResponse.getStatus(), Response.Status.OK.getStatusCode());

    final Notification updatedNotification = client.target(putResponse.getLocation())
      .request()
      .get(Notification.class);

    assertNotNull(updatedNotification);
    assertEquals(updatedNotification.getContent(), UPDATE_NOTIFICATION_MESSAGE);
  }
}