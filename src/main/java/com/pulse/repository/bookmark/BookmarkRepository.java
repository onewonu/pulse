package com.pulse.repository.bookmark;

import com.pulse.entity.bookmark.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<Bookmark> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT b FROM Bookmark b WHERE b.id IN :ids AND b.user.id = :userId")
    List<Bookmark> findByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
