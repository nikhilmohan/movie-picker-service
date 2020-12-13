package com.nikhilm.hourglass.moviepickerservice.resource;

import com.nikhilm.hourglass.moviepickerservice.models.*;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieFeedRepository;
import com.nikhilm.hourglass.moviepickerservice.services.MovieService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Slf4j
@OpenAPIDefinition(
        info = @Info(
                title = "Movie Picker service API",
                version = "1.0",
                description = "API for fetching movie recommendations in hourglass application",
                contact = @Contact(name = "Nikhil Mohan", email = "nikmohan81@gmail.com")
        )
)
public class MovieResource {

    @Autowired
    MovieService movieService;

    @Autowired
    MovieFeedRepository movieFeedRepository;

    ReactiveCircuitBreakerFactory reactiveCircuitBreakerFactory;

    ReactiveCircuitBreaker rcb;

    public MovieResource(ReactiveCircuitBreakerFactory factory) {
        this.reactiveCircuitBreakerFactory = factory;
        rcb = factory.create("movies");
    }

    @Operation(summary = "Fetch movie recommendations for the day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of movies",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MovieResponse.class)) })})
    @GetMapping("/movies")
    public Mono<MovieResponse> getMovies(@RequestHeader(name = "user", required = false) Optional<String> user)  {

        log.info("Invoked movies " + user.orElse("Not present!"));
        MovieResponse movieResponse = new MovieResponse();
        return movieFeedRepository.findByFeedDate(LocalDate.now())
                .flatMap(movieFeed -> {

                    movieResponse.getMovies().addAll(movieFeed.getMovies());
                    movieResponse.setFeedDate(LocalDate.now());
                    if (user.isPresent()) {
                        //retrieve user
                        // check favourites
                        String movieParams = constructParam(movieResponse.getMovies());
                        log.info("movieParams" + movieParams);
                        return rcb.run(movieService.fetchFavourites(user.get(), movieParams),
                                throwable -> {
                                    log.info("Fallback invoked for favourites! " + user.get());
                                    movieResponse.setFavouritesEnabled(false);
                                    return Mono.just(new FavouriteMoviesResponse());
                                })
                                .flatMap(favouriteMoviesResponse -> {
                                    if (favouriteMoviesResponse.getFavouriteMovies().size() > 0 )   {
                                        log.info("user has favourites!");
                                        movieResponse.setMovies(movieResponse.getMovies().stream()
                                                .map(movie -> {
                                                    movie.setFavourite(isFavouriteMovie(movie.getId(), favouriteMoviesResponse));
                                                    return movie;
                                                })
                                                .collect(Collectors.toList()));
                                    }
                                    return Mono.just(movieResponse);

                                });
                    } else return Mono.just(movieResponse);
                })
                .switchIfEmpty(Mono.defer(()->this.getNewFeed()))
                .onErrorReturn(getFallbackResponse());



    }

    private MovieResponse getFallbackResponse() {
        MovieResponse fallbackResponse = new MovieResponse();
        log.info("Getting fallback response");
        fallbackResponse.setMovies(Arrays.asList(new Movie("5f9672a3d20d3846ef8cd39d", "Knife in the Clear Water", "2016", "N/A",
                        "92 min",
                        "Drama",
                        "Ma Zishan (Yang Shengcang) is an aging Muslim farmer, part of the Hui minority. He and his family eke out an existence farming on an arid moonscape in China's northwest Ningxia province. ...",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg", "7.0", "8196", false, false),
                new Movie("5f96741231a5f039c69e7235", "Fear City: A Family-Style Comedy", "1994", "Unrated", "93 min",
                        "Comedy, Horror",
                        "A second-class horror movie has to be shown at Cannes Film Festival, but, before each screening, the projectionist is killed by a mysterious fellow, with hammer and sickle, just as it happens in the film to be shown.", "https://m.media-amazon.com/images/M/MV5BMjVlZDliMWItMDI3ZS00OTc3LTlmZWYtZmMxZDlkMTlhYTNhXkEyXkFqcGdeQXVyMTYzMDM0NTU@._V1_SX300.jpg", "7.5", "8196", false, false),
                new Movie("5f967471cd5fdb67817a8f1c", "The Light in the Forest", "1958", "N/A",
                        "83 min",
                        "Adventure, Drama, Family, Romance, Western",
                        "A young white man who spent his whole life raised by Native Americans is sent to live with his birth family and must learn to fit in with people he was taught to hate.",
                        "https://m.media-amazon.com/images/M/MV5BMGE5YjY5ZjYtZTRkOC00Y2MzLThjZjQtODJhNzcwYzVhMDA1XkEyXkFqcGdeQXVyMTE2NzA0Ng@@._V1_SX300.jpg", "7.2", "83 min", false, false)));
        return fallbackResponse;

    }


    private boolean isFavouriteMovie(String id, FavouriteMoviesResponse favouriteMoviesResponse) {
        return favouriteMoviesResponse.getFavouriteMovies().stream()
                .anyMatch(movie -> movie.getId().equalsIgnoreCase(id));
    }

    private String constructParam(List<Movie> movies) {
       return movies.stream()
               .map(movie -> movie.getId())
               .reduce("",(s, s2) -> s + "," + s2);
    }

    private Mono<MovieResponse> getNewFeed()    {
        MovieFeed movieFeed = new MovieFeed();
        movieFeed.setFeedDate(LocalDate.now());
        return movieService.getMovies()
                .reduce(movieFeed, (feed, movie) -> {
                    feed.getMovies().add(movie);
                    return movieFeed;
                })
                .flatMap(movieFeed1 -> rcb.run(movieFeedRepository.save(movieFeed1),
                        throwable -> Mono.just(movieFeed1)))
                .flatMap(movieFeed1 -> {
                    MovieResponse movieResponse = new MovieResponse();
                    movieResponse.getMovies().addAll(movieFeed1.getMovies());
                    movieResponse.setFeedDate(LocalDate.now());
                    return Mono.just(movieResponse);
                });
    }


}
