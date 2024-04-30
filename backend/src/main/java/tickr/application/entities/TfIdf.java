package tickr.application.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tf_idf")
public class TfIdf {
    @EmbeddedId
    private TermId termId;

    @Column(name = "term_freq")
    private double termFreq;

    @Column(name = "document_count")
    private int documentCount;

    public TfIdf () {

    }

    public TfIdf (DocumentTerm term, Event event, int termCount) {
        this.termId = new TermId(term, event);
        this.termFreq = 1 + Math.log10(termCount);
        this.documentCount = term.getTermCount();
    }

    public double getTfIdf (int documentNum) {
        documentCount = termId.getTerm().getTermCount();
        return termFreq * Math.log10((double)documentNum / (documentCount));
    }

    public DocumentTerm getTerm () {
        return termId.getTerm();
    }

    public String getTermString () {
        return getTerm().getTerm();
    }

    @Embeddable
    private static class TermId implements Serializable {
        // Linked to document term, so IDF updates automatically without needing to update this record
        // Comes at the cost of increased database accesses
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "term")
        private DocumentTerm term;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "event_id")
        private Event event;

        public TermId () {}

        public TermId (DocumentTerm term, Event event) {
            this.term = term;
            this.event = event;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TermId termId = (TermId) o;
            return term.getTerm().equals(termId.term.getTerm()) && event.getId().equals(termId.event.getId());
        }

        @Override
        public int hashCode () {
            return Objects.hash(term.getTerm(), event.getId());
        }

        public DocumentTerm getTerm () {
            return term;
        }
    }
}
