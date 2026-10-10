package com.hong.halfway.domain.station.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * GTFS 폴더 경로를 로컬 전용 포인터 파일({@link #POINTER_FILE})에서 읽음. 커밋되지 않는 파일(.gitignore 등록)이라
 * GTFS 버전이 바뀌면 파일 내용만 고치면 됨. 경로에 공백·한글이 있어 명령줄 인자로 넘기면 깨지는 문제를 피하기 위함
 */
@Slf4j
@Component
public class GtfsDirectoryLocator {

    private static final String POINTER_FILE = "gtfs-directory.local.txt";

    /**
     * 포인터 파일에서 GTFS 폴더 경로를 읽음. 파일이 없거나 비어있으면 빈 값(그 배치만 생략, 전체 중단 아님)
     */
    public Optional<Path> locate() {
        Path pointerFile = Path.of(POINTER_FILE);
        if (!Files.exists(pointerFile)) {
            log.warn("[건너뜀] {} 파일이 없어 GTFS 연동을 건너뜀 (GTFS_DataSet 폴더 경로를 한 줄로 적어두세요)", POINTER_FILE);
            return Optional.empty();
        }
        try {
            String content = Files.readString(pointerFile, StandardCharsets.UTF_8).trim();
            if (content.isBlank()) {
                log.warn("[건너뜀] {} 파일이 비어있어 GTFS 연동을 건너뜀", POINTER_FILE);
                return Optional.empty();
            }
            return Optional.of(Path.of(content));
        } catch (IOException e) {
            throw new IllegalStateException(POINTER_FILE + " 읽기 실패", e);
        }
    }
}
