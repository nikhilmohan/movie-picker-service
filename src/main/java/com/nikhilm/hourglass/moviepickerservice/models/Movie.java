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
    @JsonProperty("certification")
    private String rated;
    @JsonProperty("runningTime")
    private String runtime;
    private String genre;
    private String plot;
    private String poster;
    @JsonProperty("ratings")
    private String imdbRating;
    private String imdbVotes;
    private boolean used;
    @JsonProperty("favourite")
    private boolean isFavourite = false;
}
