package com.nikhilm.hourglass.moviepickerservice.repositories;

import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.models.MovieFeed;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface MovieFeedRepository extends ReactiveMongoRepository<MovieFeed, String> {


    Mono<MovieFeed> findByFeedDate(LocalDate now);


}
