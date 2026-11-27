package com.petCare.petCare.domain.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petCare.petCare.domain.community.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@Query("""
		select c from Comment c
		join fetch c.author
		where c.post.id = :postId
		order by c.createdAt asc, c.id asc
		""")
	List<Comment> findAllWithAuthorByPostId(@Param("postId") Long postId);

	@Query("""
		select c from Comment c
		join fetch c.author
		join fetch c.post
		where c.id = :commentId
		  and c.post.id = :postId
		""")
	Optional<Comment> findByIdAndPostIdWithAuthor(@Param("commentId") Long commentId, @Param("postId") Long postId);

	@Query("""
		select c.post.id, count(c) from Comment c
		where c.post.id in :postIds
		group by c.post.id
		""")
	List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

}
