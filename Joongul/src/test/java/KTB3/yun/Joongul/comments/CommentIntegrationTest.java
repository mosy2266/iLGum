package KTB3.yun.Joongul.comments;

import KTB3.yun.Joongul.comments.domain.Comment;
import KTB3.yun.Joongul.comments.dto.CommentUpdateRequestDto;
import KTB3.yun.Joongul.comments.dto.CommentWriteRequestDto;
import KTB3.yun.Joongul.comments.repository.CommentRepository;
import KTB3.yun.Joongul.common.auth.JwtTokenProvider;
import KTB3.yun.Joongul.common.dto.JwtToken;
import KTB3.yun.Joongul.global.support.IntegrationTestSupport;
import KTB3.yun.Joongul.members.domain.Member;
import KTB3.yun.Joongul.members.repository.MemberRepository;
import KTB3.yun.Joongul.posts.domain.Post;
import KTB3.yun.Joongul.posts.repository.PostRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CommentRepository commentRepository;


    @Test
    @DisplayName("로그인하지 않은 사용자는 댓글 작성 시 401 오류가 발생한다")
    void 비로그인_사용자_댓글_작성_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);

        CommentWriteRequestDto commentReq = new CommentWriteRequestDto("댓글");

        spec
                .contentType(ContentType.JSON)
                .body(commentReq)
                .when()
                .post("/posts/{postId}/comments", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 댓글 작성에 성공한다")
    void 로그인_사용자_댓글_작성_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        CommentWriteRequestDto commentReq = new CommentWriteRequestDto("댓글");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(commentReq)
                .when()
                .post("/posts/{postId}/comments", post1.getPostId())
                .then().log().all()
                .statusCode(201)
                .body("data.nickname", equalTo(member.getNickname()))
                .body("data.content", equalTo(commentReq.getContent()));
    }

    @Test
    @DisplayName("댓글을 작성한 게시글이 존재하지 않을 경우 404 오류가 발생한다")
    void 댓글_작성_시_존재하지_않는_게시글이면_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);

        CommentWriteRequestDto commentReq = new CommentWriteRequestDto("댓글");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(commentReq)
                .when()
                .post("/posts/{postId}/comments", 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("작성할 댓글의 내용이 유효하지 않은 경우 400 오류가 발생한다")
    void 유효하지_않은_내용으로_댓글_작성_시_400() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        CommentWriteRequestDto commentReq = new CommentWriteRequestDto("");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(commentReq)
                .when()
                .post("/posts/{postId}/comments", post1.getPostId())
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 댓글 수정 시 401 오류가 발생한다")
    void 비로그인_사용자_댓글_수정_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        CommentUpdateRequestDto updateReq = new CommentUpdateRequestDto("수정");

        spec
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 댓글 수정에 성공한다")
    void 로그인_사용자_댓글_수정_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        CommentUpdateRequestDto updateReq = new CommentUpdateRequestDto("수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(200)
                .body("data.nickname", equalTo(member.getNickname()))
                .body("data.content", equalTo(updateReq.getContent()));

        Comment savedComment = commentRepository.findById(comment.getCommentId()).orElseThrow();

        assertEquals(updateReq.getContent(), savedComment.getContent());
        assertEquals(post1.getPostId(), savedComment.getPost().getPostId());
    }

    @Test
    @DisplayName("다른 사용자의 댓글을 수정 시도 시 403 오류가 발생한다")
    void 다른_사용자_댓글_수정_시_403() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Member anotherMember = saveMember("tester@test.com", "Test111!", "테스터2");
        String accessToken = getAccessToken(anotherMember);
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        CommentUpdateRequestDto updateReq = new CommentUpdateRequestDto("수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정 시도 시 404 오류가 발생한다")
    void 존재하지_않는_댓글_수정_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        CommentUpdateRequestDto updateReq = new CommentUpdateRequestDto("수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{postId}/comments/{commentId}", post1.getPostId(), 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("댓글의 내용이 유효하지 않은 경우 400 오류가 발생한다")
    void 유효하지_않은_내용으로_댓글_수정_시_400() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        CommentUpdateRequestDto updateReq = new CommentUpdateRequestDto("");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 댓글 삭제 시 401 오류가 발생한다")
    void 비로그인_사용자_댓글_삭제_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        spec
                .when()
                .delete("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 본인의 댓글 삭제에 성공한다")
    void 로그인_사용자_댓글_삭제_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(204);

        Optional<Comment> deletedComment = commentRepository.findById(comment.getCommentId());
        assertThat(deletedComment).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 댓글 삭제 시도 시 403 오류가 발생한다")
    void 다른_사용자의_댓글_삭제_시도_시_403() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Member anotherMember = saveMember("tester@test.com", "Test111!", "테스터2");
        String accessToken = getAccessToken(anotherMember);
        Post post1 = savePost("제목1", "내용1", member);
        Comment comment = saveComment(post1, member, "내용");

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{postId}/comments/{commentId}", post1.getPostId(), comment.getCommentId())
                .then().log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시도 시 404 오류가 발생한다")
    void 존재하지_않는_댓글_삭제_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{postId}/comments/{commentId}", post1.getPostId(), 1L)
                .then().log().all()
                .statusCode(404);
    }

    private Member saveMember(String email, String password, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .roles(List.of("ROLE_USER"))
                .isDeleted(false)
                .build();

        return memberRepository.save(member);
    }

    private String getAccessToken(Member member) {
        List<String> roles = member.getRoles();
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new).toList();
        JwtToken jwt = jwtTokenProvider.generateJwt(member.getEmail(), authorities);
        return jwt.getAccessToken();
    }

    private Post savePost(String title, String content, Member member) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .nickname(member.getNickname())
                .likes(0)
                .comments(0)
                .views(0)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .commentsList(new ArrayList<>())
                .member(member).build();
        return postRepository.save(post);
    }

    private Comment saveComment(Post post, Member member, String content) {
        Comment comment = Comment.builder()
                .nickname(member.getNickname())
                .content(content)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .member(member)
                .post(post).build();
        return commentRepository.save(comment);
    }
}
