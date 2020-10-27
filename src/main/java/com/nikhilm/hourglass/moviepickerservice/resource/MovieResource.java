package com.nikhilm.hourglass.moviepickerservice.resource;

import com.nikhilm.hourglass.moviepickerservice.models.FavouriteMoviesResponse;
import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.models.MovieFeed;
import com.nikhilm.hourglass.moviepickerservice.models.MovieResponse;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieFeedRepository;
import com.nikhilm.hourglass.moviepickerservice.services.MovieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class MovieResource {

    @Autowired
    MovieService movieService;

    @Autowired
    MovieFeedRepository movieFeedRepository;

    @GetMapping("/movies")
    public Mono<MovieResponse> getMovies()  {
        return movieFeedRepository.findByFeedDate(LocalDate.now())
                .flatMap(movieFeed -> {
                    MovieResponse movieResponse = new MovieResponse();
                    movieResponse.getMovies().addAll(movieFeed.getMovies());
                    movieResponse.setFeedDate(LocalDate.now());
                    //retrieve user
                    // check favourites
                    String movieParams = constructParam(movieResponse.getMovies());
                    log.info("movieParams" + movieParams);
                    WebClient client = WebClient.create("http://localhost:9040/favourites/user/1234/movies");
                    return client.get().uri("?ids="+movieParams)
                            .retrieve()
                            .bodyToMono(FavouriteMoviesResponse.class)
                            .flatMap(favouriteMoviesResponse -> {
                                movieResponse.setMovies(movieResponse.getMovies().stream()
                                        .map(movie -> {
                                            movie.setFavourite(isFavouriteMovie(movie.getId(),favouriteMoviesResponse));
                                            return movie;
                                        })
                                        .collect(Collectors.toList()));

                                return Mono.just(movieResponse);

                                });
                            })
                .switchIfEmpty(Mono.defer(()->this.getNewFeed()));

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
                .flatMap(movieFeedRepository::save)
                .flatMap(movieFeed1 -> {
                    MovieResponse movieResponse = new MovieResponse();
                    movieResponse.getMovies().addAll(movieFeed1.getMovies());
                    movieResponse.setFeedDate(LocalDate.now());
                    return Mono.just(movieResponse);
                });
    }

}
