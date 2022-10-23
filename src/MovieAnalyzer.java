import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MovieAnalyzer {
    public static class movieStatus {
        private String Series_Title;
        private int Released_Year;
        private String Certificate;
        private int Runtime;
        private List<String> Genre;
        private float IMDB_Rating;
        private String Overview;
        private int Meta_score;
        private String Director;
        private List<String> Stars;
        private int Noofvotes;
        private long Gross;

        public int getReleased_Year() {
            return Released_Year;
        }

        public int getRuntime() {
            return Runtime;
        }

        public String getSeries_Title() {
            return Series_Title;
        }

        public int getOverviewLength() {
            return Overview.length();
        }

        public movieStatus(String Series_Title, int Released_Year, String Certificate, int Runtime,
                           String Genre, float IMDB_Rating, String Overview, int Meta_score,
                           String Director, String Star1, String Star2, String Star3, String Star4, int Noofvotes, long Gross) {
            this.Series_Title = Series_Title;
            this.Released_Year = Released_Year;
            this.Certificate = Certificate;
            this.Runtime = Runtime;
            this.Genre = List.of(Genre.split(", "));
            this.IMDB_Rating = IMDB_Rating;
            this.Overview = Overview;
            this.Meta_score = Meta_score;
            this.Director = Director;
            Stars = new ArrayList<>();
            this.Stars.add(Star1);
            this.Stars.add(Star2);
            this.Stars.add(Star3);
            this.Stars.add(Star4);
            this.Noofvotes = Noofvotes;
            this.Gross = Gross;
        }
    }

    public static List<movieStatus> movies;

    public MovieAnalyzer(String dataset_path) {
        try {
            movies = Files.lines(Paths.get(dataset_path), StandardCharsets.UTF_8)
                    .skip(1)
                    .map(l -> l.replace(",", ",<"))
                    .map(l -> l.split(",(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))"))
                    .peek(a -> Arrays.setAll(a, i -> a[i].replace("<", "")))
                    .map(a -> new movieStatus(a[1].replace("\"", ""), Integer.parseInt(a[2]), a[3], Integer.parseInt(a[4].substring(0, a[4].length() - 4)),
                            a[5].replace("\"", ""), Float.parseFloat(a[6]), a[7].replace("\"\"", "|").replace("\"", "").replace("|", "\"\""), Integer.parseInt("0" + a[8]),
                            a[9], a[10], a[11], a[12], a[13], Integer.parseInt(a[14]), Long.parseLong("0" + a[15].replace("\"", "").replace(",", ""))))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        return movies.stream().sorted(Comparator.comparing(movieStatus::getReleased_Year, Comparator.reverseOrder()))
                .collect(Collectors.groupingBy(movieStatus::getReleased_Year, LinkedHashMap::new, Collectors.reducing(0, movies -> 1, Integer::sum)));
    }

    public Map<String, Integer> getMovieCountByGenre() {
        List<String> count = new ArrayList<>();
        for (movieStatus movieStatus : movies) {
            count.addAll(movieStatus.Genre);
        }
        Map<String, Integer> genreCount = count.stream().collect(Collectors.toMap(key -> key, value -> 1, Integer::sum, LinkedHashMap::new));
        Map<String, Integer> ans = new LinkedHashMap<>();
        genreCount.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .forEach(a -> ans.put(a.getKey(), a.getValue()));
        return ans;
    }

    public Map<List<String>, Integer> getCoStarCount() {
        List<List<String>> count = new ArrayList<>();
        for (movieStatus movieStatus : movies) {
            for (int i = 0; i < 3; i++) {
                for (int j = i + 1; j < 4; j++) {
                    List<String> costar = new ArrayList<>();
                    costar.add(movieStatus.Stars.get(i));
                    costar.add(movieStatus.Stars.get(j));
                    costar.sort(String::compareTo);
                    count.add(costar);
                }
            }
        }
        return count.stream().collect(Collectors.toMap(key -> key, value -> 1, Integer::sum, LinkedHashMap::new));
    }

    public List<String> getTopMovies(int top_k, String by) {
        List<String> topMovies = new ArrayList<>();
        if (by.equals("runtime")) {
            movies.stream().sorted(Comparator.comparing(movieStatus::getRuntime, Comparator.reverseOrder()).thenComparing(movieStatus::getSeries_Title))
                    .limit(top_k).forEach(a -> topMovies.add(a.Series_Title));
        } else if (by.equals("overview")) {
            movies.stream().sorted(Comparator.comparing(movieStatus::getOverviewLength, Comparator.reverseOrder()).thenComparing(movieStatus::getSeries_Title))
                    .limit(top_k).forEach(a -> topMovies.add(a.Series_Title));
        }
        return topMovies;
    }

    public List<String> getTopStars(int top_k, String by) {
        List<String> topStars = new ArrayList<>();
        if (by.equals("rating")) {
            Map<String, Double> starRating = new HashMap<>();
            Map<String, Integer> starCount = new HashMap<>();
            movies.forEach(a -> {
                if (a.IMDB_Rating != 0) {
                    for (String star : a.Stars) {
                        starRating.put(star, starRating.getOrDefault(star, (double) 0) + a.IMDB_Rating);
                        starCount.put(star, starCount.getOrDefault(star, 0) + 1);
                    }
                }
            });
            starRating.replaceAll((k, v) -> starRating.get(k) / (double) starCount.get(k));
            starRating.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                    .limit(top_k).forEach(entry -> topStars.add(entry.getKey()));
        } else if (by.equals("gross")) {
            Map<String, Long> starGross = new HashMap<>();
            Map<String, Integer> starCount = new HashMap<>();
            movies.forEach(a -> {
                if (a.Gross != 0) {
                    for (String star : a.Stars) {
                        starGross.put(star, starGross.getOrDefault(star, (long) 0) + a.Gross);
                        starCount.put(star, starCount.getOrDefault(star, 0) + 1);
                    }
                }
            });
            starGross.replaceAll((k, v) -> starGross.get(k) / (long) starCount.get(k));
            starGross.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                    .limit(top_k).forEach(entry -> topStars.add(entry.getKey()));
        }
        return topStars;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        List<String> searchMovies = new ArrayList<>();
        movies.stream().filter(a -> a.Genre.contains(genre) && a.IMDB_Rating >= min_rating && a.Runtime <= max_runtime)
                .sorted(Comparator.comparing(a -> a.Series_Title)).forEach(a -> searchMovies.add(a.Series_Title));
        return searchMovies;
    }

}