package consumer.service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import consumer.model.Comment;
import consumer.model.Story;
import consumer.repository.CommentRepository;
import consumer.repository.IdAndTitle;
import consumer.repository.StoryRepository;

@Service
public class StoryService {

    private static final Logger logger = LoggerFactory.getLogger(StoryService.class);

    private final StoryRepository storyRepository;
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;

    public StoryService(StoryRepository storyRepository, CommentRepository commentRepository, MongoTemplate mongoTemplate) {
        this.storyRepository = storyRepository;
        this.commentRepository = commentRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public List<IdAndTitle> findAllStoryIdAndTitlePairs() {
        return storyRepository.findAllBy();
    }

    public String getStoryJsonWithRelatedData(Long externalId) {
        String json;
        if (storyRepository.existsByExternalId(externalId)) {
            // This is a Story. Fetch it with kids (comments).

            // It looks reasonable to use MongoDB's $graphLookup for the task, because we need to find
            // kids recursively based on kids array, containing children (external) IDs
            TypedAggregation<Story> agg = Aggregation.newAggregation(Story.class,
                    match(Criteria.where("externalId").is(externalId)),
                    Aggregation.graphLookup("comment")
                            .startWith("$kids")
                            .connectFrom("kids")
                            .connectTo("externalId")
                            .depthField("depth")
                            .as("children"));
            AggregationResults<StoryModel> result = mongoTemplate.aggregate(agg, StoryModel.class);

            StoryModel storyModel = result.getUniqueMappedResult(); // is not expected to be null, because of the
                                                                    // condition of the `if` above.

            return toJson(storyModel);
        } else if (commentRepository.existsByExternalId(externalId)) {
            // In case if externalId corresponds to comment, rather than story, we need to find
            // the story, this comment corresponds to, first and then this story externalId can
            // be used like in the case above (i.e. it makes sense to (recursively) call this
            // method).
            TypedAggregation<Comment> agg = Aggregation.newAggregation(Comment.class,
                     match(Criteria.where("externalId").in(externalId)),
                    Aggregation.graphLookup("comment")
                            .startWith("parentExternalId")
                            .connectFrom("parentExternalId")
                            .connectTo("externalId")
                            .depthField("depth")
                            .as("ancestors"));
            AggregationResults<CommentModel> result = mongoTemplate.aggregate(agg, CommentModel.class);
            CommentModel commentModel = result.getUniqueMappedResult(); // should be null due to condition of the `if` above

            CommentModel outermostComment;
            if (commentModel.getAncestors() == null || commentModel.getAncestors().isEmpty()) {
                // In this case, the externalParentId corresponds to the story
                outermostComment = commentModel;
            } else {
                // Otherwise, we need to find the outermost comment, which has the largest depth value
                // (because we start from the given comment and proceed up the chain to its parents).
                outermostComment = commentModel.getAncestors().get(0);
                for (int i = 1; i < commentModel.getAncestors().size(); i++) {
                    CommentModel ancestor = commentModel.ancestors.get(i);
                    if (ancestor.getDepth() > outermostComment.getDepth()) {
                        outermostComment = ancestor;
                    }
                }
            }

            return getStoryJsonWithRelatedData(outermostComment.getParentExternalId());
        } else {
            logger.info("Entity with id '{}' not present", externalId);
            json = null;
        }
        return json;
    }

    private String toJson(StoryModel storyModel) {
        if (storyModel == null) {
            throw new NullPointerException("storyModel should not be null");
        }

        ObjectMapper mapper = new ObjectMapper();

        if (storyModel.getChildren() == null) {
            // If the story has no comments - no additional actions are required.
        } else {

            // If there are comments to the story - there is a need to build a tree of comments, because the
            // children are present in flattened list, and not in a tree, after $graphLookup execution.

            // First, let's map all child comments
            Map<Long, CommentModel> commentsMap = new HashMap<>();
            for (CommentModel commentModel : storyModel.getChildren()) {
                commentsMap.put(commentModel.getExternalId(), commentModel);
            }

            // firstChildren holds first generation of children
            List<CommentModel> firstChildren = new ArrayList<>(storyModel.getChildren().size());
            for (CommentModel commentModel : storyModel.getChildren()) {
                // add actual children comments to each comment, based on kids array, containing kids' externalIds
                if (commentModel.getDepth() == 0) {
                    firstChildren.add(commentModel);
                }

                if (commentModel.getKids() == null) {
                    continue;
                }

                commentModel.getKids().forEach(kidId -> {
                    CommentModel kid = commentsMap.get(kidId);
                    if (kid != null) {
                        commentModel.addChild(kid);
                    }
                });
            }

            if (!firstChildren.isEmpty()) {
                storyModel.setChildren(firstChildren);
            }
        }

        try {
            // finally, convert the story, with related comments, to json string
            return mapper.writeValueAsString(storyModel);
        } catch (JsonProcessingException e) {
            // quazi-handling :)
            throw new RuntimeException(e);
        }
    }

    // Simple model object, describing story data returned from MongoDB's graphLookup
    private static class StoryModel {
        private Long externalId;
        private String by;
        private Date date;
        private Integer score;
        private String title;
        private String url;
        private List<CommentModel> children;
        @JsonIgnore
        private List<Long> kids;

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

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
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

        public List<CommentModel> getChildren() {
            return children;
        }

        public void setChildren(List<CommentModel> children) {
            this.children = children;
        }

        public List<Long> getKids() {
            return kids;
        }

        public void setKids(List<Long> kids) {
            this.kids = kids;
        }
    }

    // Simple model object, describing comment data returned from MongoDB's graphLookup
    private static class CommentModel {
        private Long externalId;
        private String by;
        private Long parentExternalId;
        private Date date;
        private String text;
        @JsonIgnore
        private List<Long> kids;
        @JsonIgnore
        transient private Integer depth;
        @JsonManagedReference
        private List<CommentModel> children;
        @JsonIgnore
        private List<CommentModel> ancestors;

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

        public List<Long> getKids() {
            return kids;
        }

        public void setKids(List<Long> kids) {
            this.kids = kids;
        }

        public Integer getDepth() {
            return depth;
        }

        public void setDepth(Integer depth) {
            this.depth = depth;
        }

        private List<CommentModel> getChildren() {
            if (children == null) {
                children = new ArrayList<>(kids != null ? kids.size() : 0);
            }
            return children;
        }

        public void setChildren(List<CommentModel> children) {
            this.children = children;
        }

        public void addChild(CommentModel child) {
            getChildren().add(child);
        }

        public List<CommentModel> getAncestors() {
            return ancestors;
        }

        public void setAncestors(List<CommentModel> ancestors) {
            this.ancestors = ancestors;
        }
    }
}
