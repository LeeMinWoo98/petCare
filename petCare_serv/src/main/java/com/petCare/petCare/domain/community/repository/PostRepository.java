package com.petCare.petCare.domain.community.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.community.entity.Post;
import com.petCare.petCare.domain.community.entity.PostCategory;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query(
		value = """
			select p from Post p
			join fetch p.author
			where p.deletedAt is null
			  and (:category is null or p.category = :category)
			  and (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
			       or lower(p.content) like lower(concat('%', :keyword, '%')))
			order by p.createdAt desc, p.id desc
			""",
		countQuery = """
			select count(p) from Post p
			where p.deletedAt is null
			  and (:category is null or p.category = :category)
			  and (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
			       or lower(p.content) like lower(concat('%', :keyword, '%')))
			"""
	)
	Page<Post> searchActivePosts(
		@Param("category") PostCategory category,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@Query("""
		select p from Post p
		join fetch p.author
		where p.id = :postId
		  and p.deletedAt is null
		""")
	Optional<Post> findActiveByIdWithAuthor(@Param("postId") Long postId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Post p set p.viewCount = p.viewCount + :delta where p.id = :postId and p.deletedAt is null")
	int addViewCount(@Param("postId") Long postId, @Param("delta") long delta);

}
