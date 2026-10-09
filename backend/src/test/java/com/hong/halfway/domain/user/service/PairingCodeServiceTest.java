package com.hong.halfway.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hong.halfway.domain.user.entity.PairingCode;
import com.hong.halfway.domain.user.entity.User;
import com.hong.halfway.domain.user.repository.PairingCodeRepository;
import com.hong.halfway.domain.user.repository.UserRepository;
import com.hong.halfway.global.common.AuthTokenUtils;
import com.hong.halfway.global.error.CustomException;
import com.hong.halfway.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PairingCodeServiceTest {

    @Autowired
    private PairingCodeService pairingCodeService;

    @Autowired
    private PairingCodeRepository pairingCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 코드를_발급하면_4자리_숫자다() {
        UUID userId = createUser();

        PairingCode pairingCode = pairingCodeService.issue(userId);

        assertThat(pairingCode.getCode()).hasSize(4);
        assertThat(pairingCode.getCode()).matches("\\d{4}");
    }

    @Test
    void 이미_쓸수있는_코드가_있으면_재발급하지_않고_기존_코드를_그대로_준다() {
        UUID userId = createUser();

        PairingCode first = pairingCodeService.issue(userId);
        PairingCode second = pairingCodeService.issue(userId);

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void 올바른_코드로_교환하면_새_토큰이_발급되고_기존_토큰은_무효화된다() {
        User user = userRepository.save(User.createAnonymous(AuthTokenUtils.hash("old-raw-token")));
        PairingCode pairingCode = pairingCodeRepository.save(
                PairingCode.issue("1234", user, LocalDateTime.now().plusMinutes(15)));

        String newRawToken = pairingCodeService.redeem("1234", "test-ip-redeem-success");

        User found = userRepository.findByTokenHash(AuthTokenUtils.hash(newRawToken)).orElseThrow();
        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(userRepository.findByTokenHash(AuthTokenUtils.hash("old-raw-token"))).isEmpty();
        assertThat(pairingCodeRepository.findById(pairingCode.getId()).orElseThrow().getUsedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_코드로_교환하면_예외() {
        assertThatThrownBy(() -> pairingCodeService.redeem("9999", "test-ip-not-found"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAIRING_CODE_INVALID);
    }

    @Test
    void 만료된_코드로_교환하면_예외() {
        User user = userRepository.save(User.createAnonymous(AuthTokenUtils.hash("expired-raw-token")));
        pairingCodeRepository.save(PairingCode.issue("5555", user, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> pairingCodeService.redeem("5555", "test-ip-expired"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAIRING_CODE_INVALID);
    }

    @Test
    void 이미_사용한_코드로_다시_교환하면_예외() {
        User user = userRepository.save(User.createAnonymous(AuthTokenUtils.hash("used-raw-token")));
        pairingCodeRepository.save(PairingCode.issue("7777", user, LocalDateTime.now().plusMinutes(15)));

        pairingCodeService.redeem("7777", "test-ip-reuse-1");

        assertThatThrownBy(() -> pairingCodeService.redeem("7777", "test-ip-reuse-2"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAIRING_CODE_INVALID);
    }

    @Test
    void 같은_IP로_짧은_시간에_너무_많이_시도하면_예외() {
        for (int i = 0; i < 10; i++) {
            try {
                pairingCodeService.redeem("0000", "test-ip-rate-limit");
            } catch (CustomException ignored) {
                // 코드 자체는 틀려서 PAIRING_CODE_INVALID가 날 수 있음, 여기선 횟수만 채움
            }
        }

        assertThatThrownBy(() -> pairingCodeService.redeem("0000", "test-ip-rate-limit"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAIRING_CODE_RATE_LIMITED);
    }

    private UUID createUser() {
        return userRepository.save(User.createAnonymous(AuthTokenUtils.hash(UUID.randomUUID().toString()))).getId();
    }
}
