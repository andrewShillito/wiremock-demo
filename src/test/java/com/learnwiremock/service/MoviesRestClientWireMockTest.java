package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import com.learnwiremock.utils.MoviesTestRandomUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
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
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@ExtendWith(WireMockExtension.class)
public class MoviesRestClientWireMockTest {


  MoviesRestClient moviesRestClient;
  WebClient webClient;

  @InjectServer
  WireMockServer wireMockServer;

  /*
  * Note that these timeout settings are fairly low to avoid growing the test time
  * beyond what is required for asserting timeout handling is as expected
  */

  /** The number of seconds prior to timeout during read or write once connected */
  private static final Integer CLIENT_TIMEOUT_SECONDS = 1;
  /** The number of milliseconds prior to timeout */
  private static final Integer CLIENT_TIMEOUT_MILLIS = 1000;

  @ConfigureWireMock
  Options options = WireMockConfiguration
      .wireMockConfig()
      .port(8088)
      .notifier(new ConsoleNotifier(true))
      .extensions(new ResponseTemplateTransformer(true));

  private final TcpClient tcpClient = TcpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CLIENT_TIMEOUT_MILLIS)
      .doOnConnected(connection -> connection
          .addHandlerLast(new ReadTimeoutHandler(CLIENT_TIMEOUT_SECONDS))
          .addHandlerLast(new WriteTimeoutHandler(CLIENT_TIMEOUT_SECONDS))
      );

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
  private static final Set<Integer> seededYears = Set.of(2005, 2008, 2012, 2015, 2018, 2019, 2009, 2014, 2006);

  private static final String retrieveAllMoviesStubUrl = "/" + MoviesAppConstants.V1_GET_ALL_MOVIES;

  private static final String getByIdStubUrl = "/movieservice/v1/movie/\\d+";

  private static final String getByNameStubUrl = String.format(
      "^/%s\\?%s=[\\w\\W\\s]+$",
      MoviesAppConstants.V1_GET_MOVIE_BY_NAME,
      MoviesAppConstants.V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME
  );

  private static final String getByYearStubUrlPrefix = String.format(
      "/%s?%s=",
      MoviesAppConstants.V1_GET_MOVIE_BY_YEAR,
      MoviesAppConstants.V1_GET_MOVIE_BY_YEAR_QUERY_PARAM_YEAR
  );

  private static final String postMovieStubUrl = String.format("/%s", MoviesAppConstants.V1_POST_MOVIE);

  private static final String putMovieStubUrlPrefix = "/movieservice/v1/movie/";

  private static final String deleteMovieStubUrlPrefix = "/movieservice/v1/movie/";

  @BeforeEach
  void setUp() {
    final String clientBaseUrl = String.format(wireMockBaseUrl + ":%s/", wireMockServer.port());
    webClient = WebClient.builder()
        .baseUrl(clientBaseUrl)
        .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
        .build();
    moviesRestClient = new MoviesRestClient(webClient);
  }

  @Test
  void retrieveAllMovies() {
    stubFor(get(retrieveAllMoviesStubUrl)
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
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void retrieveAllMoviesServerError() {
    stubFor(get(retrieveAllMoviesStubUrl).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getAllMovies());
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void retrieveAllMoviesServiceUnavailable() {
    stubFor(get(retrieveAllMoviesStubUrl).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getAllMovies());
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void retrieveAllMoviesTimeout() {
    stubFor(get(retrieveAllMoviesStubUrl).willReturn(ok().withFixedDelay(CLIENT_TIMEOUT_MILLIS + 1000)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getAllMovies());
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void retrieveAllMoviesFaultEmptyResponse() {
    stubFor(get(retrieveAllMoviesStubUrl).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getAllMovies());
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void retrieveAllMoviesFaultMalformedResponse() {
    stubFor(get(retrieveAllMoviesStubUrl).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getAllMovies());
    verify(exactly(1), getRequestedFor(urlEqualTo(retrieveAllMoviesStubUrl)));
  }

  @Test
  void getMovieById() {
    final String exampleName = "Example name";
    final String exampleCast = "Example cast member, member number 2, another third member";
    final LocalDate exampleDate = MoviesTestRandomUtils.getRandomLocalDateInYear(MoviesTestRandomUtils.getRandomMovieYear());
    stubFor(get(urlPathMatching(getByIdStubUrl))
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

    final int requestCount = 10;
    LongStream.range(0, requestCount).forEach((it) -> {
      Movie templateResult = moviesRestClient.getMovieById(it);
      assertNotNull(templateResult);
      assertEquals(it, templateResult.getMovie_id());
      assertEquals(exampleName, templateResult.getName());
      assertEquals(exampleCast, templateResult.getCast());
      assertEquals(exampleDate, templateResult.getReleaseDate());
      assertEquals(exampleDate.getYear(), templateResult.getYear());
    });
    verify(exactly(requestCount), getRequestedFor(urlPathMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByIdNotFound() {
    stubFor(get(urlPathMatching(getByIdStubUrl))
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
  }

  @Test
  void getMovieByIdServerError() {
    stubFor(get(urlMatching(getByIdStubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(1L));
    verify(exactly(1), getRequestedFor(urlMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByIdServiceUnavailable() {
    stubFor(get(urlMatching(getByIdStubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(1L));
    verify(exactly(1), getRequestedFor(urlMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByIdTimeout() {
    stubFor(get(urlMatching(getByIdStubUrl)).willReturn(ok().withFixedDelay(CLIENT_TIMEOUT_MILLIS + 1000)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(1L));
    verify(exactly(1), getRequestedFor(urlMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByIdFaultEmptyResponse() {
    stubFor(get(urlMatching(getByIdStubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(1L));
    verify(exactly(1), getRequestedFor(urlMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByIdFaultMalformedResponse() {
    stubFor(get(urlMatching(getByIdStubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(1L));
    verify(exactly(1), getRequestedFor(urlMatching(getByIdStubUrl)));
  }

  @Test
  void getMovieByName() {
    stubFor(get(urlMatching(getByNameStubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-name-template-randomized.json")
        )
    );

    final String nameQuery = RandomStringUtils.randomAlphabetic(32);
    List<Movie> retrievedMovies = moviesRestClient.getMoviesByName(nameQuery);
    assertFalse(retrievedMovies.isEmpty());
    // could have turned them into sets and compared using set logic but this is fine
    retrievedMovies.forEach(it -> {
      assertIsValidMovie(it);
      assertTrue(it.getName().contains(nameQuery));
    });
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
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
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByNameInvalidNameArgument() {
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName(""), "Name argument in get movies by name must not be blank");
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName("   "), "Name argument in get movies by name must not be blank");
    assertThrows(NullPointerException.class, () -> moviesRestClient.getMoviesByName(null));
  }

  @Test
  void getMoviesByNameServerError() {
    stubFor(get(urlMatching(getByNameStubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(RandomStringUtils.randomAlphabetic(10)));
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
  }

  @Test
  void getMoviesByNameServiceUnavailable() {
    stubFor(get(urlMatching(getByNameStubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(RandomStringUtils.randomAlphabetic(10)));
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
  }

  @Test
  void getMoviesByNameTimeout() {
    stubFor(get(urlMatching(getByNameStubUrl)).willReturn(ok().withFixedDelay(CLIENT_TIMEOUT_MILLIS + 1000)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(RandomStringUtils.randomAlphabetic(10)));
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
  }

  @Test
  void getMoviesByNameFaultEmptyResponse() {
    stubFor(get(urlMatching(getByNameStubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(RandomStringUtils.randomAlphabetic(10)));
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
  }

  @Test
  void getMoviesByNameFaultMalformedResponse() {
    stubFor(get(urlMatching(getByNameStubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(RandomStringUtils.randomAlphabetic(10)));
    verify(exactly(1), getRequestedFor(urlMatching(getByNameStubUrl)));
  }

  @Test
  void getMoviesByYear() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final LocalDate randomDateInYear = MoviesTestRandomUtils.getRandomLocalDateInYear(year);
    final String stubUrl = getByYearStubUrlPrefix + year;
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
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearNotFound() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("get-movies-by-year-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year), "No Movie Available with the given year - " + year);
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearServerError() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year));
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearServiceUnavailable() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year));
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearTimeout() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl)).willReturn(ok().withFixedDelay(CLIENT_TIMEOUT_MILLIS + 1000)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year));
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearFaultEmptyResponse() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year));
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void getMoviesByYearFaultMalformedResponse() {
    final Integer year = MoviesTestRandomUtils.getRandomMovieYear();
    final String stubUrl = getByYearStubUrlPrefix + year;
    stubFor(get(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(year));
    verify(exactly(1), getRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void createMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    stubFor(post(urlEqualTo(postMovieStubUrl))
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
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  void assertCreatedMovieIsAsExpected(Movie expected, Movie actual) {
    assertEquals(expected.getName(), actual.getName());
    assertEquals(expected.getCast(), actual.getCast());
    assertEquals(expected.getYear(), actual.getYear());
    assertNotEquals(expected.getMovie_id(), actual.getMovie_id());
  }

  @Test
  void createMovieBadRequest() {
    stubFor(post(urlEqualTo(postMovieStubUrl))
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
    verify(exactly(4), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  void badCreateRequest(Consumer<Movie> mutator) {
    Movie movie = MoviesTestRandomUtils.getRandomMovie();
    mutator.accept(movie);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(movie), "Bad Request");
  }

  @Test
  void createMovieServerError() {
    stubFor(post(urlEqualTo(postMovieStubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(MoviesTestRandomUtils.getRandomMovie()));
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  @Test
  void createMovieServiceUnavailable() {
    stubFor(post(urlEqualTo(postMovieStubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(MoviesTestRandomUtils.getRandomMovie()));
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  @Test
  void createMovieTimeout() {
    stubFor(post(urlEqualTo(postMovieStubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(MoviesTestRandomUtils.getRandomMovie()));
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  @Test
  void createMovieFaultEmptyResponse() {
    stubFor(post(urlEqualTo(postMovieStubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(MoviesTestRandomUtils.getRandomMovie()));
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  @Test
  void createMovieFaultMalformedResponse() {
    stubFor(post(urlEqualTo(postMovieStubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(MoviesTestRandomUtils.getRandomMovie()));
    verify(exactly(1), postRequestedFor(urlEqualTo(postMovieStubUrl)));
  }

  @Test
  void updateMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(put(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("put-movie-template.json")
            .withTransformerParameter("cast", movie.getCast())
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
    verify(exactly(3), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void updateMovieNotFound() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrlNotFound = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(put(urlEqualTo(stubUrlNotFound))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("put-movie-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie), "Not Found");
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrlNotFound)));
  }

  @Test
  void updateMovieBadRequest() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrlBadRequest = String.format("/%s%d", "movieservice/v1/movie/", movie.getMovie_id());
    stubFor(put(urlEqualTo(stubUrlBadRequest))
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
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrlBadRequest)));
  }

  @Test
  void updateMovieServerError() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = putMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(put(urlEqualTo(stubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie));
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void updateMovieServiceUnavailable() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = putMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(put(urlEqualTo(stubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie));
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void updateMovieTimeout() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = putMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(put(urlEqualTo(stubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie));
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void updateMovieFaultEmptyResponse() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = putMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(put(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie));
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void updateMovieFaultMalformedResponse() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(1, Long.MAX_VALUE));
    final String stubUrl = putMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(put(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movie.getMovie_id(), movie));
    verify(exactly(1), putRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovie() {
    final Movie movie = MoviesTestRandomUtils.getRandomMovie();
    movie.setMovie_id(RandomUtils.nextLong(0, Long.MAX_VALUE));
    final String stubUrl = deleteMovieStubUrlPrefix + movie.getMovie_id();
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("delete-movie-template.json")
        )
    );

    final String expectedResponseBody = "Movie Deleted Successfully";

    // delete existing movie
    String deleteResponse = moviesRestClient.deleteMovie(movie.getMovie_id());
    assertEquals(expectedResponseBody, deleteResponse);
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieBadRequest() {
    final Long badRequestId = RandomUtils.nextLong(0, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + badRequestId;
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.BAD_REQUEST.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("delete-movie-template-bad-request.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(badRequestId));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieNotFound() {
    final Long notFoundId = RandomUtils.nextLong(0, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + notFoundId;
    stubFor(delete(urlEqualTo(stubUrl))
        .willReturn(aResponse()
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("delete-movie-template-not-found.json")
        )
    );
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(notFoundId));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieServerError() {
    final long id = RandomUtils.nextLong(1, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + id;
    stubFor(delete(urlEqualTo(stubUrl)).willReturn(serverError()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieServiceUnavailable() {
    final long id = RandomUtils.nextLong(1, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + id;
    stubFor(delete(urlEqualTo(stubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieTimeout() {
    final long id = RandomUtils.nextLong(1, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + id;
    stubFor(delete(urlEqualTo(stubUrl)).willReturn(serviceUnavailable()));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieFaultEmptyResponse() {
    final long id = RandomUtils.nextLong(1, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + id;
    stubFor(delete(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

  @Test
  void deleteMovieFaultMalformedResponse() {
    final long id = RandomUtils.nextLong(1, Long.MAX_VALUE);
    final String stubUrl = deleteMovieStubUrlPrefix + id;
    stubFor(delete(urlEqualTo(stubUrl)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(id));
    verify(exactly(1), deleteRequestedFor(urlEqualTo(stubUrl)));
  }

}