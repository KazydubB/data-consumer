package consumer.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import consumer.model.Story;

public interface StoryRepository extends MongoRepository<Story, String> {
    List<IdAndTitle> findAllBy();
    boolean existsByExternalId(Long externalId);
}
