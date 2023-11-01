package com.learnwiremock.service;

import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
public class MoviesRestClient {

  private final WebClient webClient;

  public MoviesRestClient(WebClient webClient) {
    this.webClient = webClient;
  }

  List<Movie> getAllMovies() {
    return webClient.get()
        .uri(MoviesAppConstants.V1_GET_ALL_MOVIES)
        .retrieve()
        .bodyToFlux(Movie.class)
        .collectList()
        .block();
  }

  public Movie getMovieById(@NonNull Long id) {
    try {
      return webClient.get()
          .uri(MoviesAppConstants.V1_GET_MOVIE_BY_ID, id)
          .retrieve()
          .bodyToMono(Movie.class)
          .block();
    } catch (WebClientResponseException e) {
      log.error(String.format(e.getClass().getName()
              + ": Movie id %d not found with status %s. Response message is: %s", id,
          e.getStatusCode(), e.getResponseBodyAsString()));
      throw new MovieErrorResponse(e.getStatusText(), e);
    } catch (Exception e) {
      log.error(
          String.format(e.getClass().getName() + ": Movie id %d not found. Response message is: %s",
              id, e.getMessage()));
      throw new MovieErrorResponse(e);
    }
  }

  public List<Movie> getMoviesByName(@NonNull String name) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Name argument in search by name must not be blank");
    }
    try {
      return webClient.get()
          .uri(builder -> builder
              .path(MoviesAppConstants.V1_GET_MOVIE_BY_NAME)
              .queryParam(MoviesAppConstants.V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME, name)
              .build())
          .retrieve()
          .bodyToFlux(Movie.class)
          .collectList()
          .block();
    } catch (WebClientResponseException e) {
      log.error(String.format(e.getClass().getName()
              + ": Movies matching name %s not found with status %s. Response message is: %s", name,
          e.getStatusCode(), e.getResponseBodyAsString()));
      throw new MovieErrorResponse(e.getStatusText(), e);
    } catch (Exception e) {
      log.error(String.format(
          e.getClass().getName() + ": Movies matching name %s not found. Response message is: %s", name,
          e.getMessage()));
      throw new MovieErrorResponse(e);
    }
  }

  public List<Movie> getMoviesByYear(@NonNull Integer year) {
    try {
      return webClient.get()
          .uri(builder -> builder
              .path(MoviesAppConstants.V1_GET_MOVIE_BY_YEAR)
              .queryParam(MoviesAppConstants.V1_GET_MOVIE_BY_YEAR_QUERY_PARAM_YEAR, year)
              .build())
          .retrieve()
          .bodyToFlux(Movie.class)
          .collectList()
          .block();
    } catch (WebClientResponseException e) {
      log.error(String.format(e.getClass().getName()
              + ": Movies from year %d not found with status %s. Response message is: %s", year,
          e.getStatusCode(), e.getResponseBodyAsString()));
      throw new MovieErrorResponse(e.getStatusText(), e);
    } catch (Exception e) {
      log.error(String.format(
          e.getClass().getName() + ": Movies from year %d not found. Response message is: %s", year,
          e.getMessage()));
      throw new MovieErrorResponse(e);
    }
  }
}
