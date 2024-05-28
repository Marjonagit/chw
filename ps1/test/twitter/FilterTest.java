package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.*;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy for Filter methods:
     *
     * writtenBy:
     *  - Multiple tweets:
     *      - One tweet by the given user
     *      - Multiple tweets by the given user
     *      - No tweets by the given user
     *  - Case sensitivity of the username
     *  - Empty list of tweets
     *
     * inTimespan:
     *  - Multiple tweets:
     *      - All tweets within timespan
     *      - Some tweets within timespan
     *      - No tweets within timespan
     *  - Timespan boundaries:
     *      - Start time inclusive
     *      - End time inclusive
     *  - Empty list of tweets
     *
     * containing:
     *  - Tweets:
     *      - Contains all words
     *      - Contains some words
     *      - Contains none of the words
     *      - Case sensitivity of words
     *  - Words list:
     *      - Empty list of words
     *      - Single word
     *      - Multiple words
     *  - Empty list of tweets
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    
    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }
    
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");
        
        assertEquals("expected singleton list", 1, writtenBy.size());
        assertTrue("expected list to contain tweet", writtenBy.contains(tweet1));
    }
    
    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");
        
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(testStart, testEnd));
        
        assertFalse("expected non-empty list", inTimespan.isEmpty());
        assertTrue("expected list to contain tweets", inTimespan.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, inTimespan.indexOf(tweet1));
    }

    @Test
    public void testContainingWordsInTweets() {
        List<String> words = Arrays.asList("rivest", "talk");
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), words);

        assertFalse("expected non-empty list", containing.isEmpty());
        assertTrue("expected list to contain tweet1", containing.contains(tweet1));
        assertTrue("expected list to contain tweet2", containing.contains(tweet2));
    }

    private long idCounter = 0;
    private final List<Character> space = List.of(' ', '\t', '\n');

    private long getId() {
        return idCounter++;
    }

    String wordGenerator() {
        var random = new Random();
        int len = random.nextInt(5) + 3; // words of length between 3 and 7
        StringBuilder word = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = (char) ('a' + random.nextInt(26));
            word.append(c);
        }
        return word.toString();
    }

    Tweet generateTweet(List<String> words) {
        var random = new Random();
        StringBuilder sb = new StringBuilder();

        for (var word : words) {
            sb.append(word);
            sb.append(space.get(random.nextInt(space.size())));
            sb.append(wordGenerator());
            sb.append(space.get(random.nextInt(space.size())));
        }

        return new Tweet(getId(), "Behbudiy", sb.toString(), d1);
    }

    Tweet generateNonTweet(List<String> words) {
        var word = words.stream().max(String::compareTo).get();
        StringBuilder sb = new StringBuilder(word);

        sb.deleteCharAt(sb.length() - 1);
        sb.append("$$");

        return new Tweet(getId(), "Behbudiy", sb.toString(), d1);
    }

    @Test
    public void testContaining() {
        var res = Filter.containing(List.of(), List.of());
        assertTrue("expected empty list", res.isEmpty());

        var words = List.of(wordGenerator(), wordGenerator(), wordGenerator());

        res = Filter.containing(List.of(), words);
        assertTrue("expected empty list", res.isEmpty());
    }

    @Test
    public void testContainingTweets1() {
        var words = List.of(wordGenerator(), wordGenerator(), wordGenerator());
        var tweet = generateTweet(words);

        var res = Filter.containing(List.of(tweet), List.of());
        assertTrue("expected empty list", res.isEmpty());

        res = Filter.containing(List.of(tweet), List.of(words.get(0)));

        assertEquals("expected singleton list", 1, res.size());
        assertEquals("expected tweet id to match", res.get(0).getId(), tweet.getId());
    }

    @Test
    public void testContainingTweetsMany() {
        var words = List.of(wordGenerator(), wordGenerator(), wordGenerator(), wordGenerator(), wordGenerator());

        List<Tweet> tweets = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            tweets.add(generateTweet(words));
        }

        var res = Filter.containing(tweets, words);
        assertEquals("expected all tweets to match", tweets, res);

        res = Filter.containing(tweets, List.of());
        assertTrue("expected empty list", res.isEmpty());
    }

    @Test
    public void testOrderTweets() {
        var words = List.of(wordGenerator(), wordGenerator(), wordGenerator(), wordGenerator(), wordGenerator());

        List<Tweet> tweets = new ArrayList<>();
        for (int j = 0; j < 100; ++j) {
            tweets.add(generateTweet(words));
            tweets.add(generateNonTweet(words));
        }

        for (int i = 0; i < 10; ++i) {
            Collections.shuffle(tweets);
            var res = Filter.containing(tweets, words);

            var tweetIt = tweets.iterator();

            for (var resTweet : res) {
                if (!tweetIt.hasNext()) {
                    fail("Order destroyed");
                }
                while (tweetIt.hasNext()) {
                    if (resTweet.getId() == tweetIt.next().getId()) {
                        break;
                    }
                }
            }
        }
    }

    // Additional tests for more comprehensive coverage
    @Test
    public void testContainingNoMatchingWords() {
        var words = List.of(wordGenerator(), wordGenerator(), wordGenerator());
        var tweet = new Tweet(getId(), "user", "this tweet does not contain any matching words", d1);

        var res = Filter.containing(List.of(tweet), words);
        assertTrue("expected empty list", res.isEmpty());
    }

    @Test
    public void testContainingCaseSensitivity() {
        var words = List.of("RIVEST", "TALK");
        var tweet = new Tweet(getId(), "user", "rivest talk in 30 minutes", d1);

        var res = Filter.containing(List.of(tweet), words);
        assertTrue("expected empty list due to case sensitivity", res.isEmpty());

        words = List.of("rivest", "talk");
        res = Filter.containing(List.of(tweet), words);
        assertEquals("expected singleton list", 1, res.size());
        assertEquals("expected tweet id to match", res.get(0).getId(), tweet.getId());
    }

    @Test
    public void testContainingSingleWord() {
        var words = List.of("rivest");
        var tweet1 = new Tweet(getId(), "user", "rivest talk", d1);
        var tweet2 = new Tweet(getId(), "user", "no mention here", d2);

        var res = Filter.containing(Arrays.asList(tweet1, tweet2), words);
        assertEquals("expected single tweet in list", 1, res.size());
        assertEquals("expected tweet id to match", res.get(0).getId(), tweet1.getId());
    }
}
