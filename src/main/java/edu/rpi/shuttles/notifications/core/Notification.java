package edu.rpi.shuttles.notifications.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
public class Notification {
  @Getter
  @Setter
  @JsonProperty
  private String id;
  @Getter
  @Setter
  @JsonProperty
  private String title;
  @Getter
  @Setter
  @JsonProperty
  private String content;
  @Setter
  @JsonProperty
  private JsonNode payload;
  @Getter
  @Setter
  @JsonProperty
  private Date ttl;
}
