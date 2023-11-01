package com.learnwiremock.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MoviesAppConstants {

  public static final String V1_GET_ALL_MOVIES = "movieservice/v1/allMovies";
  public static final String V1_GET_MOVIE_BY_ID = "movieservice/v1/movie/{id}";
  public static final String V1_GET_MOVIE_BY_NAME = "movieservice/v1/movieName";
  public static final String V1_GET_MOVIE_BY_NAME_MOVIE_NAME_QUERY_PARAM = "movie_name";

}
