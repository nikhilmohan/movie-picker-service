package com.nikhilm.hourglass.moviepickerservice.services;

import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MovieService {

    @Autowired
    MovieRepository movieRepository;

    @Value("${movieFeedSize}")
    private int movieFeedSize;

    public Flux<Movie> getMovies() {
        return movieRepository.findCountByUnused(true)
                .flatMapMany(this::generateRandom)
                .flatMap(aLong -> movieRepository.findAll()
                    .filter(movie -> !movie.isUsed())
                    .skip(aLong)
                    .take(1)
                ).flatMap(movie -> {
                    movie.setUsed(true);
                    return movieRepository.save(movie);
                });
    }
    private Flux<Long> generateRandom(Long count)    {
        List<Long> randomIndices = new ArrayList<>();
        long offset = (count - 1) / movieFeedSize;
        long lb = 0;
        long ub = 0;
        for ( int i = 0; i < movieFeedSize; i++) {
            ub = (lb + offset >= count) ? count - 1 : lb + offset;
            long idx = getRandomNumberinRange(lb, ub);
            log.info("idx " + idx);
            randomIndices.add(idx);
            lb = ub + 1;
        }
        return Flux.fromIterable(randomIndices);
    }
    private long getRandomNumberinRange(long min, long max) {
        return (long) ((Math.random() * (max - min)) + min);
    }

}
