package com.nikhilm.hourglass.moviepickerservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhilm.hourglass.moviepickerservice.models.FavouriteMoviesResponse;
import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class MovieServiceTest {

    @Mock
    MovieRepository movieRepository;

    @InjectMocks
    MovieService movieService = new MovieService();

    @Mock
    WebClient webClient;



    List<Movie> movieList = new ArrayList<>();

    @BeforeEach
    public void setup() {

        movieList.addAll(Arrays.asList(new Movie("5f9672a3d20d3846ef8cd39d", "Knife in the Clear Water", "2016", "N/A",
                        "92 min", "Drama", "Ma Zishan (Yang Shengcang) is an aging Muslim farmer, part of the Hui minority. He and his family eke out an existence farming on an arid moonscape in China's northwest Ningxia province. ...",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "6.5", "99",
                        false,  false),
                new Movie("5f96741231a5f039c69e7235", "Fear City: A Family-Style Comedy", "1994", "N/A",
                        "93 min", "Comedy", "A second-class horror movie has to be shown at Cannes Film Festival, but, before each screening, the projectionist is killed by a mysterious fellow, with hammer and sickle",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "7.6", "8186",
                        false,  false),
                new Movie("5f967471cd5fdb67817a8f1c", "The Light in the Forest", "1958", "Unrated",
                        "83 min", "Adventure, Drama", "A young white man who spent his whole life raised by Native Americans is sent to live with his birth family and must learn to fit in with people he was taught to hate.",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "6.5", "520",
                        false,  false)));

                movieService.setMovieFeedSize(3);

    }

    @Test
    void getMovies() {

        movieList.forEach(movie -> log.info("Moviename: " + movie.getTitle()));
        when(movieRepository.findCountByUnused(true)).thenReturn(Mono.just(3L));
        when(movieRepository.findAll()).thenReturn(Flux.fromIterable(movieList));
        when(movieRepository.save(any(Movie.class))).thenReturn(Mono.just(new Movie()));

        StepVerifier.create(movieService.getMovies())
                .expectSubscription()
                .expectNextCount(3L)
                .verifyComplete();
    }
    @Test
    public void testGetMoviesWithNoMovies() {
        when(movieRepository.findCountByUnused(true)).thenReturn(Mono.just(0L));
        StepVerifier.create(movieService.getMovies())
                .expectSubscription()
                .expectErrorMessage("Insufficient number of entries!")
                .verify();
    }
    @Test
    public void testGetMoviesWithNoFavouritesService() {

        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();
        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString()))
                .thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));
        StepVerifier.create(movieService.fetchFavourites("abc", ""))
                .expectSubscription()
                .expectError()
                .verify();
    }
    @Test
    public void testGetMoviesWithFavouritesService() {

        Movie favMovie = new Movie();
        favMovie.setId("5f96741231a5f039c69e7235");
        favMovie.setTitle("Fear City: A Family-Style Comedy");
        favMovie.setPlot("A second-class horror movie has to be shown at Cannes Film Festival, but, before each screening, the projectionist is killed by a mysterious fellow, with hammer and sickle");
        favMovie.setPoster("https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg");
        FavouriteMoviesResponse favouriteMoviesResponse = new FavouriteMoviesResponse();
        favouriteMoviesResponse.setUserId("abc");
        favouriteMoviesResponse.getFavouriteMovies().add(favMovie);
        ObjectMapper mapper = new ObjectMapper();
        String body = "";
        try {
            body =  mapper.writeValueAsString(favouriteMoviesResponse);
        } catch (JsonProcessingException e) {
            log.error("Cannot parse");
        }


        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body).build();

        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(anyString()))
                .thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.exchange()).thenReturn(Mono.just(clientResponse));
        StepVerifier.create(movieService.fetchFavourites("abc",
                "5f9672a3d20d3846ef8cd39d,5f96741231a5f039c69e7235,5f967471cd5fdb67817a8f1c"))
                .expectSubscription()
                .expectNextMatches(response -> response.getFavouriteMovies().size() == 1
                    && response.getFavouriteMovies().stream().anyMatch(movie -> movie.getTitle()
                        .equalsIgnoreCase("Fear City: A Family-Style Comedy")))
                .verifyComplete();
    }







}