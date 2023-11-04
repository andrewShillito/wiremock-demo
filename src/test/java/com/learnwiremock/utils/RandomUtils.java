package com.learnwiremock.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.constants.MoviesTestConstants;
import com.learnwiremock.dto.Movie;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

public class RandomUtils {

  private static final Random random = new Random();

  public static String getRandomString(int length) {
    StringBuilder sb = new StringBuilder();
    IntStream
        .generate(() -> random.nextInt(0, MoviesTestConstants.ASCII_CHARACTER_SET.length()))
        .limit(length)
        .forEach(index -> sb.append(MoviesTestConstants.ASCII_CHARACTER_SET.charAt(index)));
    return sb.toString();
  }

  public static String getRandomUniqueMovieName(Map<String, Movie> existingMovies) {
    String name;
    int attemptLimit = 0; // sanity prevent infinite loop
    do {
      name = getRandomName();
      attemptLimit++;
    } while (existingMovies.containsKey(name) && attemptLimit < 10);
    assertTrue(attemptLimit < 10, "Attempted to randomly generate a movie name beyond the mandated attemptLimit of 10 attempts.");
    return name;
  }

  public static String getRandomName() {
    return getRandomString(random.nextInt(24, 64));
  }

  /**
   * Returns a new Movie object with null id
   * @return a randomly generated movie
   */
  public static Movie generateRandomMovie() {
    LocalDate localDate = getRandomLocalDate();
    return new Movie(
        getRandomString(100),
        getRandomName(),
        localDate,
        localDate.getYear()
    );
  }

  public static LocalDate getRandomLocalDate(Set<Integer> excludedYears) {
    int year = getRandomYear(excludedYears);
    Month month = Month.of(getRandomMonth());
    return LocalDate.of(year, month, getRandomDayInMonth(year, month));
  }

  public static LocalDate getRandomLocalDate() {
    int year = getRandomYear();
    Month month = Month.of(getRandomMonth());
    return LocalDate.of(year, month, getRandomDayInMonth(year, month));
  }

  public static int getRandomYear(Set<Integer> excludedYears) {
    // random past or future year
    int year;
    do {
      year = getRandomYear();
    } while (excludedYears.contains(year));
    return year;
  }

  /**
   * The first movie ever made was in 1888. This method produces year values between 1888 and now.
   * @return an integer between 1888 and the current year number ie: 2023
   */
  public static int getRandomMovieYear() {
    return random.nextInt(MoviesAppConstants.YEAR_OF_FIRST_MOVIE_EVER_MADE, LocalDate.now().getYear() + 1);
  }

  public static int getRandomYear() {
    return random.nextInt(0, LocalDate.now().getYear() + 1);
  }

  public static Integer getRandomInvalidYear() {
    if (random.nextInt(0, 2) % 2 == 0) {
      return random.nextInt(Integer.MIN_VALUE, MoviesAppConstants.YEAR_OF_FIRST_MOVIE_EVER_MADE);
    }
    return random.nextInt(3000, Integer.MAX_VALUE);
  }

  public static int getRandomMonth() {
    return random.nextInt(1, 13);
  }

  public static int getRandomDayInMonth(int year, Month month) {
    return random.nextInt(1, YearMonth.of(year, month).lengthOfMonth() + 1);
  }
}
