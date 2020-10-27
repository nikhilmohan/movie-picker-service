package com.nikhilm.hourglass.moviepickerservice.repositories;

import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface MovieRepository extends ReactiveMongoRepository<Movie, String> {

    @Query(value = "{used : {$ne : ?0}}", count = true)
    public Mono<Long> findCountByUnused(boolean used);
}
