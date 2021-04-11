package consumer.rabbit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import consumer.model.Comment;
import consumer.model.Story;
import consumer.repository.CommentRepository;
import consumer.repository.StoryRepository;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ReceiverTest.RabbitMQConfiguration.class)
public class ReceiverTest {

    @Autowired
    private Receiver receiver;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private StoryRepository storyRepository;

    @Test
    public void testCommentsAreSaved() {
        List<Comment> comments = Arrays.asList(
                createComment(1L),
                createComment(2L),
                createComment(3L)
        );
        when(commentRepository.saveAll(anyList())).thenReturn(anyList());

        receiver.processComments(comments);

        verify(commentRepository).saveAll(comments);
    }

    @Test
    public void testStoriesAreSaved() {
        List<Story> stories = Arrays.asList(
                createStory(1L),
                createStory(2L),
                createStory(3L)
        );
        when(storyRepository.saveAll(anyList())).thenReturn(anyList());

        receiver.processStories(stories);

        verify(storyRepository).saveAll(stories);
    }

    private Comment createComment(Long externalId) {
        Comment comment = new Comment();
        comment.setExternalId(externalId);
        return comment;
    }

    private Story createStory(Long externalId) {
        Story story = new Story();
        story.setExternalId(externalId);
        return story;
    }

    @TestConfiguration
    public static class RabbitMQConfiguration {

        @Bean
        public CommentRepository commentRepository() {
            return Mockito.mock(CommentRepository.class);
        }

        @Bean
        public StoryRepository storyRepository() {
            return Mockito.mock(StoryRepository.class);
        }

        @Bean
        public Receiver receiver(CommentRepository commentRepository, StoryRepository storyRepository) {
            return new Receiver(commentRepository, storyRepository);
        }
    }
}
