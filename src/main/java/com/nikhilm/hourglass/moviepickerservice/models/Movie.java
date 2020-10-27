package com.nikhilm.hourglass.moviepickerservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "movies")
public class Movie {
    @Id
    private String id;
    private String title;
    private String year;
    private String rated;
    private String runtime;
    private String genre;
    private String plot;
    private String poster;
    private String imdbRating;
    private String imdbVotes;
    private boolean used;
    private boolean isFavourite = false;
}
