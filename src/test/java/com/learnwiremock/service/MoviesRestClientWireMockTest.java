package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import com.learnwiremock.utils.MoviesTestRandomUtils;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@ExtendWith(WireMockExtension.class)
public class MoviesRestClientWireMockTest {

  MoviesRestClient moviesRestClient;
  WebClient webClient;

  @InjectServer
  WireMockServer wireMockServer;

  @ConfigureWireMock
  Options options = WireMockConfiguration
      .wireMockConfig()
      .port(8088)
      .notifier(new ConsoleNotifier(true))
      .extensions(new ResponseTemplateTransformer(true));

  private static final Random random = new Random();
  private static final String wireMockBaseUrl = "http://localhost";
  private static final Map<String, Movie> expectedMovies = Map.of(
      "Batman Begins", new Movie("Christian Bale, Katie Holmes , Liam Neeson", 1L, "Batman Begins", LocalDate.of(2005, Month.JUNE, 15), 2005),
      "Dark Knight", new Movie("Christian Bale, Heath Ledger , Michael Caine", 2L, "Dark Knight", LocalDate.of(2008, Month.JULY, 18), 2008),
      "The Dark Knight Rises", new Movie("Christian Bale, Heath Ledger , Michael Caine", 3L, "The Dark Knight Rises", LocalDate.of(2012, Month.JULY, 20), 2012),
      "The Avengers", new Movie("Robert Downey Jr, Chris Evans , Chris HemsWorth", 4L, "The Avengers", LocalDate.of(2012, Month.MAY, 4), 2012),
      "Avengers: Age of Ultron", new Movie("Robert Downey Jr, Chris Evans , Chris HemsWorth", 5L, "Avengers: Age of Ultron", LocalDate.of(2015, Month.MAY, 1), 2015),
      "Avengers: Infinity War", new Movie("Robert Downey Jr, Chris Evans , Chris HemsWorth", 6L, "Avengers: Infinity War", LocalDate.of(2018, Month.APRIL, 27), 2018),
      "Avengers: End Game", new Movie("Robert Downey Jr, Chris Evans , Chris HemsWorth", 7L, "Avengers: End Game", LocalDate.of(2019, Month.APRIL, 26), 2019),
      "The Hangover", new Movie("Bradley Cooper, Ed Helms , Zach Galifianakis", 8L, "The Hangover", LocalDate.of(2009, Month.JUNE, 5), 2009),
      "The Imitation Game", new Movie("Benedict Cumberbatch, Keira Knightley", 9L, "The Imitation Game", LocalDate.of(2014, Month.DECEMBER, 25), 2014),
      "The Departed", new Movie("Leonardo DiCaprio, Matt Damon , Mark Wahlberg", 10L, "The Departed", LocalDate.of(2006, Month.OCTOBER, 6), 2006)
  );

  /** Prevent clash of randomly selected years with seeded test movies ( so get movie by year is re-runnable ) */
  private static Set<Integer> seededYears = Set.of(2005, 2008, 2012, 2015, 2018, 2019, 2009, 2014, 2006);

  @BeforeEach
  void setUp() {
    webClient = WebClient.create(String.format(wireMockBaseUrl + ":%s/", wireMockServer.port()));
    moviesRestClient = new MoviesRestClient(webClient);
  }

  @Test
  void retrieveAllMovies() {
    stubFor(get("/" + MoviesAppConstants.V1_GET_ALL_MOVIES)
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-all-movies.json")
        )
    );

