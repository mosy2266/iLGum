package KTB3.yun.Joongul.members;

import KTB3.yun.Joongul.global.support.IntegrationTestSupport;
import KTB3.yun.Joongul.members.domain.Member;
import KTB3.yun.Joongul.members.dto.LoginRequestDto;
import KTB3.yun.Joongul.members.dto.MemberInfoUpdateRequestDto;
import KTB3.yun.Joongul.members.dto.PasswordUpdateRequestDto;
import KTB3.yun.Joongul.members.dto.SignUpRequestDto;
import KTB3.yun.Joongul.members.repository.MemberRepository;
import KTB3.yun.Joongul.members.service.MemberService;
import io.restassured.http.ContentType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MemberIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberService memberService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String signUpAndLogin(String email, String password, String nickname) {
        spec.contentType(ContentType.JSON)
                .body(new SignUpRequestDto(email, password, password,nickname, null))
                .post("/members");

        return spec
                .contentType(ContentType.JSON)
                .body(new LoginRequestDto(email, password))
                .post("/members/session")
                .then().extract().jsonPath().getString("accessToken");
    }

    @Test
    @DisplayName("회원가입 후 로그인하면 토큰을 발급받고, 해당 토큰으로 내 정보를 조회할 수 있다")
    void 회원가입_후_로그인_시_내_정보_조회_성공() {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");

        spec
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/members/me")
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .body("data.email", equalTo("test@test.com"))
                .body("data.nickname", equalTo("테스터"));
    }

    @Test
    @DisplayName("토큰 없이 내 정보 API를 호출하면 401 에러가 발생한다")
    void 토큰_없이_내_정보_호출하면_401 () {
        spec
                .contentType(ContentType.JSON)
                .when()
                .get("/members/me")
                .then().log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("중복된 이메일로 가입을 시도하면 실패한다")
    void 중복_이메일로_가입하면_실패 () {
        SignUpRequestDto req1 = new SignUpRequestDto("test@test.com", "Test123!", "Test123!", "테스터1", null);
        SignUpRequestDto req2 = new SignUpRequestDto("test@test.com", "Test123!", "Test123!", "테스터2", null);

        memberService.signup(req1);

        spec
                .contentType(ContentType.JSON)
                .body(req2)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("중복된 닉네임으로 가입을 시도하면 실패한다")
    void 중복_닉네임으로_가입하면_실패 () {
        SignUpRequestDto req1 = new SignUpRequestDto("test@test.com", "Test123!", "Test123!", "테스터1", null);
        SignUpRequestDto req2 = new SignUpRequestDto("testtest@test.com", "Test123!", "Test123!", "테스터1", null);

        memberService.signup(req1);

        spec
                .contentType(ContentType.JSON)
                .body(req2)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("회원가입 폼의 이메일 유효성 검사를 통과하지 못한 경우 400 에러가 발생한다")
    void 회원가입_폼_이메일_유효성_검사_통과_실패() {
        SignUpRequestDto req = new SignUpRequestDto("wrong_email", "Test123!", "Test123!", "테스터1", null);

        spec
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(400)
                .body("email", equalTo("올바른 이메일 주소 형식을 입력해주세요.(예: example@example.com)"));
    }

    @Test
    @DisplayName("회원가입 폼의 비밀번호 유효성 검사를 통과하지 못한 경우 400 에러가 발생한다")
    void 회원가입_폼_비밀번호_유효성_검사_통과_실패() {
        SignUpRequestDto req = new SignUpRequestDto("test@test.com", "t", "Test123!", "테스터1", null);

        spec
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(400)
                .body("password", equalTo("비밀번호는 8자 이상 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."));
    }

    @Test
    @DisplayName("회원가입 폼의 닉네임 유효성 검사(띄어쓰기)를 통과하지 못한 경우 400 에러가 발생한다")
    void 회원가입_폼_닉네임_띄어쓰기_있으면_실패() {
        SignUpRequestDto req = new SignUpRequestDto("test@test.com", "Test123!", "Test123!", "테 스터", null);

        spec
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(400)
                .body("nickname", equalTo("띄어쓰기를 없애주세요."));
    }

    @Test
    @DisplayName("회원가입 폼의 닉네임 유효성 검사(열 글자 이하)를 통과하지 못한 경우 400 에러가 발생한다")
    void 회원가입_폼_닉네임_열_글자_넘으면_실패() {
        SignUpRequestDto req = new SignUpRequestDto("test@test.com", "Test123!", "Test123!", "열글자가넘는테스트이름입니다", null);

        spec
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post("/members")
                .then().log().all()
                .statusCode(400)
                .body("nickname", equalTo("닉네임은 최대 10자까지 작성 가능합니다."));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 회원 정보 수정을 요청하면 401 에러가 발생한다")
    void 비로그인_회원_정보_수정_요청_시_실패() {
        MemberInfoUpdateRequestDto updateReq = new MemberInfoUpdateRequestDto("닉네임", "이미지");

        spec
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/members/{id}", 1L)
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("다른 사용자의 정보를 수정하려고 하면 403 에러가 발생한다")
    void 타인_정보_수정_실패() {
        String attackerToken = signUpAndLogin("test@test.com", "Test111!", "공격자");
        Member victim = Member.builder()
                .email("victim@test.com")
                .password("Victim111!")
                .nickname("피해자")
                .isDeleted(false).build();
        memberRepository.save(victim);

        MemberInfoUpdateRequestDto updateReq = new MemberInfoUpdateRequestDto("공격시도", "공격시도이미지");

        spec
                .header("Authorization", "Bearer " + attackerToken)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/members/{id}", victim.getMemberId())
                .then().log().all()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("로그인 후 내 정보를 수정하면 DB에 반영된다")
    void 내_정보_수정_성공() {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        MemberInfoUpdateRequestDto updateReq = new MemberInfoUpdateRequestDto("테스트", "이미지");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        spec
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/members/{id}", memberId)
                .then().log().all()
                .statusCode(200);

        spec
                .header("Authorization", "Bearer " + accessToken)
                .when().get("/members/me")
                .then()
                .body("data.nickname", equalTo("테스트"))
                .body("data.profileImage", equalTo("이미지"));
    }

    @Test
    @DisplayName("열 글자가 넘는 닉네임으로 회원 정보를 수정하면 400 에러가 발생한다")
    void 열_글자_초과_닉네임으로_수정_시_400() {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        MemberInfoUpdateRequestDto updateReq = new MemberInfoUpdateRequestDto("열글자가넘는테스트이름입니다", "이미지");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        spec
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/members/{id}", memberId)
                .then().log().all()
                .statusCode(400)
                .body("nickname", equalTo("닉네임은 최대 10자까지 작성 가능합니다."));
    }

    @Test
    @DisplayName("띄어쓰기가 있는 닉네임으로 회원 정보를 수정하면 400 에러가 발생한다")
    void 띄어쓰기_있는_닉네임으로_수정_시_400() {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        MemberInfoUpdateRequestDto updateReq = new MemberInfoUpdateRequestDto("테 스터", "이미지");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        spec
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .put("/members/{id}", memberId)
                .then().log().all()
                .statusCode(400)
                .body("nickname", equalTo("띄어쓰기를 없애주세요."));
    }


    @Test
    @DisplayName("로그인하지 않은 사용자가 비밀번호 변경을 요청하면 401 에러가 발생한다")
    void 비로그인_비밀번호_변경_요청_시_실패() {
        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto("NewPw123!", "NewPw123!");

        spec
                .contentType(ContentType.JSON)
                .body(updateReq)
                .when()
                .patch("/members/{id}", 1L)
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("다른 사용자의 비밀번호를 수정하려고 하면 403 에러가 발생한다")
    void 타인_비밀번호_변경_실패() throws Exception {
        String attackerToken = signUpAndLogin("test@test.com", "Test111!", "공격자");
        Member victim = Member.builder()
                .email("victim@test.com")
                .password("Victim111!")
                .nickname("피해자")
                .isDeleted(false).build();
        memberRepository.save(victim);

        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto("Attack111!", "Attack111!");

        mockMvc.perform(patch("/members/{id}", victim.getMemberId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + attackerToken)
                        .cookie(new Cookie("refreshToken", "dummy"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isForbidden());

    }

    @Test
    @DisplayName("로그인 후 비밀번호를 변경하면 DB에 반영된다")
    void 비밀번호_변경_성공() throws Exception {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto("Test123!", "Test123!");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        mockMvc.perform(patch("/members/{id}", memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", "dummy"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isOk());

        Member updatedMember = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("멤버 없음"));
        boolean isMatched = passwordEncoder.matches("Test123!", updatedMember.getPassword());

        assertTrue(isMatched);
    }

    @Test
    @DisplayName("기존에 사용하던 비밀번호로 변경 요청 시 422 에러가 발생한다")
    void 기존_비밀번호로_변경_시_422() throws Exception {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto("Test111!", "Test111!");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        mockMvc.perform(patch("/members/{id}", memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", "dummy"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 비밀번호입니다."));
    }

    @Test
    @DisplayName("유효하지 않은 비밀번호(틀린 형식)로 변경 요청 시 400 에러가 발생한다")
    void 형식이_틀린_비밀번호로_변경_시_400() throws Exception {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto("test", "test");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        mockMvc.perform(patch("/members/{id}", memberId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", "dummy"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("비밀번호는 8자 이상 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 회원 탈퇴를 요청하면 401 에러가 발생한다")
    void 비로그인_회원_탈퇴_요청_시_실패() {
        spec
                .when()
                .delete("/members/{id}", 1L)
                .then().log().all()
                .statusCode(401);
    }

    @Test
    @DisplayName("다른 사용자의 계정을 탈퇴하려고 하면 403 에러가 발생한다")
    void 타인_계정_탈퇴_실패() throws Exception {
        String attackerToken = signUpAndLogin("test@test.com", "Test111!", "공격자");
        Member victim = Member.builder()
                .email("victim@test.com")
                .password("Victim111!")
                .nickname("피해자")
                .isDeleted(false).build();
        memberRepository.save(victim);

        mockMvc.perform(delete("/members/{id}", victim.getMemberId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + attackerToken)
                        .cookie(new Cookie("refreshToken", "dummy")))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("회원 탈퇴 시 해당 계정으로 다시 로그인할 수 없다")
    void 회원_탈퇴_후_해당_계정으로_로그인_불가() throws Exception {
        String accessToken = signUpAndLogin("test@test.com", "Test111!", "테스터");
        Long memberId = memberRepository.findByEmail("test@test.com").orElseThrow(() -> new RuntimeException("멤버 없음"))
                .getMemberId();

        mockMvc.perform(delete("/members/{id}", memberId)
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", "dummy_token")))
                .andDo(print())
                .andExpect(status().isNoContent());

        LoginRequestDto loginReq = new LoginRequestDto("test@test.com", "Test111!");
        spec
                .contentType(ContentType.JSON)
                .body(loginReq)
                .when()
                .post("/members/session")
                .then().log().all()
                .statusCode(404);
    }
}
