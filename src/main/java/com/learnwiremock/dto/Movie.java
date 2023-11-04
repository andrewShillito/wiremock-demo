package com.learnwiremock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Movie {

  private String cast;
  private Long movie_id;
  private String name;
  @JsonProperty("release_date")
  private LocalDate releaseDate;
  private Integer year;

  public Movie(String cast, String name, LocalDate releaseDate, Integer year) {
    this.cast = cast;
    this.name = name;
    this.releaseDate = releaseDate;
    this.year = year;
  }
}
