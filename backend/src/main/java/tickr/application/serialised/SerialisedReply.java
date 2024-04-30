package tickr.application.serialised;

import tickr.application.serialised.SerialisedReaction;

import java.util.List;

public class SerialisedReply {
    public String replyId;
    public String authorId;
    public String text;
    public String time;
    public List<SerialisedReaction> reactions;

    public SerialisedReply () {}

    public SerialisedReply (String replyId, String authorId, String text, String time, List<SerialisedReaction> reactions) {
        this.replyId = replyId;
        this.authorId = authorId;
        this.text = text;
        this.time = time;
        this.reactions = reactions;
    }
}
