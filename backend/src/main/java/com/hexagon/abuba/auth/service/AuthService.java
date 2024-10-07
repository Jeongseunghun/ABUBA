package com.hexagon.abuba.auth.service;

import com.hexagon.abuba.auth.dto.request.JoinDTO;
import com.hexagon.abuba.auth.entity.VerificationToken;
import com.hexagon.abuba.auth.repository.VerificationTokenRepository;
import com.hexagon.abuba.global.exception.BusinessException;
import com.hexagon.abuba.global.exception.ErrorCode;
import com.hexagon.abuba.global.openfeign.FinAPIClient;
import com.hexagon.abuba.global.openfeign.dto.request.SignupRequestDTO;
import com.hexagon.abuba.global.openfeign.dto.response.SignupResponseDTO;
import com.hexagon.abuba.user.Parent;
import com.hexagon.abuba.user.repository.BabyRepository;
import com.hexagon.abuba.user.repository.ParentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Transactional
@Service
@Slf4j
public class AuthService {

    private final ParentRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final FinAPIClient finAPIClient;
    private final BabyRepository babyRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${api.key}")
    private String apikey;

    @Value("${app.email.verification-url}")
    private String verificationUrl; // 이메일 인증 링크에 사용할 URL

    public AuthService(ParentRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, FinAPIClient finAPIClient, BabyRepository babyRepository, VerificationTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.finAPIClient = finAPIClient;
        this.babyRepository = babyRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public void joinProcess(JoinDTO joinDTO) {

        String username = joinDTO.getEmail();
        String password = joinDTO.getPassword();
        String name = joinDTO.getName();
        log.info("joinDTO={}", joinDTO.toString());
        Boolean isExist = userRepository.existsByUsername(username);

        if (isExist) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        //금융api로 user키를 발급 받는다.
//        SignupResponseDTO response = finAPIClient.signup(new SignupRequestDTO(apikey, joinDTO.getEmail()));

        Parent data = new Parent();

        data.setUsername(username);
        data.setPassword(bCryptPasswordEncoder.encode(password));
        data.setName(name);
        data.setRole("ROLE_USER");
//        data.setUserkey(response.getUserKey());

        userRepository.save(data);
    }


    // 이메일로 인증 링크 전송
    public void sendVerificationEmail(String email) {
        // 1. 사용자가 이미 존재하는지 확인
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        // 2. 인증 토큰 생성 및 저장
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, email);
        tokenRepository.save(verificationToken);

        // 3. 이메일 발송
        String verificationLink = verificationUrl + "?token=" + token;
        emailService.sendVerificationEmail(email, verificationLink);
    }

    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null || verificationToken.isExpired()) {
            return false; // 토큰이 없거나 만료된 경우
        }

        // 토큰이 유효하면 해당 이메일의 인증 상태를 true로 변경
        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);
        return true;
    }

    // 이메일이 인증되었는지 확인
    public boolean isEmailVerified(String email) {
        VerificationToken token = tokenRepository.findByEmail(email);
        return token != null && token.isVerified();
    }

    public boolean checkOnboarding(Long parentId) {
        Parent user = userRepository.findById(parentId).orElseThrow();
        return user.getBaby() == null;
    }

}
