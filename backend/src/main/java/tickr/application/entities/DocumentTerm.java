package tickr.application.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_term_count")
public class DocumentTerm {
    @Id
    private String term;

    @Column(name = "term_count")
    private int termCount;

    public DocumentTerm () {}

    public DocumentTerm (String term, int initialCount) {
        this.term = term;
        this.termCount = initialCount;
    }

    public String getTerm () {
        return term;
    }

    public int getTermCount () {
        return termCount;
    }

    public void incrementCount () {
        termCount++;
    }
}
