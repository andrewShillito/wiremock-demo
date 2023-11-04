package com.learnwiremock.constants;

public class MoviesAppConstants {

  /*
  * GENERAL
  */
  public static final Integer YEAR_OF_FIRST_MOVIE_EVER_MADE = 1888;

  /*
  * URI
  */
  public static final String V1_GET_ALL_MOVIES = "movieservice/v1/allMovies";
  public static final String V1_GET_MOVIE_BY_ID = "movieservice/v1/movie/{id}";
  public static final String V1_GET_MOVIE_BY_NAME = "movieservice/v1/movieName";
  public static final String V1_GET_MOVIE_BY_YEAR = "movieservice/v1/movieYear";
  public static final String V1_POST_MOVIE = "movieservice/v1/movie";

  /*
  * QUERY PARAMETERS
  */
  public static final String V1_GET_MOVIE_BY_NAME_QUERY_PARAM_MOVIE_NAME = "movie_name";
  public static final String V1_GET_MOVIE_BY_YEAR_QUERY_PARAM_YEAR = "year";

}
