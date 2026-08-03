package KTB3.yun.Joongul.posts;

import KTB3.yun.Joongul.common.auth.JwtTokenProvider;
import KTB3.yun.Joongul.common.dto.JwtToken;
import KTB3.yun.Joongul.global.support.IntegrationTestSupport;
import KTB3.yun.Joongul.members.domain.Member;
import KTB3.yun.Joongul.members.repository.MemberRepository;
import KTB3.yun.Joongul.posts.domain.Post;
import KTB3.yun.Joongul.posts.dto.PostUpdateRequestDto;
import KTB3.yun.Joongul.posts.dto.PostWriteRequestDto;
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

public class PostIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("로그인하지 않은 사용자는 게시글 목록 조회 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_목록_조회_시_401 () {
        spec
                .when()
                .get("/posts")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글 목록을 조회할 수 있다")
    void 로그인_사용자_게시글_목록_조회_성공() {

        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        Post post2 = savePost("제목2", "내용2", member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts")
                .then().log().all()
                .statusCode(200)
                .body("data.size()", equalTo(2))
                .body("data[0].title", equalTo(post1.getTitle()))
                .body("data[0].nickname", equalTo(post1.getNickname()))
                .body("data[1].title", equalTo(post2.getTitle()))
                .body("data[1].nickname", equalTo(post2.getNickname()));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 게시글 상세 조회 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_상세_조회_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .when()
                .get("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글을 상세 조회할 수 있다")
    void 로그인_사용자_게시글_상세_조회_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(200)
                .body("data.postId", equalTo((Number) post1.getPostId().intValue()))
                .body("data.title", equalTo(post1.getTitle()))
                .body("data.nickname", equalTo(post1.getNickname()))
                .body("data.content", equalTo(post1.getContent()));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 조회할 경우 404 오류가 발생한다")
    void 존재하지_않는_게시글_조회_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts/{id}", 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("삭제된 게시글을 조회할 경우 404 오류가 발생한다")
    void 삭제된_게시글_조회_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post deletedPost = Post.builder()
                .title("1")
                .nickname(member.getNickname())
                .content("1")
                .likes(0)
                .comments(0)
                .views(0)
                .createdAt(LocalDateTime.now())
                .isDeleted(true)
                .member(member).build();

        postRepository.save(deletedPost);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/posts/{id}", deletedPost.getPostId())
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 게시글 작성 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_작성_시_401() {
        PostWriteRequestDto postReq = new PostWriteRequestDto("제목", "내용", null);

        spec
                .contentType(ContentType.JSON)
                .body(postReq)
                .when()
                .post("/posts")
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 게시글을 작성할 수 있다")
    void 로그인_사용자_게시글_작성_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        PostWriteRequestDto postReq = new PostWriteRequestDto("제목", "내용", null);


        Long postId = spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(postReq)
                .when()
                .post("/posts")
                .then().log().all()
                .statusCode(201)
                .body("data.title", equalTo(postReq.getTitle()))
                .body("data.content", equalTo(postReq.getContent()))
                .body("data.nickname", equalTo(member.getNickname()))
                .extract().jsonPath().getLong("data.postId");

        Post newPost = postRepository.findById(postId).orElseThrow();

        assertEquals(postReq.getTitle(), newPost.getTitle());
        assertEquals(postReq.getContent(), newPost.getContent());
        assertEquals(member.getNickname(), newPost.getNickname());
    }

    @Test
    @DisplayName("작성할 게시글의 제목이나 내용이 유효하지 않은 경우 400 오류가 발생한다")
    void 유효하지_않은_제목이나_내용으로_작성_시_400() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        PostWriteRequestDto postReq = new PostWriteRequestDto("제목", "", null);

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(postReq)
                .when()
                .post("/posts")
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 게시글 수정 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_수정_시_401() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목1", "수정", null);

        spec
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 본인의 게시글을 수정할 수 있다")
    void 로그인_사용자_본인_게시글_수정_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post post1 = savePost("제목1", "내용1", member);
        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목 수정", "내용 수정", "이미지 수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(200)
                .body("data.title", equalTo(updateReq.getTitle()))
                .body("data.content", equalTo(updateReq.getContent()))
                .body("data.postImage", equalTo(updateReq.getPostImage()));

        Post updatedPost = postRepository.findById(post1.getPostId()).orElseThrow();

        assertEquals(updateReq.getTitle(), updatedPost.getTitle());
        assertEquals(updateReq.getContent(), updatedPost.getContent());
        assertEquals(updateReq.getPostImage(), updatedPost.getPostImage());
        assertEquals(member.getNickname(), updatedPost.getNickname());
    }

    @Test
    @DisplayName("다른 사용자의 게시글 수정 시도 시 403 오류가 발생한다")
    void 다른_사용자_게시글_수정_시_403() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목1", "수정", null);

        Member anotherMember = saveMember("another@test.com", "Test111!", "다른테스터");
        String accessToken = getAccessToken(anotherMember);

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시도 시 404 오류가 발생한다")
    void 존재하지_않는_게시글_수정_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목 수정", "내용 수정", "이미지 수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{id}", 1L)
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("삭제된 게시글 수정 시도 시 404 오류가 발생한다")
    void 삭제된_게시글_수정_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);
        Post deletedPost = Post.builder()
                .title("1")
                .nickname(member.getNickname())
                .content("1")
                .likes(0)
                .comments(0)
                .views(0)
                .createdAt(LocalDateTime.now())
                .isDeleted(true)
                .member(member).build();

        postRepository.save(deletedPost);

        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목 수정", "내용 수정", "이미지 수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{id}", deletedPost.getPostId())
                .then().log().all()
                .statusCode(404);
    }

    @Test
    @DisplayName("수정할 제목이나 내용이 유효하지 않은 경우 400 오류가 발생한다")
    void 유호하지_않은_제목이나_내용으로_수정_시_400() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        String accessToken = getAccessToken(member);
        PostUpdateRequestDto updateReq = new PostUpdateRequestDto("제목 수정", "", "이미지 수정");

        spec
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(updateReq)
                .when()
                .put("/posts/{id}",  post1.getPostId())
                .then().log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 게시글 삭제 시 401 오류가 발생한다")
    void 비로그인_사용자_게시글_삭제_실패() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);

        spec
                .when()
                .delete("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("로그인한 사용자는 본인의 게시글 삭제에 성공한다")
    void 로그인_사용자_게시글_삭제_성공() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);
        String accessToken = getAccessToken(member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(204);

        Optional<Post> deletedPost = postRepository.findById(post1.getPostId());
        assertThat(deletedPost).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 게시글 삭제 시도 시 403 오류가 발생한다")
    void 다른_사용자_게시글_삭제_시_403() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        Post post1 = savePost("제목1", "내용1", member);

        Member anotherMember = saveMember("another@test.com", "Test111!", "다른테스터");
        String accessToken = getAccessToken(anotherMember);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}", post1.getPostId())
                .then().log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 삭제 시도 시 404 오류가 발생한다")
    void 존재하지_않는_게시글_삭제_시_404() {
        Member member = saveMember("test@test.com", "Test111!", "테스터");
        String accessToken = getAccessToken(member);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/posts/{id}", 1L)
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

}
