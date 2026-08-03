package KTB3.yun.Joongul.likes;

import KTB3.yun.Joongul.common.auth.JwtTokenProvider;
import KTB3.yun.Joongul.common.dto.JwtToken;
import KTB3.yun.Joongul.global.support.IntegrationTestSupport;
import KTB3.yun.Joongul.likes.domain.Like;
import KTB3.yun.Joongul.likes.repository.LikeRepository;
import KTB3.yun.Joongul.members.domain.Member;
import KTB3.yun.Joongul.members.repository.MemberRepository;
import KTB3.yun.Joongul.posts.domain.Post;
import KTB3.yun.Joongul.posts.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

public class LikeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("로그인하지 않은 사용자는 게시글 좋아요 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_좋아요_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .when()
                .post("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글 좋아요에 성공한다")
    void 로그인_사용자_게시글_좋아요_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(204);

        Post likedPost = postRepository.findById(post1.getPostId()).orElseThrow();
        assertEquals(1, likedPost.getLikes());

        boolean isLikeExist = spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(200)
                .extract().as(boolean.class);
        assertTrue(isLikeExist);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 요청 시 404 오류가 발생한다")
    void 존재하지_않는_게시글에_좋아요_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/posts/{id}/likes", 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 사용자가 중복으로 요청하면 409 오류가 발생한다")
    void 중복_좋아요_시_204() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        saveLike(post1, member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(409);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 좋아요 취소 요청을 할 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_좋아요_취소_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        saveLike(post1, member);

        spec
                .when()
                .delete("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글 좋아요 취소에 성공한다")
    void 로그인_사용자_게시글_좋아요_취소_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        saveLike(post1, member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(204);

        Post unlikedPost = postRepository.findById(post1.getPostId()).orElseThrow();
        assertEquals(0, unlikedPost.getLikes());

        boolean isLiked = spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(200)
                .extract().as(boolean.class);
        assertFalse(isLiked);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 취소 요청 시 404 오류가 발생한다")
    void 존재하지_않는_게시글_좋아요_취소_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}/likes", 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("좋아요를 누르지 않은 상태에서 취소 요청 시 400 오류가 발생한다")
    void 좋아요_누르지_않은_상태에서_취소_시_400() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}/likes", post1.getPostId())
                .then().log().all()
                .statusCode(400)
                .body("message", equalTo("좋아요를 누르지 않았습니다."));
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
                .member(member).build();
        return postRepository.save(post);
    }

    private Like saveLike(Post post, Member member) {
        Like like = Like.builder()
                .post(post)
                .member(member).build();

        post.increaseLikes();
        postRepository.save(post);
        return likeRepository.save(like);
    }



}
