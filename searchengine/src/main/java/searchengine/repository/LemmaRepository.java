package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository<Lemma, Long> {

    @Query(
            value = "SELECT l FROM Lemma l " +
                    "WHERE l.site.id = :siteId AND l.lemma IN (:lemmas) " +
                    "ORDER BY l.frequency"
    )
    List<Lemma> getByLemma(Long siteId, Collection<String> lemmas);

    List<Lemma> findByLemmaOrderByFrequency(String lemma);

    long countBySite(Site site);

    @Modifying
    @Query(
        value = "UPDATE lemma l " +
            "JOIN `index` i ON i.lemma_id = l.id " +
            "SET l.frequency = IF(l.frequency > 0, l.frequency - 1, 0) " +
            "WHERE i.page_id = :pageId",
        nativeQuery = true
    )
    void updateByPage(Long pageId);

    @Modifying
    void deleteBySite(Site site);

    @Modifying
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) " +
            "VALUES (?1, ?2, ?3) AS new(s, l, f) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + new.f ", nativeQuery = true)
    void insertLemma(long siteId, String lemma, int frequency);
}