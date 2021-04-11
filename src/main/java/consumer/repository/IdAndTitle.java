package consumer.repository;

// Special projection model containing story externalId and title fields only
public interface IdAndTitle {
    Long getExternalId();
    String getTitle();
}
