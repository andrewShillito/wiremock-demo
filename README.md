# wiremock-demo
A WireMock testing demo built using Java 17 and gradle.

# Primary content

- [src/test/java/com/learnwiremock/service/MoviesRestClientWireMockTest.java](https://github.com/andrewShillito/wiremock-demo/blob/3d32820176ac4f4a6b7af937959c893a42b1872e/src/test/java/com/learnwiremock/service/MoviesRestClientWireMockTest.java) A wiremock based test class for a REST API.
- [src/test/java/com/learnwiremock/service/MoviesRestClientTest.java](https://github.com/andrewShillito/wiremock-demo/blob/fd982f3b8ae5f49865956e27efe2c80768690a32/src/test/java/com/learnwiremock/service/MoviesRestClientTest.java) A test class which runs against an example REST API. See [# Optional: Running the example REST API](#Optional: Running the example REST API) below on running the example REST API this relies on.

# Generate code coverage report using jacocoTestReport
`./gradlew build jacocoTestReport`

# Optional: Running the example REST API

Clone or download the `movies-restful-service-beyond-java8.jar` from [movies-restful-service](https://github.com/dilipsundarraj1/wiremock-for-java-developers/tree/master/movies-restful-service) and run the jar with `java -jar movies-restful-service-beyond-java8.jar`
