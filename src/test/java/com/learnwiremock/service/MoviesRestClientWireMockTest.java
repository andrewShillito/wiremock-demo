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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
            .withBodyFile("get-movie-template-parameterized.json")
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
            .withBodyFile("get-movie-template-not-found.json")
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
            .withBodyFile("get-movie-by-name-template-randomized.json")
        )
    );

    final String nameQuery = "Avengers";
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
    final String stubUrl = String.format(
        "/%s?%s=notFound",
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME,
        MoviesAppConstants.V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME
    );
    stubFor(get(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movie-by-name-template-not-found.json")
        )
    );
    final String nameQuery = "notFound";
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(nameQuery), "No Movie Available with the given name - " + nameQuery);
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
    Map<Integer, List<Movie>> moviesGroupedByYear = expectedMovies.values().stream().collect(Collectors.groupingBy(Movie::getYear));
    moviesGroupedByYear.forEach((year, expectedMovies) -> {
      List<Movie> retrievedMovies = moviesRestClient.getMoviesByYear(year);
      assertEquals(expectedMovies.size(), retrievedMovies.size());
      assertTrue(retrievedMovies.containsAll(expectedMovies));
      assertTrue(expectedMovies.containsAll(retrievedMovies));
    });
  }

  @Test
  void getMoviesByYearNotFound() {
    IntStream
        .generate(MoviesTestRandomUtils::getRandomInvalidYear)
        .limit(25)
        .forEach(invalidYear -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(invalidYear)));
  }

  @Test
  void createMovie() {
    // note behavior that id value given is ignored by the rest api
    AtomicReference<Long> previousId = new AtomicReference<>();
    // with id
    IntStream.range(0, 10).forEach(it -> {
      Movie generatedMovie = MoviesTestRandomUtils.getRandomMovie();
      assertNull(generatedMovie.getMovie_id());
      generatedMovie.setMovie_id(random.nextLong(1L, Long.MAX_VALUE));
      Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
      Long expectedId = previousId.accumulateAndGet(1L, (a, b) -> a == null || b == null ? createdMovie.getMovie_id() : a + b);
      assertCreatedMovieIsAsExpected(generatedMovie, createdMovie, expectedId);
    });

    // without id
    IntStream.range(0, 10).forEach(it -> {
      Movie generatedMovie = MoviesTestRandomUtils.getRandomMovie();
      assertNull(generatedMovie.getMovie_id());
      Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
      assertCreatedMovieIsAsExpected(generatedMovie, createdMovie, previousId.accumulateAndGet(1L, Long::sum));
    });
  }

  void assertCreatedMovieIsAsExpected(Movie expected, Movie actual, Long expectedId) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getCast(), actual.getCast());
    assertEquals(expected.getReleaseDate(), actual.getReleaseDate());
    assertEquals(expected.getYear(), actual.getYear());
    assertNotEquals(expected.getMovie_id(), actual.getMovie_id());
    if (expectedId != null) {
      assertEquals(expectedId, actual.getMovie_id());
    }
  }

  @Test
  void createMovieBadRequest() {
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
    Movie generatedMovie = MoviesTestRandomUtils.getRandomMovie();
    Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
    assertCreatedMovieIsAsExpected(generatedMovie, createdMovie, null);

    // clear cast?
    String cast = createdMovie.getCast();
    createdMovie.setCast(null);
    Movie updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(cast, updated.getCast()); // nothing happens if you try to set to null

    // append to cast
    createdMovie.setCast(RandomStringUtils.randomAscii(10));
    updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(cast + ", " + createdMovie.getCast(), updated.getCast());

    // change name
    createdMovie.setName(MoviesTestRandomUtils.getRandomUniqueMovieName(expectedMovies));
    updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(createdMovie.getName(), updated.getName());

    // try and fail to increment id
    Long existingId = createdMovie.getMovie_id();
    Long newId = createdMovie.getMovie_id() + 1;
    createdMovie.setMovie_id(newId);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie), "Not Found");
    createdMovie.setMovie_id(existingId); // reset to correct value

    // change LocalDate
    createdMovie.setReleaseDate(MoviesTestRandomUtils.getRandomLocalDate(seededYears));
    updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(createdMovie.getReleaseDate(), updated.getReleaseDate());

    // change year
    createdMovie.setYear(MoviesTestRandomUtils.getRandomYear(seededYears));
    updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(createdMovie.getYear(), updated.getYear());

    // change everything at once
    LocalDate newReleaseDate = MoviesTestRandomUtils.getRandomLocalDate(seededYears);
    String appendToCast = RandomStringUtils.randomAscii(10);
    String newName = MoviesTestRandomUtils.getRandomName();
    String previousCast = updated.getCast();
    createdMovie.setName(newName);
    createdMovie.setCast(appendToCast);
    createdMovie.setReleaseDate(newReleaseDate);
    createdMovie.setYear(newReleaseDate.getYear());
    updated = moviesRestClient.updateMovie(createdMovie.getMovie_id(), createdMovie);
    assertEquals(createdMovie.getMovie_id(), updated.getMovie_id());
    assertEquals(createdMovie.getName(), updated.getName());
    assertEquals(previousCast + ", " + createdMovie.getCast(), updated.getCast());
    assertEquals(createdMovie.getReleaseDate(), updated.getReleaseDate());
    assertEquals(createdMovie.getYear(), updated.getYear());
  }

  @Test
  void deleteMovie() {
    final String expectedResponseBody = "Movie Deleted Successfully";

    Movie generatedMovie = MoviesTestRandomUtils.getRandomMovie();
    Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
    assertCreatedMovieIsAsExpected(generatedMovie, createdMovie, null);

    Movie generatedMovie2 = MoviesTestRandomUtils.getRandomMovie();
    Movie createdMovie2 = moviesRestClient.createMovie(generatedMovie2);
    assertCreatedMovieIsAsExpected(generatedMovie2, createdMovie2, null);

    // delete existing movie
    String deleteResponse = moviesRestClient.deleteMovie(createdMovie.getMovie_id());
    assertEquals(expectedResponseBody, deleteResponse);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(createdMovie.getMovie_id()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(createdMovie.getMovie_id()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(createdMovie.getName()));
    assertEquals(createdMovie2, moviesRestClient.getMovieById(createdMovie2.getMovie_id()));

    // delete second existing movie
    deleteResponse = moviesRestClient.deleteMovie(createdMovie2.getMovie_id());
    assertEquals(expectedResponseBody, deleteResponse);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(createdMovie2.getMovie_id()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(createdMovie2.getMovie_id()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(createdMovie2.getName()));

    // invalid arguments
    assertThrows(NullPointerException.class, () -> moviesRestClient.deleteMovie(null));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(0L));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(-1L));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(Long.MIN_VALUE));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(Long.MAX_VALUE));
  }

}