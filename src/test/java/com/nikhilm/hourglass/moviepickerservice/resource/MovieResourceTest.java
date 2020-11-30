package com.nikhilm.hourglass.moviepickerservice.resource;

import com.nikhilm.hourglass.moviepickerservice.exceptions.ApiError;
import com.nikhilm.hourglass.moviepickerservice.exceptions.MovieException;
import com.nikhilm.hourglass.moviepickerservice.models.FavouriteMoviesResponse;
import com.nikhilm.hourglass.moviepickerservice.models.Movie;
import com.nikhilm.hourglass.moviepickerservice.models.MovieFeed;
import com.nikhilm.hourglass.moviepickerservice.models.MovieResponse;
import com.nikhilm.hourglass.moviepickerservice.repositories.MovieFeedRepository;
import com.nikhilm.hourglass.moviepickerservice.services.MovieService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest
@Slf4j
class MovieResourceTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    MovieService movieService;

    @MockBean
    MovieFeedRepository movieFeedRepository;

    static MovieFeed movieFeed;

    static List<Movie> movies = new ArrayList<>();


    @BeforeAll
    public static void setup()  {
        movieFeed = new MovieFeed();
        movieFeed.setFeedDate(LocalDate.now());
        movieFeed.getMovies().addAll(Arrays.asList(new Movie("5f9672a3d20d3846ef8cd39d", "Knife in the Clear Water", "2016", "N/A",
                        "92 min", "Drama", "Ma Zishan (Yang Shengcang) is an aging Muslim farmer, part of the Hui minority. He and his family eke out an existence farming on an arid moonscape in China's northwest Ningxia province. ...",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "6.5", "99",
                        true,  false),
                new Movie("5f96741231a5f039c69e7235", "Fear City: A Family-Style Comedy", "1994", "N/A",
                        "93 min", "Comedy", "A second-class horror movie has to be shown at Cannes Film Festival, but, before each screening, the projectionist is killed by a mysterious fellow, with hammer and sickle",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "7.6", "8186",
                        true,  false),
                new Movie("5f967471cd5fdb67817a8f1c", "The Light in the Forest", "1958", "Unrated",
                        "83 min", "Adventure, Drama", "A young white man who spent his whole life raised by Native Americans is sent to live with his birth family and must learn to fit in with people he was taught to hate.",
                        "https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg",
                        "6.5", "520",
                        true,  false)));

        movies = movieFeed.getMovies();


    }

    @Test
    public void testGetMovies()  {
        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(movieFeed));
        when(movieService.getMovies()).thenReturn(Flux.fromIterable(movies));
        List<Movie> movieList = webTestClient.get().uri("http://localhost:9030/movies")
                .exchange()
                .expectBody(MovieResponse.class)
                .returnResult()
                .getResponseBody()
                .getMovies();

        assertEquals(3, movieList.size());


    }
    @Test
    public void testGetMoviesWithFavourite()  {
        MovieResourceTest.movieFeed.getMovies().forEach(movie -> {
            movie.setFavourite(true);

        });

        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(movieFeed));
        when(movieService.getMovies()).thenReturn(Flux.fromIterable(movieFeed.getMovies()));
        List<Movie> movieList = webTestClient.get().uri("http://localhost:9030/movies")
                .exchange()
                .expectBody(MovieResponse.class)
                .returnResult()
                .getResponseBody()
                .getMovies();

        assertEquals(3, movieList.stream().filter(movie -> movie.isFavourite()).count());


    }
    @Test
    public void testGetMoviesWithNewFeed()  {

        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.empty());
        when(movieFeedRepository.save(any(MovieFeed.class))).thenReturn(Mono.just(movieFeed));
        when(movieService.getMovies()).thenReturn(Flux.fromIterable(movieFeed.getMovies()));
        List<Movie> movieList = webTestClient.get().uri("http://localhost:9030/movies")
                .exchange()
                .expectBody(MovieResponse.class)
                .returnResult()
                .getResponseBody()
                .getMovies();

        assertEquals(3, movieList.size());


    }
    @Test
    public void testGetMoviesForUser()  {

        FavouriteMoviesResponse favouriteMoviesResponse = new FavouriteMoviesResponse();
        favouriteMoviesResponse.setUserId("abc");
        Movie favMovie = new Movie();
        favMovie.setId("5f96741231a5f039c69e7235");
        favMovie.setTitle("Fear City: A Family-Style Comedy");
        favMovie.setPlot("A second-class horror movie has to be shown at Cannes Film Festival, but, before each screening, the projectionist is killed by a mysterious fellow, with hammer and sickle");
        favMovie.setPoster("https://m.media-amazon.com/images/M/MV5BMTg1MmUwOGYtZTQ2ZS00ODgzLWFlODEtNDAxY2U4YWE0NDczXkEyXkFqcGdeQXVyNzI1NzMxNzM@._V1_SX300.jpg");
        favouriteMoviesResponse.getFavouriteMovies().add(favMovie);

        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(movieFeed));
        when(movieService.fetchFavourites(eq("abc"), anyString())).thenReturn(Mono.just(favouriteMoviesResponse));
        MovieResponse response = webTestClient.get().uri("http://localhost:9030/movies")
                .header("user", "abc")
                .exchange()
                .expectBody(MovieResponse.class)
                .returnResult()
                .getResponseBody();

        log.info(" val " + response.getMovies().stream().filter(movie -> movie.isFavourite()).findAny().get().getId());
        assertEquals(1, response.getMovies().stream().filter(movie -> movie.isFavourite()).count());
        assertEquals("5f96741231a5f039c69e7235", response.getMovies().stream()
                .filter(movie -> movie.isFavourite()).findAny().get().getId());


    }
    @Test
    public void testGetMoviesWithDBDown() {
        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenThrow(new RuntimeException());
        webTestClient.get().uri("http://localhost:9030/movies")
                .exchange()
                .expectStatus().is5xxServerError();
    }
    @Test
    public void testGetMoviesWithServiceError() {
        when(movieService.getMovies()).thenThrow(new MovieException(500, "Internal server error!"));
        webTestClient.get().uri("http://localhost:9030/movies")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    public void testFavouritesAccessErrorInGetMovies() {
        when(movieFeedRepository.findByFeedDate(LocalDate.now())).thenReturn(Mono.just(movieFeed));
        when(movieService.fetchFavourites(eq("abc"), anyString())).thenReturn(Mono.error(new RuntimeException("Favourites service error!")));

        MovieResponse response =  webTestClient.get().uri("http://localhost:9030/movies")
                .header("user", "abc")
                .exchange()
                .expectBody(MovieResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(false, response.isFavouritesEnabled());
    }


}