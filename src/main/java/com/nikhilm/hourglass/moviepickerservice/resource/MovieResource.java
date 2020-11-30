package com.nikhilm.hourglass.moviepickerservice.resource;

import com.nikhilm.hourglass.moviepickerservice.exceptions.MovieException;
import com.nikhilm.hourglass.moviepickerservice.models.*;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieFeedRepository;
import com.nikhilm.hourglass.moviepickerservice.services.MovieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Slf4j
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

    @GetMapping("/movies")
    public Mono<MovieResponse> getMovies(@RequestHeader("user") Optional<String> user)  {

        log.info("Invoked movies " + user.orElse("Not present!"));
        return movieFeedRepository.findByFeedDate(LocalDate.now())
                .flatMap(movieFeed -> {
                    MovieResponse movieResponse = new MovieResponse();
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
                .onErrorMap(throwable -> new MovieException(500, "Internal server error!"));

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