    List<Movie> movies = moviesRestClient.getAllMovies();
    System.out.println(movies);
    assertNotNull(movies);
    assertFalse(movies.isEmpty());
    assertTrue(movies.stream().allMatch(expectedMovies::containsValue));
    assertTrue(movies.containsAll(expectedMovies.values()));
  }

  @Test
  void getMovieById() {
    final String exampleName = "Example name";
    final String exampleCast = "Example cast member, member number 2, another third member";
    final LocalDate exampleDate = MoviesTestRandomUtils.getRandomLocalDate();
    stubFor(get(urlPathMatching("/movieservice/v1/movie/\\d+"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movie-by-id-template-parameterized.json")
            .withTransformerParameter("name", exampleName)
            .withTransformerParameter("release_date", exampleDate.toString())
            .withTransformerParameter("cast", exampleCast)
            .withTransformerParameter("year", exampleDate.getYear())
        )
    );

    LongStream.range(0, 10).forEach((it) -> {
      Movie templateResult = moviesRestClient.getMovieById(it);
      assertNotNull(templateResult);
      assertEquals(it, templateResult.getMovie_id());
      assertEquals(exampleName, templateResult.getName());
      assertEquals(exampleCast, templateResult.getCast());
      assertEquals(exampleDate, templateResult.getReleaseDate());
      assertEquals(exampleDate.getYear(), templateResult.getYear());
    });
  }

  @Test
  void getMovieByIdNotFound() {
    stubFor(get(urlPathMatching("/movieservice/v1/movie/-?\\d+"))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movie-by-id-template-not-found.json")
        )
    );

    // null value
    assertThrows(NullPointerException.class, () -> moviesRestClient.getMovieById(null));

    // boundary value
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(0L));

    // positive values
    LongStream
        .generate(() -> random.nextLong(1L, Long.MAX_VALUE))
        .limit(5)
        .forEach(invalidId -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(invalidId)));
    // negative values
    LongStream
        .generate(() -> random.nextLong(Long.MIN_VALUE, 0))
        .limit(5)
        .forEach(invalidId -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(invalidId)));
  }

  @Test
  void getMovieByName() {
    final String stubUrl = String.format(
        "^/%s\\?%s=[\\w\\W\\s]+$",
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME,
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME
    );
    stubFor(get(urlMatching(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-name-template-randomized.json")
        )
    );

    final String nameQuery = RandomStringUtils.random(32);
    List<Movie> retrievedMovies = moviesRestClient.getMoviesByName(nameQuery);
    assertFalse(retrievedMovies.isEmpty());
    // could have turned them into sets and compared using set logic but this is fine
    retrievedMovies.forEach(it -> {
      assertIsValidMovie(it);
      assertTrue(it.getName().contains(nameQuery));
    });
  }

  void assertIsValidMovie(Movie movie) {
    assertNotNull(movie);
    assertNotNull(movie.getMovie_id());
    assertNotNull(movie.getName());
    assertNotNull(movie.getCast());
    assertNotNull(movie.getReleaseDate());
    assertNotNull(movie.getYear());
  }

  @Test
  void getMoviesByNameNotFound() {
    final String randomName = RandomStringUtils.randomAlphabetic(10);
    final String stubUrl = String.format(
        "/%s?%s=%s",
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME,
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME,
        randomName
    );
    stubFor(get(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-name-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(randomName), "No Movie Available with the given name - " + randomName);
  }

  @Test
  void getMoviesByNameInvalidNameArgument() {
    // Note that this is different than the class impl
    // Overall, null, empty string, and blank string name arguments seemed like improper usage of this method
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName(""), "Name argument in get movies by name must not be blank");
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName("   "), "Name argument in get movies by name must not be blank");
    assertThrows(NullPointerException.class, () -> moviesRestClient.getMoviesByName(null));
  }

  @Test
  void getMoviesByYear() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final LocalDate randomDateInYear = MoviesTestRandomUtils.getRandomLocalDateInYear(year);
    final String stubUrl = String.format(
        "/%s?%s=%d",
        MoviesAppConstants.V1_GET_MOVIE_BY_YEAR,
        MoviesAppConstants.V1_GET_MOVIE_BY_YEAR_QUERY_PARAM_YEAR,
        year
    );

    stubFor(get(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-year-template.json")
            .withTransformerParameter("release_date", randomDateInYear.toString())
        )
    );

    List<Movie> retrievedMovies = moviesRestClient.getMoviesByYear(year);
    retrievedMovies.forEach(it -> {
      assertIsValidMovie(it);
      assertEquals(year, it.getYear());
      assertEquals(randomDateInYear, it.getReleaseDate());
    });
  }

  @Test
  void getMoviesByYearNotFound() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = String.format(
        "/%s?%s=%d",
        MoviesAppConstants.V1_GET_MOVIE_BY_YEAR,
        MoviesAppConstants.V1_GET_MOVIE_BY_YEAR_QUERY_PARAM_YEAR,
        year
    );
    stubFor(get(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-year-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year), "No Movie Available with the given year - " + year);
  }

  @Test
  void createMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    final String stubUrl = String.format("/%s", MoviesAppConstants.V1_POST_MOVIE);
    stubFor(post(urlEqualTo(stubUrl))
        .withRequestBody(matchingJsonPath("$.name", equalTo(movie.getName())))
        .withRequestBody(matchingJsonPath("$.cast", equalTo(movie.getCast())))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("post-movie-template.json")
        )
    );

    Movie createdMovie = moviesRestClient.createMovie(movie);
    assertCreatedMovieIsAsExpected(movie, createdMovie);
  }

  void assertCreatedMovieIsAsExpected(Movie expected, Movie actual) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getCast(), actual.getCast());
    assertEquals(expected.getYear(), actual.getYear());
    assertNotEquals(expected.getMovie_id(), actual.getMovie_id());
  }

  @Test
  void createMovieBadRequest() {
    final String stubUrl = String.format("/%s", MoviesAppConstants.V1_POST_MOVIE);
    stubFor(post(urlPathMatching(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .withBodyFile("post-movie-template-not-found.json")
        )
    );

    badCreateRequest(m -> m.setCast(null));
    badCreateRequest(m -> m.setName(null));
    badCreateRequest(m -> m.setYear(null));
    badCreateRequest(m -> m.setReleaseDate(null));
  }

  void badCreateRequest(Consumer<Movie> mutator) {
    Movie movie = MoviesTestRandomUtils.getRandomMovie();
    mutator.accept(movie);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(movie), "Bad Request");
  }

  @Test
  void updateMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(put(urlMatching(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("put-movie-template.json")
            .withTransformerParameter("cast", movie.getCast())
            .withTransformerParameter("id", movie.getMovie_id())
        )
    );

    // append to cast
    String existingCast = movie.getCast();
    movie.setCast(RandomStringUtils.randomAlphanumeric(10));
    Movie updated = moviesRestClient.updateMovie(movie.getMovie_id(), movie);
    assertEquals(existingCast + ", " + movie.getCast(), updated.getCast());

    // change name
    movie.setName(MoviesTestRandomUtils.getRandomUniqueMovieName(expectedMovies));
    updated = moviesRestClient.updateMovie(movie.getMovie_id(), movie);
    assertEquals(movie.getName(), updated.getName());

    // change year
    movie.setYear(MoviesTestRandomUtils.getRandomYear(seededYears));
    updated = moviesRestClient.updateMovie(movie.getMovie_id(), movie);
    assertEquals(movie.getYear(), updated.getYear());
  }

  @Test
  void updateMovieNotFound() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrlNotFound = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id() + 1);
    stubFor(put(urlMatching(stubUrlNotFound))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("put-movie-template-not-found.json")
            .withTransformerParameter("cast", movie.getCast())
            .withTransformerParameter("id", movie.getMovie_id() + 1)
        )
    );
    Long existingId = movie.getMovie_id();
    Long newId = movie.getMovie_id() + 1;
    movie.setMovie_id(newId);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie), "Not Found");
    movie.setMovie_id(existingId); // reset to correct value
  }

  @Test
  void updateMovieBadRequest() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrlNotFound = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(put(urlMatching(stubUrlNotFound))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("put-movie-template-bad-request.json")
            .withTransformerParameter("cast", movie.getCast())
            .withTransformerParameter("id", movie.getMovie_id())
        )
    );
    movie.setName(null);
    movie.setYear(null);
    movie.setReleaseDate(null);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie), "Not Found");
  }

  @Test
  void deleteMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(0, Long.MAX_VALUE));
    final String stubUrl = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .withBodyFile("delete-movie-template.json")
        )
    );

    final String expectedResponseBody = "Movie Deleted Successfully";

    // delete existing movie
    String deleteResponse = moviesRestClient.deleteMovie(movie.getMovie_id());
    assertEquals(expectedResponseBody, deleteResponse);
  }

  @Test
  void deleteMovieBadRequest() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(0, Long.MAX_VALUE));
    final String stubUrl = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .withBodyFile("delete-movie-template-bad-request.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(movie.getMovie_id()));
  }

  @Test
  void deleteMovieNotFound() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(0, Long.MAX_VALUE));
    final String stubUrl = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
            .withBodyFile("delete-movie-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(movie.getMovie_id()));
  }

}