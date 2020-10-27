package com.nikhilm.hourglass.moviepickerservice.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "moviefeeds")
public class MovieFeed {
    @Id
    private String id;
    private List<Movie> movies = new ArrayList<>();
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate feedDate;

}
