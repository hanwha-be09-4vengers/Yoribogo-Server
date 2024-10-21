package com.avengers.yoribogo.recipe.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.avengers.yoribogo.common.exception.CommonException;
import com.avengers.yoribogo.common.exception.ErrorCode;
import com.avengers.yoribogo.openai.service.OpenAIService;
import com.avengers.yoribogo.recipe.domain.MenuType;
import com.avengers.yoribogo.recipe.domain.Recipe;
import com.avengers.yoribogo.recipe.dto.*;
import com.avengers.yoribogo.recipe.repository.RecipeRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final Integer ELEMENTS_PER_PAGE = 12;

    private final ModelMapper modelMapper;
    private final RecipeRepository recipeRepository;
    private final RecipeManualService recipeManualService;
    private final PublicDataRecipeService publicDataRecipeService;
    private final AIRecipeService aiRecipeService;
    private final OpenAIService openAIService;
    private final AmazonS3Client s3Client;

    @Autowired
    public RecipeServiceImpl(ModelMapper modelMapper,
                             RecipeRepository recipeRepository,
                             RecipeManualService recipeManualService,
                             PublicDataRecipeService publicDataRecipeService,
                             AIRecipeService aiRecipeService,
                             OpenAIService openAIService,
                             AmazonS3Client s3Client) {
        this.modelMapper = modelMapper;
        this.recipeRepository = recipeRepository;
        this.recipeManualService = recipeManualService;
        this.publicDataRecipeService = publicDataRecipeService;
        this.aiRecipeService = aiRecipeService;
        this.openAIService = openAIService;
        this.s3Client = s3Client;
    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 페이지 번호로 요리 레시피 조회
    @Override
    public Page<RecipeDTO> findRecipeByPageNo(Integer pageNo) {
        // 페이지 번호 유효성 검사
        if (pageNo == null || pageNo < 1) {
            throw new CommonException(ErrorCode.INVALID_PARAMETER_FORMAT);
        }

        // 페이지와 한 페이지당 요소 수 설정 (pageNumber는 0부터 시작)
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                ELEMENTS_PER_PAGE,
                Sort.by(Sort.Direction.DESC, "recipeId")
        );

        // 레시피 조회
        Page<Recipe> recipePage = recipeRepository.findAll(pageable);

        // 레시피가 존재하지 않는 경우
        if (recipePage.getContent().isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_RECIPE);
        }

        // Recipe -> RecipeDTO 변환 및 Page 반환
        return convertEntityPageToDTOPage(recipePage);
    }

    // 요리 레시피 단건 조회
    @Override
    public RecipeDTO findRecipeByRecipeId(Long recipeId) {
        // 레시피 조회
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));
        return modelMapper.map(recipe, RecipeDTO.class);
    }

    // 요리 레시피 요리 이름으로 조회
    @Override
    public Page<RecipeDTO> findRecipeByMenuName(String menuName, Integer pageNo) {
        // 페이지 번호 유효성 검사
        if (pageNo == null || pageNo < 1) {
            throw new CommonException(ErrorCode.INVALID_PARAMETER_FORMAT);
        }

        // 페이지와 한 페이지당 요소 수 설정 (pageNumber는 0부터 시작)
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                ELEMENTS_PER_PAGE,
                Sort.by(Sort.Direction.DESC, "recipeId")
        );

        // 레시피 조회
        Page<Recipe> recipePage = recipeRepository.findByMenuNameContaining(menuName, pageable);

        // 레시피가 존재하지 않는 경우
        if (recipePage.getContent().isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_RECIPE);
        }

        // Recipe -> RecipeDTO 변환 및 Page 반환
        return convertEntityPageToDTOPage(recipePage);
    }

    // 요리 레시피 등록
    @Override
    @Transactional
    public RecipeDTO registRecipe(RecipeDTO registRecipeDTO) {
        // 요리 레시피 테이블에 저장
        Recipe newRecipe = modelMapper.map(registRecipeDTO, Recipe.class);
        newRecipe = recipeRepository.save(newRecipe);

        // 요리 구분 검사
        if (registRecipeDTO.getMenuType() == MenuType.PUBLIC) {
            // DTO에 요리 레시피 정보 담기
            PublicDataRecipeDTO publicDataRecipeDTO = PublicDataRecipeDTO
                    .builder()
                    .menuName(newRecipe.getMenuName())
                    .menuIngredient(newRecipe.getMenuIngredient())
                    .menuImage(newRecipe.getMenuImage())
                    .recipeId(newRecipe.getRecipeId())
                    .build();

            // 공공데이터 요리 레시피 등록
            publicDataRecipeService.registPublicDataRecipe(publicDataRecipeDTO);
        } else if (registRecipeDTO.getMenuType() == MenuType.AI) {
            // DTO에 요리 레시피 정보 담기
            AIRecipeDTO aiRecipeDTO = AIRecipeDTO
                    .builder()
                    .menuName(newRecipe.getMenuName())
                    .menuIngredient(newRecipe.getMenuIngredient())
                    .menuImage(newRecipe.getMenuImage())
                    .recipeId(newRecipe.getRecipeId())
                    .build();

            // AI 요리 레시피 등록
            aiRecipeService.registAIRecipe(aiRecipeDTO);
        } else {
            // 요리 구분이 잘못되었을 경우
            throw new CommonException(ErrorCode.INVALID_REQUEST_BODY);
        }

        return modelMapper.map(newRecipe, RecipeDTO.class);
    }

    // 요리 레시피 수정
    @Override
    @Transactional
    public RecipeDTO modifyRecipe(Long recipeId, RecipeDTO modifyRecipeDTO) {
        // 기존 엔티티 조회
        Recipe existingRecipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));

        // 엔티티 정보 수정
        existingRecipe.setMenuName(modifyRecipeDTO.getMenuName());
        existingRecipe.setMenuIngredient(modifyRecipeDTO.getMenuIngredient());
        existingRecipe.setMenuImage(modifyRecipeDTO.getMenuImage());
        existingRecipe.setUserId(modifyRecipeDTO.getUserId());

        // 요리 구분 검사
        if (existingRecipe.getMenuType() == MenuType.PUBLIC) {
            // DTO에 요리 레시피 정보 담기
            PublicDataRecipeDTO publicDataRecipeDTO = PublicDataRecipeDTO
                    .builder()
                    .menuName(modifyRecipeDTO.getMenuName())
                    .menuIngredient(modifyRecipeDTO.getMenuIngredient())
                    .menuImage(modifyRecipeDTO.getMenuImage())
                    .recipeId(existingRecipe.getRecipeId())
                    .build();

            // 공공데이터 요리 레시피 수정
            publicDataRecipeService.modifyPublicDataRecipe(publicDataRecipeDTO);
        } else if (existingRecipe.getMenuType() == MenuType.AI) {
            // DTO에 요리 레시피 정보 담기
            AIRecipeDTO aiRecipeDTO = AIRecipeDTO
                    .builder()
                    .menuName(modifyRecipeDTO.getMenuName())
                    .menuIngredient(modifyRecipeDTO.getMenuIngredient())
                    .menuImage(modifyRecipeDTO.getMenuImage())
                    .recipeId(existingRecipe.getRecipeId())
                    .build();

            // AI 요리 레시피 수정
            aiRecipeService.modifyAIRecipe(aiRecipeDTO);
        } else {
            // 요리 구분이 잘못되었을 경우
            throw new CommonException(ErrorCode.INVALID_REQUEST_BODY);
        }

        return modelMapper.map(recipeRepository.save(existingRecipe), RecipeDTO.class);
    }

    // 요리 레시피 삭제
    @Override
    @Transactional
    public void removeRecipe(Long recipeId) {
        // 기존 엔티티 조회
        Recipe existingRecipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));

        recipeRepository.delete(existingRecipe);
    }

    // 요리 추천하기
    @Override
    @Transactional
    public BaseRecipeDTO registRecommendRecipe(RequestRecommendDTO requestRecommendDTO) {
        try {
            // 1단계: AI에게 추천하는 요리 이름 물어보기
            String prompt = "날씨: '" + requestRecommendDTO.getFirst() + "', 기분: '" + requestRecommendDTO.getSecond() +
                    "', 인원: '" + requestRecommendDTO.getThird() + "', 채식 여부: '" + requestRecommendDTO.getFourth() +
                    "', 추가 사항: '" + requestRecommendDTO.getFifth() + "'. " +
                    "추가 사항이 요리와 관련된 경우(예: 알레르기 정보, 선호하는 음식, 상황), 그 요청을 우선 고려하여 요리를 추천해줘. " +
                    "비관련 사항(예: 유명 인물 이름, 해킹 방법 등, 성적인 단어)이 포함된 경우에는 요리를 추천하지 말고 '에러'라고 답해줘. " +
                    "요리를 추천하는 경우에는 다양한 나라의 요리 중 하나를 " +
                    "'한국어 요리 이름(Detailed English description including the dish name)' 형식으로 한 문장으로 추천해줘. " +
                    "영어 설명에는 영어 요리 이름을 포함하고, 설명은 20단어 이내로 간결하게 해줘.";

            String aiAnswerMenu = openAIService.getRecommend(prompt).getChoices().get(0).getMessage().getContent();
            System.out.println(aiAnswerMenu);

            // 한국어 이름과 영어 이름 분리
            String koreanName = aiAnswerMenu.split("\\(")[0].trim();
            String description = aiAnswerMenu.split("\\(")[1].replace(")", "").trim();

            // 앞뒤 특수문자 제거
            String trimmedAiAnswerMenu = trimSpecialCharacters(koreanName);
            String trimmedDescription = trimSpecialCharacters(description);

            System.out.println(trimmedAiAnswerMenu);
            System.out.println(trimmedDescription);

            // 2단계: 요리 레시피 테이블 조회하기

            // 비건이 아니고, 추가 요청 사항이 없을 경우
            if (requestRecommendDTO.getFourth().equals("아니요") && requestRecommendDTO.getFifth().isEmpty()) {
                List<Recipe> recipeDTOList = recipeRepository.findByMenuNameContaining(trimmedAiAnswerMenu);

                // 요리 이름을 저장하는 리스트
                List<String> recipeNameList = new ArrayList<>();

                // 0번 인덱스는 AI가 추천한 요리
                recipeNameList.add(trimmedAiAnswerMenu);

                // 조회된 요리의 이름 삽입
                for (Recipe recipeDTO : recipeDTOList) {
                    recipeNameList.add(recipeDTO.getMenuName());
                }

                // 난수 발생
                int idx = (int) (Math.random() * recipeNameList.size());

                // 난수가 0보다 크면 기존에 존재하던 데이터이므로 return
                if (idx > 0) return modelMapper.map(recipeDTOList.get(idx - 1), RecipeDTO.class);
            }

            // 3단계: 공공데이터 요리 레시피 테이블 조회하기
            PublicDataRecipeDTO publicDataRecipeDTO =
                    publicDataRecipeService.findPublicDataRecipeByMenuName(trimmedAiAnswerMenu);

            // 조회되었을 경우
            if (publicDataRecipeDTO != null) return publicDataRecipeDTO;

            // 4단계: AI 요리 레시피 테이블 조회하기
            AIRecipeDTO aiRecipeDTO = aiRecipeService.findAIRecipeByMenuName(trimmedAiAnswerMenu);

            // 조회되었을 경우
            if (aiRecipeDTO != null) return aiRecipeDTO;

            // 5단계: AI가 추천한 요리의 재료를 물어보기
            String ingredientsPrompt =
                    trimmedAiAnswerMenu + "에 필요한 재료를 ','로 구분해 양과 함께 알려줘. 예: '설탕 2스푼'. 특수문자나 불필요한 말은 제외.";
            String aiAnswerIngredients = openAIService.getRecommend(ingredientsPrompt).getChoices().get(0).getMessage().getContent();
            System.out.println(aiAnswerIngredients);

            // ':'가 있는 경우, ':' 이후의 문자열만 남기기
            aiAnswerIngredients = parseString(aiAnswerIngredients);

            // 앞뒤 특수문자 제거
            String trimmedAiAnswerIngredients = trimSpecialCharacters(aiAnswerIngredients);

            // 6단계: AI가 추천한 요리의 레시피를 물어보기
            String recipePrompt = trimmedAiAnswerMenu + "에 필요한 재료가 " + trimmedAiAnswerIngredients + "일 때, " +
                    trimmedAiAnswerMenu + "의 레시피를 최대 6단계로 요약해줘. " +
                    "각 단계에 번호만 붙여 '1. 쌀을 씻습니다.'와 같은 형식으로 간결하게 작성해줘.";
            String aiAnswerRecipe = openAIService.getRecommend(recipePrompt).getChoices().get(0).getMessage().getContent();
            System.out.println(aiAnswerRecipe);

            // ':'가 있는 경우, ':' 이후의 문자열만 남기기
            aiAnswerRecipe = parseString(aiAnswerRecipe);

            // 7단계: AI가 생성한 요리 등록

            // AI 사진 생성
            String gptImageUrl = registImages(trimmedDescription);
            System.out.println(gptImageUrl);

            // AI가 생성한 요리 정보 입력
            RecipeDTO newRecipeDTO = RecipeDTO
                    .builder()
                    .menuName(trimmedAiAnswerMenu)
                    .menuIngredient(aiAnswerIngredients)
                    .menuImage(null)
                    .menuType(MenuType.AI)
                    .userId(1L)
                    .build();

            // 엔티티 생성
            newRecipeDTO = registRecipe(newRecipeDTO);

            // S3에 사진 등록
            String s3ImageUrl = uploadMenuImage(gptImageUrl, newRecipeDTO.getRecipeId());

            // 대표 이미지 업데이트
            RecipeDTO modifyRecipeDTO = RecipeDTO
                    .builder()
                    .recipeId(newRecipeDTO.getRecipeId())
                    .menuName(newRecipeDTO.getMenuName())
                    .menuIngredient(newRecipeDTO.getMenuIngredient())
                    .menuImage(s3ImageUrl)
                    .menuType(MenuType.AI)
                    .userId(1L)
                    .build();

            modifyRecipeDTO = modifyRecipe(modifyRecipeDTO.getRecipeId(), modifyRecipeDTO);
            log.info(modifyRecipeDTO.toString());

            // 8단계: AI가 생성한 요리 레시피 매뉴얼 등록
            List<Map<String, String>> manual = new ArrayList<>();

            List<String> contents = Arrays.stream(aiAnswerRecipe.split("\n")).toList();
            for (String content : contents) {
                Map<String, String> map = new HashMap<>();
                map.put("content", content);
                manual.add(map);
            }

            RequestRecipeManualDTO requestRecipeManualDTO = RequestRecipeManualDTO
                    .builder()
                    .manual(manual)
                    .build();

            recipeManualService.registRecipeManual(modifyRecipeDTO.getRecipeId(), requestRecipeManualDTO);

            return modifyRecipeDTO;
        } catch (Exception e) {
            throw new CommonException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    // 요리 사진 생성하기
    @Override
    public String registImages(String menuName) {
        return openAIService.getImages(menuName).getData().get(0).getUrl();
    }

    // 이미지의 url을 받아 S3에 등록하는 메소드
    public String uploadMenuImage(String imageUrl, Long recipeId) {
        String fileName = "recipe_" + recipeId + ".png"; // 기본 파일명 및 확장자

        // URL 객체 생성
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();

            // 응답 코드 확인
            if (connection.getResponseCode() != 200) {
                throw new IOException("URL에서 이미지를 가져오지 못했습니다. 응답 코드: " + connection.getResponseCode());
            }

            // 연결에서 입력 스트림 가져오기
            try (InputStream inputStream = connection.getInputStream()) {
                // S3에 대한 ObjectMetadata 생성
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(connection.getContentLengthLong());
                metadata.setContentType(connection.getContentType());
                metadata.setContentDisposition("inline");

                // S3에 파일 업로드
                s3Client.putObject(new PutObjectRequest(bucket, fileName, inputStream, metadata));
            } // try-with-resources 문을 사용하여 입력 스트림 자동으로 닫기

            // 업로드된 이미지의 S3 URL 반환
            return s3Client.getUrl(bucket, fileName).toString();
        } catch (AmazonClientException | IOException e) {
            log.error("S3에 이미지 업로드 실패", e);
            throw new CommonException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    // 이미지의 url을 받아 S3에서 삭제하는 메소드
    public void deleteMenuImage(String fileUrl) {
        // 유효성 검사
        if (fileUrl == null || !fileUrl.contains(".com/")) {
            log.error("유효하지 않은 파일 URL: {}", fileUrl);
            return;
        }

        String fileName = fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5); // ".com/" 길이 5

        try {
            // S3에서 파일 삭제 요청
            s3Client.deleteObject(new DeleteObjectRequest(bucket, fileName));
            log.info("S3에서 성공적으로 파일이 삭제되었습니다: {}", fileName);
        } catch (AmazonClientException e) {
            log.error("S3에서 파일을 삭제하지 못하였습니다.: {}", fileName, e);
        }
    }

    // ':'가 있는 경우, ':' 이후의 문자열만 남기는 메소드
    private String parseString(String aiAnswer) {
        int colonIndex = aiAnswer.indexOf(":");
        if (colonIndex != -1) {
            aiAnswer = aiAnswer.substring(colonIndex + 1).trim();
        }
        return aiAnswer;
    }

    // 문자열의 앞뒤에 있는 모든 특수문자를 제거하는 메소드
    private String trimSpecialCharacters(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 정규식을 사용하여 문자열의 앞뒤에서 특수문자를 제거
        return input.replaceAll("^[^a-zA-Z0-9가-힣]+|[^a-zA-Z0-9가-힣]+$", "");
    }

    // Recipe -> RecipeDTO 변환 및 Page 반환 메소드
    private Page<RecipeDTO> convertEntityPageToDTOPage(Page<Recipe> recipePage) {
        List<RecipeDTO> recipeDTOList = recipePage.getContent().stream()
                .map(recipe -> modelMapper.map(recipe, RecipeDTO.class))
                .toList();

        return new PageImpl<>(recipeDTOList, recipePage.getPageable(), recipePage.getTotalElements());
    }

}
