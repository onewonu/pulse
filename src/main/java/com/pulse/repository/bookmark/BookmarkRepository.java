package com.pulse.repository.bookmark;

import com.pulse.entity.bookmark.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    List<Bookmark> findByUserIdOrderByDisplayOrderAsc(Long userId);

    @Query("SELECT COALESCE(MAX(b.displayOrder), -1) FROM Bookmark b WHERE b.user.id = :userId")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
