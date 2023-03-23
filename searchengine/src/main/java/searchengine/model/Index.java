package searchengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Table(
    name = "`index`",  uniqueConstraints = {
        @UniqueConstraint(columnNames = {"page_id", "lemma_id"})})
public class Index {

    public Index(Page page, Lemma lemma, double rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(
        name = "page_id",
        referencedColumnName = "id",
        nullable = false
    )
    private Page page;

    @ManyToOne(cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(
        name = "lemma_id",
        referencedColumnName = "id",
        nullable = false
    )
    private Lemma lemma;

    @Column(name = "index_rank", nullable = false)
    private double rank;
}
