package KTB3.yun.Joongul.members;

import KTB3.yun.Joongul.common.auth.JwtTokenProvider;
import KTB3.yun.Joongul.common.dto.JwtToken;
import KTB3.yun.Joongul.global.support.IntegrationTestSupport;
import KTB3.yun.Joongul.members.domain.Member;
import KTB3.yun.Joongul.members.domain.RefreshToken;
import KTB3.yun.Joongul.members.repository.MemberRepository;
import KTB3.yun.Joongul.members.repository.RefreshTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TokenIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("유효한 RT로 요청 시 새로운 AT와 RT 재발급에 성공한다")
    void 유효한_RT이면_토큰_재발급_성공() throws Exception {
        Member member = saveMember();
        String validRefreshToken = createAndSaveRefreshToken(member);
        RefreshToken oldRefreshToken = refreshTokenRepository.findByRefreshTokenAndRevokedFalse(validRefreshToken).orElseThrow();
        Long oldTokenId = oldRefreshToken.getTokenId();

        mockMvc.perform(post("/token")
                        .cookie(new Cookie("refreshToken", validRefreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));

        boolean isOldTokenExists = refreshTokenRepository.existsById(oldTokenId);
        assertFalse(isOldTokenExists);
    }

    @Test
    @DisplayName("RT 쿠키가 없으면 재발급 요청 시 TOKEN_NOT_FOUND(404) 오류가 발생한다")
    void RT_쿠키_없으면_TOKEN_NOT_FOUND() {
        spec
                .when()
                .post("/token")
                .then().log().all()
                .statusCode(404)
                .body("message", equalTo("존재하지 않는 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("유효하지 않은 형식의 RT로 재발급 요청 시 INVALID_TOKEN(401) 오류가 발생한다")
    void 유효하지_않은_형식의_RT는_INVALID_TOKEN() throws Exception {

        mockMvc.perform(post("/token")
                        .cookie(new Cookie("refreshToken", "not.valid.token")))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다"));
    }

    @Test
    @DisplayName("DB에 존재하지 않는 RT로 재발급 요청 시 TOKEN_NOT_FOUND(404) 오류가 발생한다")
    void DB에_없는_RT는_TOKEN_NOT_FOUND() throws Exception {
        Member member = saveMember();
        JwtToken jwtToken = jwtTokenProvider.generateJwt(member.getEmail(),
                member.getRoles().stream().map(SimpleGrantedAuthority::new).toList());

        mockMvc.perform(post("/token")
                        .cookie(new Cookie("refreshToken", jwtToken.getRefreshToken())))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 RT로 재발급 요청 시 401 오류가 발생한다")
    void 만료된_RT로_재발급_요청_시_실패() throws Exception {
        Member member = saveMember();
        JwtToken jwtToken = jwtTokenProvider.generateJwt(member.getEmail(),
                member.getRoles().stream().map(SimpleGrantedAuthority::new).toList());

        RefreshToken expiredRefreshToken = RefreshToken.builder()
                .refreshToken(jwtToken.getRefreshToken())
                .member(member)
                .createdAt(System.currentTimeMillis() - 10000000)
                .expiresAt(1000L)
                .revoked(false)
                .build();
        refreshTokenRepository.save(expiredRefreshToken);

        mockMvc.perform(post("/token")
                        .cookie(new Cookie("refreshToken", expiredRefreshToken.getRefreshToken())))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("만료된 토큰입니다"));
    }


    private Member saveMember() {
        return memberRepository.save(Member.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("pw"))
                .nickname("tester")
                .roles(List.of("ROLE_USER"))
                .isDeleted(false)
                .build());
    }

    private String createAndSaveRefreshToken(Member member) {
        JwtToken jwtToken = jwtTokenProvider.generateJwt(member.getEmail(),
                member.getRoles().stream().map(SimpleGrantedAuthority::new).toList());

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshToken(jwtToken.getRefreshToken())
                .member(member)
                .createdAt(System.currentTimeMillis())
                .expiresAt(jwtToken.getRefreshTokenExpireTime())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return jwtToken.getRefreshToken();
    }
}
