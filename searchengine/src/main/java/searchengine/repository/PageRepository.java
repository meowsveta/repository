package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;

import java.util.List;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Long> {

    @Query(
        value = "SELECT s.id FROM page p " +
            "WHERE p.path = :path AND site_id = :siteId",
        nativeQuery = true
    )
    Long getIdByPath(String path, Long siteId);

    @Query(
        value = "SELECT p FROM Page p " +
            "WHERE p.path = :path AND site.id = :siteId"
    )
    Page getByPath(String path, Long siteId);

    @Query(
        value = "SELECT p FROM Page p " +
            "JOIN Index i ON p.id = i.page.id " +
            "WHERE i.lemma.id = :lemmaId"
    )
    List<Page> getByLemma(Long lemmaId);

    @Query(
        value = "SELECT p FROM Page p " +
            "JOIN Index i ON p.id = i.page.id " +
            "WHERE i.lemma.id = :lemmaId AND p.id IN (:pageIds)"
    )
    List<Page> getByLemma(Long lemmaId, List<Long> pageIds);

    @Query(
        value = "SELECT COUNT(*) FROM page WHERE site_id = :siteId",
        nativeQuery = true
    )
    long countBy(Long siteId);

    @Modifying
    @Query(
        value = "INSERT INTO page(site_id, path) VALUES(:siteId, :path) " +
            "ON DUPLICATE KEY UPDATE id = id",
        nativeQuery = true
    )
    int insert(Long siteId, String path);

    @Modifying
    @Query(
        value = "INSERT INTO page(site_id, path, code, content) " +
            "VALUES(:siteId, :path, :code, :content) " +
            "ON DUPLICATE KEY UPDATE site_id = site_id, " +
            "path = path, code = code, content = content",
        nativeQuery = true
    )
    void insert(Long siteId, String path, int code, String content);

    @Modifying
    @Query(
        value = "UPDATE page SET code = :code, content = :content " +
            "WHERE site_id = :siteId AND path = :path",
        nativeQuery = true
    )
    int update(int code, String content, Long siteId, String path);

    @Modifying
    @Query(
        value = "DELETE FROM page WHERE site_id = :siteId",
        nativeQuery = true
    )
    void deleteBySiteId(Long siteId);
}
