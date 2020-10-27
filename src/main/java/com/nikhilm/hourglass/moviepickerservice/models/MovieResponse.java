package com.nikhilm.hourglass.moviepickerservice.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MovieResponse {
    private List<Movie> movies = new ArrayList<>();
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate feedDate;

}
