package com.nikhilm.hourglass.moviepickerservice.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovieException extends RuntimeException{
    private int status;
    private String message;
}
