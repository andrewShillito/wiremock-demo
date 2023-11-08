# wiremock-demo
A WireMock testing demo built using Java 17 and gradle.

# Primary content

- [src/main/java/com/learnwiremock/service/MoviesRestClient.java](https://github.com/andrewShillito/wiremock-demo/blob/3d32820176ac4f4a6b7af937959c893a42b1872e/src/main/java/com/learnwiremock/service/MoviesRestClient.java) - A REST client built using Spring WebClient.
- [src/test/java/com/learnwiremock/service/MoviesRestClientWireMockTest.java](https://github.com/andrewShillito/wiremock-demo/blob/3d32820176ac4f4a6b7af937959c893a42b1872e/src/test/java/com/learnwiremock/service/MoviesRestClientWireMockTest.java) A wiremock based test class for a REST API.
- [src/test/java/com/learnwiremock/service/MoviesRestClientTest.java](https://github.com/andrewShillito/wiremock-demo/blob/fd982f3b8ae5f49865956e27efe2c80768690a32/src/test/java/com/learnwiremock/service/MoviesRestClientTest.java) A test class which runs against an example REST API. Optionally, see [Running the example REST API](#Running-the-example-REST-API) below on how to download and run the example API.

# Generate code coverage report using jacocoTestReport
`./gradlew build jacocoTestReport`

# Running the example REST API
Clone or download the `movies-restful-service-beyond-java8.jar` from [movies-restful-service](https://github.com/dilipsundarraj1/wiremock-for-java-developers/tree/master/movies-restful-service) and run the jar with `java -jar movies-restful-service-beyond-java8.jar`
