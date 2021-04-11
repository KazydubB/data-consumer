package consumer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

// MongoDB document, also acts as DTO.
public class Comment implements Serializable {

    @Id
    private String id;
    @Indexed
    private Long externalId;
    private String by;
    private Long parentExternalId; // can be either Story or Comment
    private Date date;
    private String text;
    private Boolean dead;
    private List<Long> kids;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public Long getParentExternalId() {
        return parentExternalId;
    }

    public void setParentExternalId(Long parentExternalId) {
        this.parentExternalId = parentExternalId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getDead() {
        return dead;
    }

    public void setDead(Boolean dead) {
        this.dead = dead;
    }

    public List<Long> getKids() {
        return kids;
    }

    public void setKids(List<Long> kids) {
        this.kids = kids;
    }
}
