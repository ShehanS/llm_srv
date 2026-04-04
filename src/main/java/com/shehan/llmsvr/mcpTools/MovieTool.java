package com.shehan.llmsvr.mcpTools;

import com.shehan.llmsvr.dtos.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MovieTool {

    @Tool(name = "move_tool",  description = "Record or provide details about a specific movie")
    public String recordMovieDetails(
            @ToolParam(description = "The title of the movie") String title,
            @ToolParam(description = "The year the movie was released") int year,
            @ToolParam(description = "The director of the movie") String director,
            @ToolParam(description = "The movie's rating out of 10") double rating
    ) {
        Movie movie = new Movie(title, year, director, rating);
        log.info("Received structured movie: {}",  movie);

        return "Successfully recorded " + title;
    }
}
