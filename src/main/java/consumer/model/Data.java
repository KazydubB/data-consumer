package consumer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import consumer.service.Client;

/**
 * Representation of entity returned from the endpoint. This structure is tricky, because it may represent 2 different
 * logical types of entities, 'story' and 'comment', which are distinguished by {@link #getType()} attribute. Worth noting
 * that while these types have some fields in common (id, by (author), time, etc.) they also have different fields,
 * not present (i.e. always {@code null}) for the other type.
 *
 * To handle the matter, these two types are represented by two (MongoDB) documents for persistence within the program.
 *
 * @see #convertToComment() method to convert {@code this} to {@link Comment}
 * @see #convertToStory() method to convert {@code this} to {@link Story}
 * @see Client#getData(long)
 */
public class Data implements Serializable {

    public enum Type {
        @JsonProperty("comment")
        COMMENT,
        @JsonProperty("story")
        STORY
    }

    private Long id;
    private String by; // author
    private List<Long> kids;
    private Long parent;
    private String text;
    private Date time;
    private Type type;
    private Integer descendants; // integer should do
    private Integer score; // integer should do
    private String title;
    private String url;
    private Boolean dead;

    /**
     * Create {@link Comment comment} representation of the {@code Data}. It is assumed that {@link #getType()} for the
     * {@code Data} returns {@link Type#COMMENT} and is not validated in the method (hence one needs to verify
     * {@code getType() == Data.Type.COMMENT} before the method invocation).
     * @return Comment representation of {@code this} (none of the fields is copied)
     * @see #convertToStory()
     */
    // It is in no way a good practice to do such things as retrieved Data should not even be aware of other types
    // such as persistence documents model. Bit it is present here to make conversion easier and does not require
    // new classes.
    public Comment convertToComment() {
        Comment comment = new Comment();
        comment.setExternalId(id);
        comment.setBy(by);
        comment.setKids(kids); // not copying the List here should be OK
        comment.setParentExternalId(parent);
        comment.setText(text);
        comment.setDate(time);
        comment.setDead(dead);
        return comment;
    }

    /**
     * Create {@link Story comment} representation of the {@code Data}. It is assumed that {@link #getType()} for the
     * {@code Data} returns {@link Type#STORY} and is not validated in the method (hence one needs to verify
     * {@code getType() == Data.Type.STORY} before the method invocation).
     * @return Story representation of {@code this} (none of the fields is copied)
     * @see #convertToComment()
     */
    // It is in no way a good practice to do such things as retrieved Data should not even be aware of other types
    // such as persistence documents model. Bit it is present here to make conversion easier and does not require
    // new classes.
    public Story convertToStory() {
        Story story = new Story();
        story.setExternalId(id);
        story.setBy(by);
        story.setDescendants(descendants);
        story.setDate(time);
        story.setKids(kids); // not copying the List here should be OK
        story.setScore(score);
        story.setTitle(title);
        story.setUrl(url);
        return story;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public List<Long> getKids() {
        return kids;
    }

    public void setKids(List<Long> kids) {
        this.kids = kids;
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Integer getDescendants() {
        return descendants;
    }

    public void setDescendants(Integer descendants) {
        this.descendants = descendants;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getDead() {
        return dead;
    }

    public void setDead(Boolean dead) {
        this.dead = dead;
    }

    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", by='" + by + '\'' +
                ", kids=" + kids +
                ", parent=" + parent +
                ", text='" + text + '\'' +
                ", time=" + time +
                ", type=" + type +
                ", descendants=" + descendants +
                ", score=" + score +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", dead=" + dead +
                '}';
    }

    // Auto-Generated methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Objects.equals(id, data.id) && Objects.equals(by, data.by)
                && Objects.equals(kids, data.kids) && Objects.equals(parent, data.parent)
                && Objects.equals(text, data.text) && Objects.equals(time, data.time)
                && type == data.type && Objects.equals(descendants, data.descendants)
                && Objects.equals(score, data.score)
                && Objects.equals(title, data.title)
                && Objects.equals(url, data.url) && Objects.equals(dead, data.dead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, by, kids, parent, text, time, type, descendants, score, title, url, dead);
    }
}
