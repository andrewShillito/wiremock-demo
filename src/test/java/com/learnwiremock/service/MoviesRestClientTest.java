package com.learnwiremock.service;

import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.constants.MoviesTestConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MoviesRestClientTest {

  MoviesRestClient moviesRestClient;
  WebClient webClient;

  private static final Random random = new Random();
  private static final Long invalidIdsStartNumber = 100_000L;
  private static final String baseUrl = "http://localhost:8081";
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

  @BeforeEach
  void setUp() {
    webClient = WebClient.create(baseUrl);
    moviesRestClient = new MoviesRestClient(webClient);
  }

  @Test
  void retrieveAllMovies() {
    List<Movie> movies = moviesRestClient.getAllMovies();
    System.out.println(movies);
    assertNotNull(movies);
    assertFalse(movies.isEmpty());
  }

  @Test
  void getMovieById() {
    // given
    expectedMovies.values().forEach((it) -> {
      // when
      Movie retrievedMovie = moviesRestClient.getMovieById(it.getMovie_id());
      // then
      assertNotNull(retrievedMovie);
      assertEquals(it, retrievedMovie);
    });
  }

  @Test
  void getMoviesByIdNotFound() {
    // given
    // just note that this way of testing is predicated on invalidIdsStartNumber being knowable
    // there would be the potential in some cases of clash between this test case and others depending on how data is persisted

    // null value
    assertThrows(NullPointerException.class, () -> moviesRestClient.getMovieById(null));

    // boundary value
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(0L));

    // positive values
    LongStream
        .generate(() -> random.nextLong(invalidIdsStartNumber, Long.MAX_VALUE))
        .limit(25)
        .forEach(invalidId -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(invalidId)));
    // negative values
    LongStream
        .generate(() -> random.nextLong(Long.MIN_VALUE, 0))
        .limit(25)
        .forEach(invalidId -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMovieById(invalidId)));
  }

  @Test
  void getMovieByName() {
    final String avengers = "Avengers";
    List<Movie> expectedAvengersMovies = expectedMovies.entrySet().stream()
        .filter(it -> it.getKey().contains(avengers))
        .map(Entry::getValue)
        .toList();
    List<Movie> avengersMovies = moviesRestClient.getMoviesByName(avengers);
    assertEquals(expectedAvengersMovies.size(), avengersMovies.size());
    // could have turned them into sets and compared using set logic but this is fine
    assertTrue(expectedAvengersMovies.containsAll(avengersMovies));
    assertTrue(avengersMovies.containsAll(expectedAvengersMovies));

    // given
    expectedMovies.forEach((name, movie) -> {
      // when
      List<Movie> retrievedMovies = moviesRestClient.getMoviesByName(name);
      // then
      assertFalse(retrievedMovies.isEmpty());
      assertTrue(retrievedMovies.stream().allMatch((it) -> movie.equals(it) || it.getName().contains(name)));
    });
  }

  @Test
  void getMoviesByNameNotFound() {
    LongStream.range(0, 25).forEach(i -> {
      final String invalidName = getRandomName();
      assertNotNull(invalidName);
      assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByName(invalidName));
    });
  }

  @Test
  void getMoviesByNameInvalidNameArgument() {
    // Note that this is different than the class impl
    // Overall, null, empty string, and blank string name arguments seemed like improper usage of this method
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName(""));
    assertThrows(IllegalArgumentException.class, () -> moviesRestClient.getMoviesByName("   "));
    assertThrows(NullPointerException.class, () -> moviesRestClient.getMoviesByName(null));
  }

  @Test
  @SuppressWarnings("null")
  void getMoviesByNameNullNameArgument() {
  }

  private String getRandomName() {
    String invalidName;
    int attemptLimit = 0; // sanity prevent infinite loop
    do {
      invalidName = getRandomString(random.nextInt(24, 64));
      attemptLimit++;
    } while (expectedMovies.containsKey(invalidName) && attemptLimit < 10);
    assertTrue(attemptLimit < 10, "Attempted to randomly generate a movie name beyond the mandated attemptLimit of 10 attempts.");
    return invalidName;
  }

  private String getRandomString(int length) {
    StringBuilder sb = new StringBuilder();
    IntStream
        .generate(() -> random.nextInt(0, MoviesTestConstants.ASCII_CHARACTER_SET.length()))
        .limit(length)
        .forEach(index -> sb.append(MoviesTestConstants.ASCII_CHARACTER_SET.charAt(index)));
    return sb.toString();
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
        .generate(this::getRandomInvalidYear)
        .limit(25)
        .forEach(invalidYear -> assertThrows(MovieErrorResponse.class, () -> moviesRestClient.getMoviesByYear(invalidYear)));
  }

  Integer getRandomInvalidYear() {
    if (random.nextInt(0, 2) % 2 == 0) {
      return random.nextInt(Integer.MIN_VALUE, MoviesAppConstants.YEAR_OF_FIRST_MOVIE_EVER_MADE);
    }
    return random.nextInt(3000, Integer.MAX_VALUE);
  }

  @Test
  void createMovie() {
    // note behavior that id value given is ignored by the rest api
    AtomicReference<Long> previousId = new AtomicReference<>();
    // with id
    IntStream.range(0, 10).forEach(it -> {
      Movie generatedMovie = generateRandomMovie();
      assertNull(generatedMovie.getMovie_id());
      generatedMovie.setMovie_id(random.nextLong(invalidIdsStartNumber, Long.MAX_VALUE));
      Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
      assertEquals(generatedMovie.getName(), createdMovie.getName());
      assertEquals(generatedMovie.getCast(), createdMovie.getCast());
      assertEquals(generatedMovie.getReleaseDate(), createdMovie.getReleaseDate());
      assertEquals(generatedMovie.getYear(), createdMovie.getYear());
      assertNotEquals(generatedMovie.getMovie_id(), createdMovie.getMovie_id());
      if (previousId.get() != null) {
        assertEquals(previousId.get() + 1, createdMovie.getMovie_id());
      }
      previousId.set(createdMovie.getMovie_id());
    });

    // without id
    IntStream.range(0, 10).forEach(it -> {
      Movie generatedMovie = generateRandomMovie();
      assertNull(generatedMovie.getMovie_id());
      Movie createdMovie = moviesRestClient.createMovie(generatedMovie);
      assertEquals(generatedMovie.getName(), createdMovie.getName());
      assertEquals(generatedMovie.getCast(), createdMovie.getCast());
      assertEquals(generatedMovie.getReleaseDate(), createdMovie.getReleaseDate());
      assertEquals(generatedMovie.getYear(), createdMovie.getYear());
      assertNotEquals(generatedMovie.getMovie_id(), createdMovie.getMovie_id());
      if (previousId.get() != null) {
        assertEquals(previousId.get() + 1, createdMovie.getMovie_id());
      }
      previousId.set(createdMovie.getMovie_id());
    });
  }

  @Test
  void createMovieBadRequest() {
    badCreateRequest(m -> m.setCast(null));
    badCreateRequest(m -> m.setName(null));
    badCreateRequest(m -> m.setYear(null));
    badCreateRequest(m -> m.setReleaseDate(null));
  }

  void badCreateRequest(Consumer<Movie> mutator) {
    Movie movie = generateRandomMovie();
    mutator.accept(movie);
    assertThrows(MovieErrorResponse.class, () -> moviesRestClient.createMovie(movie), "Bad Request");
  }

  /**
   * Returns a new Movie object with null id
   * @return a randomly generated movie
   */
  private Movie generateRandomMovie() {
    int year = getRandomYear();
    Month month = Month.of(getRandomMonth());
    return new Movie(
        getRandomString(100),
        getRandomName(),
        LocalDate.of(year, month, getRandomDayInMonth(year, month)),
        year
    );
  }

  private int getRandomYear() {
    return random.nextInt(0, LocalDate.now().getYear() + 1);
  }

  private int getRandomMonth() {
    return random.nextInt(1, 13);
  }

  private int getRandomDayInMonth(int year, Month month) {
    return random.nextInt(1, YearMonth.of(year, month).lengthOfMonth() + 1);
  }
}