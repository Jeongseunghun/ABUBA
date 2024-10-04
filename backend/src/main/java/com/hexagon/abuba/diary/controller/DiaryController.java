package com.hexagon.abuba.diary.controller;

import com.hexagon.abuba.auth.dto.CustomUserDetails;
import com.hexagon.abuba.diary.Diary;
import com.hexagon.abuba.diary.dto.request.DiaryDetailReqDTO;
import com.hexagon.abuba.diary.dto.request.DiaryEditReqDTO;
import com.hexagon.abuba.diary.dto.request.DiaryRecentReqDTO;
import com.hexagon.abuba.diary.dto.response.DiaryDetailResDTO;
import com.hexagon.abuba.diary.dto.response.DiaryRecentResDTO;
import com.hexagon.abuba.diary.dto.response.DiaryResDTO;
import com.hexagon.abuba.diary.service.DiaryService;
import com.hexagon.abuba.user.Parent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/diary")
@CrossOrigin("*")
@Slf4j
public class DiaryController {
    /*
    TODO
    /notification : GET - 알림
    / : PUT - 일기 수정
    /{diary_id} : GET - 상세 조회
     */

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /***
     * 가장 최근에 다이어리에 작성한 사진이 있는 게시물 3개 불러오기
     * @return
     */
    @SecurityRequirement(name = "access")  // 이 API는 토큰이 필요함
    @GetMapping("/recents")
    @Operation(summary = "가장 최근에 다이어리에 작성한 사진이 있는 게시물 3개 불러오기")
    public ResponseEntity<List<DiaryRecentResDTO>> getRecent(@AuthenticationPrincipal(expression = "user") Parent user) {
        log.info("getRecent");
        List<DiaryRecentResDTO> diaryRecentResDTOList = diaryService.recentDiary(user.getId());

        return ResponseEntity.ok(diaryRecentResDTOList);
    }

    /***
     * 다이어리에 작성한 게시물 목록을 리턴하기
     //     * @param diaryRecentReqDTO
     * @return
     */
    @SecurityRequirement(name = "access")  // 이 API는 토큰이 필요함
    @GetMapping
    @Operation(summary = "작성한 게시글의 목록 조회")
    public ResponseEntity<List<DiaryResDTO>> getList(@AuthenticationPrincipal(expression = "user") Parent user) {
        log.info("user_id={}", user.getId());
        List<DiaryResDTO> resDTOList = diaryService.getList(user.getId());

        return ResponseEntity.ok(resDTOList);
    }

    @SecurityRequirement(name = "access")  // 이 API는 토큰이 필요함
    @GetMapping("/{diary_id}")
    @Operation(summary = "일기 상세 조회")
    public ResponseEntity<DiaryDetailResDTO> getDetail(@PathVariable Long diary_id) {
        log.info("getDetail");
        DiaryDetailResDTO diaryDetailResDTO = diaryService.getDetail(diary_id);
        return ResponseEntity.ok(diaryDetailResDTO);
    }


    @SecurityRequirement(name = "access")  // 이 API는 토큰이 필요함
    @PostMapping(consumes = {MediaType.ALL_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "일기 작성")
    public ResponseEntity<String> addDiary(
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "record", required = false) MultipartFile record,
            @RequestPart("info") DiaryDetailReqDTO diaryDetailReqDTO,
            @AuthenticationPrincipal(expression = "user") Parent user) {

        log.info("addDiary");
        diaryService.addDiary(user.getId(), diaryDetailReqDTO, image, record);
        return ResponseEntity.ok("add Diary Success");
    }


    @SecurityRequirement(name = "access")  // 이 API는 토큰이 필요함
    @PutMapping(consumes = {MediaType.ALL_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "일기 수정")
    public ResponseEntity<String> editDiary(@RequestPart(value = "image", required = false) MultipartFile image,
                                            @RequestPart(value = "record", required = false) MultipartFile record,
                                            @RequestPart("info") DiaryEditReqDTO diaryEditReqDTO,
                                            @AuthenticationPrincipal(expression = "user") Parent user) {
        log.info("editDiary");
        diaryService.editDiary(diaryEditReqDTO, image, record);
        return ResponseEntity.ok("edit Diary Success");
    }
}
