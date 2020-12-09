package com.nikhilm.hourglass.moviepickerservice.services;

import com.nikhilm.hourglass.moviepickerservice.exceptions.MovieException;
import com.nikhilm.hourglass.moviepickerservice.models.FavouriteMoviesResponse;
import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MovieService {

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    WebClient webClient;


    private int movieFeedSize;

    @Value("${service.url.gateway}")
    private String serviceGatewayUrl;


    @Value("${movieFeedSize}")
    public void setMovieFeedSize(int movieFeedSize)  {
        this.movieFeedSize = movieFeedSize;
    }

    public int getMovieFeedSize()  {
        return this.movieFeedSize;
    }

    public Flux<Movie> getMovies() {

        List<Movie> pickedMovies = new ArrayList<>();
        return movieRepository.findCountByUnused(true)
                .flatMapMany(this::generateRandom)
                .flatMap(aLong -> movieRepository.findAll()
                    .filter(movie -> !movie.isUsed())
                    .skip(aLong)
                    .take(1)
                ).reduce(pickedMovies, (movies, movie1) -> {
                     movies.add(movie1);
                     return movies;
                })
                .thenMany(Flux.fromIterable(pickedMovies))
                .flatMap(movie -> {
                    movie.setUsed(true);
                    return movieRepository.save(movie);
                })
                .switchIfEmpty(Mono.defer(()->Mono.error(new MovieException(500, "Internal server error!"))));

    }

    private Mono<ClientResponse> callFavourites(String userId, String params)    {
        return webClient.get()
                .uri("http://" + serviceGatewayUrl + ":9900/favourites-service/favourites/user/" + userId + "/movies?ids="+params)
                .exchange();

    }
    public Mono<FavouriteMoviesResponse> fetchFavourites(String userId, String params)   {
        log.info("Fetching favourites for user " + userId);
        return callFavourites(userId, params)
                .flatMap(clientResponse -> {
                    if (!clientResponse.statusCode().is2xxSuccessful())  {
                        return Mono.error(new RuntimeException("Favourites service error!"));
                    }
                    else return clientResponse.bodyToMono(FavouriteMoviesResponse.class);
                });
    }
    private Flux<Long> generateRandom(Long count)    {
        log.info("Movie feed size : " + getMovieFeedSize() + " " + count);
        if (count < getMovieFeedSize()) {
            throw new MovieException(500, "Insufficient number of entries!");
        }
        List<Long> randomIndices = new ArrayList<>();
        long offset = (count - 1) / getMovieFeedSize();
        long lb = 0;
        long ub = 0;
        for ( int i = 0; i < getMovieFeedSize(); i++) {
            ub = (lb + offset >= count) ? count - 1 : lb + offset;
            long idx = getRandomNumberinRange(lb, ub);
            log.info("idx " + idx);
            randomIndices.add(idx);
            lb = ub + 1;
        }
        return Flux.fromIterable(randomIndices);
    }
    public long getRandomNumberinRange(long min, long max) {
        return new SecureRandom().longs(min, max + 1).findFirst().getAsLong();
    }

}
