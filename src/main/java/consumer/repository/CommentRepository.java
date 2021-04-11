package consumer.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import consumer.model.Comment;

public interface CommentRepository extends MongoRepository<Comment, String> {
    boolean existsByExternalId(Long externalId);
}
