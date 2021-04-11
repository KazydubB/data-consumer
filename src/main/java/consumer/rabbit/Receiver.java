package consumer.rabbit;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import consumer.config.RabbitMQConfiguration;
import consumer.model.Comment;
import consumer.model.Story;
import consumer.repository.CommentRepository;
import consumer.repository.StoryRepository;

/**
 * Receiver is responsible for processing batches of Stories and Comments entities sent to RabbitMQ's queues.
 * During the processing the entities are persisted.
 */
@Component
public class Receiver {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;

    public Receiver(CommentRepository commentRepository, StoryRepository storyRepository) {
        this.commentRepository = commentRepository;
        this.storyRepository = storyRepository;
    }

    @RabbitListener(queues = RabbitMQConfiguration.QUEUE_COMMENT)
    public void processComments(List<Comment> comments) {
        logger.info("Processing {} comment(s)", comments.size());
        commentRepository.saveAll(comments);
    }

    @RabbitListener(queues = RabbitMQConfiguration.QUEUE_STORY)
    public void processStories(List<Story> stories) {
        logger.info("Processing {} stories", stories.size());
        storyRepository.saveAll(stories);
    }
}
