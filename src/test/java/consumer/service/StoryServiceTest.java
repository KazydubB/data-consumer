package consumer.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import consumer.model.Comment;
import consumer.model.Story;
import consumer.repository.CommentRepository;
import consumer.repository.StoryRepository;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StoryServiceTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // enable non-static @BeforeAll
public class StoryServiceTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StoryService storyService;

    @BeforeAll
    public void setupData() {
        List<Story> stories = new ArrayList<>(2);
        stories.add(createStory(1L, "Title 1", Arrays.asList(2L, 3L)));

        List<Comment> comments = new ArrayList<>(3);
        comments.add(createComment(2L, 1L, "Comment 1", Collections.singletonList(7L)));
        comments.add(createComment(3L, 1L, "Comment 2", null));
        comments.add(createComment(7L, 2L, "Comment 3", null));

        stories.add(createStory(4L, "Title 2", Collections.singletonList(9L)));
        comments.add(createComment(9L, 4L, "Comment 4", null));

        comments.add(createComment(120L, 17L, "Comment with absent parent", Arrays.asList(139L, 141L)));

        stories.add(createStory(21L, "Title 3", Arrays.asList(41L, 45L, 179L)));
        comments.add(createComment(41L, 21L, "Comment 5", null));
        comments.add(createComment(45L, 21L, "Comment 5", Collections.singletonList(67L)));

        storyRepository.saveAll(stories);
        commentRepository.saveAll(comments);
    }

    @Test
    public void testGetStoryJsonWithRelatedData_StoryExternalId() {
        String expected = "{\"externalId\":1,\"by\":null,\"date\":null,\"score\":null,\"title\":\"Title 1\"," +
                "\"url\":null,\"children\":[{\"externalId\":3,\"by\":null,\"parentExternalId\":1,\"date\":null," +
                "\"text\":\"Comment 2\",\"children\":null},{\"externalId\":2,\"by\":null,\"parentExternalId\":1," +
                "\"date\":null,\"text\":\"Comment 1\",\"children\":[{\"externalId\":7,\"by\":null,\"parentExternalId\"" +
                ":2,\"date\":null,\"text\":\"Comment 3\",\"children\":null}]}]}";

        String actual = storyService.getStoryJsonWithRelatedData(1L);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetStoryJsonWithRelatedData_CommentExternalId() {
        String expected = "{\"externalId\":1,\"by\":null,\"date\":null,\"score\":null,\"title\":\"Title 1\"," +
                "\"url\":null,\"children\":[{\"externalId\":3,\"by\":null,\"parentExternalId\":1,\"date\":null," +
                "\"text\":\"Comment 2\",\"children\":null},{\"externalId\":2,\"by\":null,\"parentExternalId\":1," +
                "\"date\":null,\"text\":\"Comment 1\",\"children\":[{\"externalId\":7,\"by\":null,\"parentExternalId\"" +
                ":2,\"date\":null,\"text\":\"Comment 3\",\"children\":null}]}]}";

        String actual = storyService.getStoryJsonWithRelatedData(7L);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetStoryJsonWithRelatedData_AbsentExternalId() {
        Long notPresentInDbExtId = 999L;
        String actual = storyService.getStoryJsonWithRelatedData(notPresentInDbExtId);
        Assertions.assertNull(actual);
    }

    @Test
    public void testGetStoryJsonWithRelatedData_CommentExternalIdWithNoParent() {
        String actual = storyService.getStoryJsonWithRelatedData(120L);
        Assertions.assertNull(actual);
    }

    @Test
    public void testGetStoryJsonWithRelatedData_CommentExternalIdWithSomeMissingChildren() {
        String expected = "{\"externalId\":21,\"by\":null,\"date\":null,\"score\":null,\"title\":\"Title 3\"," +
                "\"url\":null,\"children\":[{\"externalId\":45,\"by\":null,\"parentExternalId\":21,\"date\":null," +
                "\"text\":\"Comment 5\",\"children\":null},{\"externalId\":41,\"by\":null,\"parentExternalId\":21," +
                "\"date\":null,\"text\":\"Comment 5\",\"children\":null}]}";

        String actual = storyService.getStoryJsonWithRelatedData(45L);
        Assertions.assertEquals(expected, actual);
    }

    private Story createStory(Long externalId, String title, List<Long> kids) {
        Story story = new Story();
        story.setExternalId(externalId);
        story.setTitle(title);
        story.setKids(kids);
        return story;
    }

    private Comment createComment(Long externalId, Long parentExternalId, String text, List<Long> kids) {
        Comment comment = new Comment();
        comment.setExternalId(externalId);
        comment.setParentExternalId(parentExternalId);
        comment.setText(text);
        comment.setKids(kids);
        return comment;
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public StoryService storyService(StoryRepository storyRepository,
                                         CommentRepository commentRepository, MongoTemplate mongoTemplate) {
            return new StoryService(storyRepository, commentRepository, mongoTemplate);
        }
    }
}
